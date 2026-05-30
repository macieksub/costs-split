package com.splitcosts.backend.service

import com.splitcosts.backend.dto.*
import com.splitcosts.backend.model.Expense
import com.splitcosts.backend.model.Settlement
import com.splitcosts.backend.model.User
import com.splitcosts.backend.repository.ExpenseRepository
import com.splitcosts.backend.repository.SettlementRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Objects

@Service
class BalanceService(
    private val groupService: GroupService,
    private val expenseRepository: ExpenseRepository,
    private val settlementRepository: SettlementRepository
) {

    @Transactional(readOnly = true)
    fun getGroupBalances(groupId: Long, currentUser: User): GroupBalancesDto {
        val group = groupService.getGroupEntity(groupId)
        groupService.verifyMember(group, currentUser)

        val expenses = expenseRepository.findByGroupIdWithSplitsAndPaidBy(groupId)
        val settlements = settlementRepository.findByGroupIdWithDebtorAndCreditor(groupId)

        // Find all currencies used in this group (default to PLN if none exists yet)
        val currencies = (expenses.map { it.currency } + settlements.map { it.currency } + "PLN")
            .filter { it.isNotBlank() }
            .distinct()

        val allBalances = mutableListOf<MemberBalanceDto>()
        val allSimplifiedDebts = mutableListOf<DebtDto>()

        val members = group.members.toList()

        for (currency in currencies) {
            // Compute balances for this specific currency
            val currencyExpenses = expenses.filter { it.currency.uppercase() == currency.uppercase() }
            val currencySettlements = settlements.filter { it.currency.uppercase() == currency.uppercase() && it.approved }

            val balanceMap = mutableMapOf<User, BigDecimal>()
            members.forEach { balanceMap[it] = BigDecimal.ZERO }

            // 1. Add paid amounts and subtract split shares from expenses
            for (expense in currencyExpenses) {
                // Paid by
                val paidUser = expense.paidBy
                if (balanceMap.containsKey(paidUser)) {
                    balanceMap[paidUser] = balanceMap[paidUser]!!.add(expense.amount)
                }

                // Splits
                for (split in expense.splits) {
                    val splitUser = split.user
                    if (balanceMap.containsKey(splitUser)) {
                        balanceMap[splitUser] = balanceMap[splitUser]!!.subtract(split.amount)
                    }
                }
            }

            // 2. Adjust for approved settlements
            for (settlement in currencySettlements) {
                val debtor = settlement.debtor
                val creditor = settlement.creditor

                // Debtor paid, so their negative balance increases (moves towards zero)
                if (balanceMap.containsKey(debtor)) {
                    balanceMap[debtor] = balanceMap[debtor]!!.add(settlement.amount)
                }
                // Creditor received, so their positive balance decreases (moves towards zero)
                if (balanceMap.containsKey(creditor)) {
                    balanceMap[creditor] = balanceMap[creditor]!!.subtract(settlement.amount)
                }
            }

            // Convert map to DTOs (only add if there are any transactions/balances in this currency)
            val hasTransactions = currencyExpenses.isNotEmpty() || currencySettlements.isNotEmpty()
            if (hasTransactions) {
                balanceMap.forEach { (user, balance) ->
                    allBalances.add(
                        MemberBalanceDto(
                            user = user.toDto(),
                            netBalance = balance.setScale(2, RoundingMode.HALF_UP),
                            currency = currency
                        )
                    )
                }

                // 3. Simplify debts for this currency
                val simplified = simplifyDebtsForCurrency(balanceMap, currency)
                allSimplifiedDebts.addAll(simplified)
            }
        }

        return GroupBalancesDto(
            groupId = groupId,
            balances = allBalances,
            simplifiedDebts = allSimplifiedDebts
        )
    }

    private fun simplifyDebtsForCurrency(balances: Map<User, BigDecimal>, currency: String): List<DebtDto> {
        // Separate debtors (negative balance) and creditors (positive balance)
        val debtors = mutableListOf<Pair<User, BigDecimal>>()
        val creditors = mutableListOf<Pair<User, BigDecimal>>()

        val threshold = BigDecimal("0.01")

        balances.forEach { (user, bal) ->
            if (bal.compareTo(BigDecimal.ZERO) < 0 && bal.abs().compareTo(threshold) > 0) {
                debtors.add(Pair(user, bal))
            } else if (bal.compareTo(BigDecimal.ZERO) > 0 && bal.compareTo(threshold) > 0) {
                creditors.add(Pair(user, bal))
            }
        }

        // Sort: debtors ascending (most negative first), creditors descending (most positive first)
        debtors.sortBy { it.second } // e.g. -50, -30, -10
        creditors.sortByDescending { it.second } // e.g. 60, 20, 10

        val debts = mutableListOf<DebtDto>()
        var dIndex = 0
        var cIndex = 0

        val workingDebtors = debtors.map { it.second.abs() }.toMutableList()
        val workingCreditors = creditors.map { it.second }.toMutableList()

        while (dIndex < debtors.size && cIndex < creditors.size) {
            val debtorUser = debtors[dIndex].first
            val creditorUser = creditors[cIndex].first

            val debtorOwes = workingDebtors[dIndex]
            val creditorIsOwed = workingCreditors[cIndex]

            val transactionAmount = debtorOwes.min(creditorIsOwed)

            if (transactionAmount.compareTo(threshold) > 0) {
                debts.add(
                    DebtDto(
                        fromUser = debtorUser.toDto(),
                        toUser = creditorUser.toDto(),
                        amount = transactionAmount.setScale(2, RoundingMode.HALF_UP),
                        currency = currency
                    )
                )
            }

            // Update remaining amounts
            workingDebtors[dIndex] = debtorOwes.subtract(transactionAmount)
            workingCreditors[cIndex] = creditorIsOwed.subtract(transactionAmount)

            if (workingDebtors[dIndex].compareTo(threshold) < 0) {
                dIndex++
            }
            if (workingCreditors[cIndex].compareTo(threshold) < 0) {
                cIndex++
            }
        }

        return debts
    }
}
