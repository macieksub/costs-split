package com.splitcosts.backend.service

import com.splitcosts.backend.dto.CreateExpenseRequest
import com.splitcosts.backend.dto.ExpenseDto
import com.splitcosts.backend.dto.toDto
import com.splitcosts.backend.model.Expense
import com.splitcosts.backend.model.ExpenseSplit
import com.splitcosts.backend.model.User
import com.splitcosts.backend.repository.ExpenseRepository
import com.splitcosts.backend.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.math.RoundingMode

@Service
class ExpenseService(
    private val expenseRepository: ExpenseRepository,
    private val groupService: GroupService,
    private val userRepository: UserRepository
) {

    @Transactional
    fun createExpense(groupId: Long, request: CreateExpenseRequest, currentUser: User): ExpenseDto {
        val group = groupService.getGroupEntity(groupId)
        groupService.verifyMember(group, currentUser)

        val paidBy = userRepository.findById(request.paidByUserId)
            .orElseThrow { IllegalArgumentException("Payer with id ${request.paidByUserId} not found.") }

        if (group.members.none { it.id == paidBy.id }) {
            throw IllegalArgumentException("Payer must be a member of the group.")
        }

        val expense = Expense(
            group = group,
            description = request.description,
            amount = request.amount,
            currency = request.currency,
            paidBy = paidBy
        )

        val splits = mutableListOf<ExpenseSplit>()

        if (request.splits.isEmpty()) {
            // Default to equal split among all group members
            val members = group.members.toList()
            val totalMembers = members.size
            if (totalMembers == 0) {
                throw IllegalArgumentException("Cannot split expense in a group with no members.")
            }

            // Calculate base amount per person
            val baseAmount = request.amount.divide(BigDecimal(totalMembers), 2, RoundingMode.HALF_UP)
            
            // Adjust for any rounding issues to make sure the sum matches exactly
            var sum = BigDecimal.ZERO
            for (i in 0 until totalMembers - 1) {
                splits.add(ExpenseSplit(expense = expense, user = members[i], amount = baseAmount))
                sum = sum.add(baseAmount)
            }
            // Last member gets the remainder
            val remainderAmount = request.amount.subtract(sum)
            splits.add(ExpenseSplit(expense = expense, user = members.last(), amount = remainderAmount))
        } else {
            // Custom splits provided: validate sum matches exactly
            var splitsSum = BigDecimal.ZERO
            for (splitReq in request.splits) {
                val splitUser = userRepository.findById(splitReq.userId)
                    .orElseThrow { IllegalArgumentException("User in split with id ${splitReq.userId} not found.") }

                if (group.members.none { it.id == splitUser.id }) {
                    throw IllegalArgumentException("All users in splits must be members of the group.")
                }

                splits.add(ExpenseSplit(expense = expense, user = splitUser, amount = splitReq.amount))
                splitsSum = splitsSum.add(splitReq.amount)
            }

            // Compare splits sum with total amount, ignoring tiny differences less than 0.01 (or matching exactly up to 2 decimal places)
            if (splitsSum.setScale(2, RoundingMode.HALF_UP) != request.amount.setScale(2, RoundingMode.HALF_UP)) {
                throw IllegalArgumentException("The sum of splits ($splitsSum) does not match the total expense amount (${request.amount}).")
            }
        }

        expense.splits = splits
        return expenseRepository.save(expense).toDto()
    }

    @Transactional(readOnly = true)
    fun getExpensesForGroup(groupId: Long, currentUser: User): List<ExpenseDto> {
        val group = groupService.getGroupEntity(groupId)
        groupService.verifyMember(group, currentUser)
        return expenseRepository.findByGroupIdWithSplitsAndPaidBy(groupId).map { it.toDto() }
    }

    @Transactional
    fun deleteExpense(groupId: Long, expenseId: Long, currentUser: User) {
        val group = groupService.getGroupEntity(groupId)
        groupService.verifyMember(group, currentUser)

        val expense = expenseRepository.findById(expenseId)
            .orElseThrow { NoSuchElementException("Expense with id $expenseId not found.") }

        if (expense.group.id != groupId) {
            throw IllegalArgumentException("Expense does not belong to this group.")
        }

        // Admin of the group or the user who paid can delete the expense
        if (group.admin.id != currentUser.id && expense.paidBy.id != currentUser.id) {
            throw SecurityException("Only the group administrator or the person who paid this expense can delete it.")
        }

        expenseRepository.delete(expense)
    }
}
