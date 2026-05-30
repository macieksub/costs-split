package com.splitcosts.backend.config

import com.splitcosts.backend.model.*
import com.splitcosts.backend.repository.*
import org.springframework.boot.CommandLineRunner
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.Instant

@Component
class DatabaseInitializer(
    private val userRepository: UserRepository,
    private val groupRepository: GroupRepository,
    private val expenseRepository: ExpenseRepository,
    private val settlementRepository: SettlementRepository,
    private val passwordEncoder: PasswordEncoder
) : CommandLineRunner {

    @Transactional
    override fun run(vararg args: String) {
        if (userRepository.count() > 0L) {
            println("Baza danych zawiera już dane. Pomijam inicjalizację przykładowych rekordów.")
            return
        }

        println("Inicjalizacja przykładowych danych w bazie...")

        // 1. Tworzenie użytkowników
        val passwordHash = passwordEncoder.encode("password123")!!
        
        val janek = userRepository.save(
            User(
                username = "janek",
                email = "jan@example.com",
                passwordHash = passwordHash,
                name = "Jan Kowalski"
            )
        )

        val adam = userRepository.save(
            User(
                username = "adam",
                email = "adam@example.com",
                passwordHash = passwordHash,
                name = "Adam Nowak"
            )
        )

        val kasia = userRepository.save(
            User(
                username = "kasia",
                email = "kasia@example.com",
                passwordHash = passwordHash,
                name = "Kasia Wiśniewska"
            )
        )

        println("Utworzono 3 przykładowych użytkowników (hasło dla każdego: 'password123').")

        // 2. Tworzenie grup
        val mazuryGroup = groupRepository.save(
            Group(
                name = "Wyjazd na Mazury ⛵",
                description = "Wspólny wyjazd majówkowy ze znajomymi ze studiów",
                admin = janek,
                members = mutableSetOf(janek, adam, kasia)
            )
        )

        val flatGroup = groupRepository.save(
            Group(
                name = "Wspólne Mieszkanie 🏠",
                description = "Rozliczenia mediów i chemii domowej w mieszkaniu przy ul. Kotlinowej",
                admin = adam,
                members = mutableSetOf(janek, adam)
            )
        )

        println("Utworzono 2 przykładowe grupy.")

        // 3. Dodawanie wydatków do grupy "Wyjazd na Mazury"
        
        // Wydatek 1: Paliwo (zapłacił Janek = 180 PLN, dzielone na 3 osoby po 60 PLN)
        val fuelExpense = Expense(
            group = mazuryGroup,
            description = "Paliwo do auta (trasa tam i z powrotem)",
            amount = BigDecimal("180.00"),
            currency = "PLN",
            paidBy = janek,
            date = Instant.now().minusSeconds(86400 * 3) // 3 dni temu
        )
        fuelExpense.splits = mutableListOf(
            ExpenseSplit(expense = fuelExpense, user = janek, amount = BigDecimal("60.00")),
            ExpenseSplit(expense = fuelExpense, user = adam, amount = BigDecimal("60.00")),
            ExpenseSplit(expense = fuelExpense, user = kasia, amount = BigDecimal("60.00"))
        )
        expenseRepository.save(fuelExpense)

        // Wydatek 2: Biedronka (zapłacił Adam = 120 PLN, dzielone na 3 osoby po 40 PLN)
        val groceryExpense = Expense(
            group = mazuryGroup,
            description = "Zakupy w Biedronce (grill, napoje, śniadania)",
            amount = BigDecimal("120.00"),
            currency = "PLN",
            paidBy = adam,
            date = Instant.now().minusSeconds(86400 * 2) // 2 dni temu
        )
        groceryExpense.splits = mutableListOf(
            ExpenseSplit(expense = groceryExpense, user = janek, amount = BigDecimal("40.00")),
            ExpenseSplit(expense = groceryExpense, user = adam, amount = BigDecimal("40.00")),
            ExpenseSplit(expense = groceryExpense, user = kasia, amount = BigDecimal("40.00"))
        )
        expenseRepository.save(groceryExpense)

        // Wydatek 3: Łódka (zapłaciła Kasia = 90 PLN, dzielone na 3 osoby po 30 PLN)
        val boatExpense = Expense(
            group = mazuryGroup,
            description = "Wypożyczenie łódki elektrycznej",
            amount = BigDecimal("90.00"),
            currency = "PLN",
            paidBy = kasia,
            date = Instant.now().minusSeconds(86400 * 1) // 1 dzień temu
        )
        boatExpense.splits = mutableListOf(
            ExpenseSplit(expense = boatExpense, user = janek, amount = BigDecimal("30.00")),
            ExpenseSplit(expense = boatExpense, user = adam, amount = BigDecimal("30.00")),
            ExpenseSplit(expense = boatExpense, user = kasia, amount = BigDecimal("30.00"))
        )
        expenseRepository.save(boatExpense)

        println("Utworzono 3 przykładowe wydatki w grupie 'Wyjazd na Mazury'.")

        // 4. Dodawanie rozliczeń (Settlements)
        // Adam spłacił Jankowi 20 PLN (zatwierdzone przez Janka)
        val adamToJanekSettlement = Settlement(
            group = mazuryGroup,
            debtor = adam,
            creditor = janek,
            amount = BigDecimal("20.00"),
            currency = "PLN",
            date = Instant.now().minusSeconds(3600), // godzinę temu
            approved = true
        )
        settlementRepository.save(adamToJanekSettlement)

        // Dodatkowe przykładowe koszty w grupie "Wspólne Mieszkanie"
        val internetExpense = Expense(
            group = flatGroup,
            description = "Internet światłowodowy - maj",
            amount = BigDecimal("80.00"),
            currency = "PLN",
            paidBy = adam,
            date = Instant.now().minusSeconds(86400 * 5)
        )
        internetExpense.splits = mutableListOf(
            ExpenseSplit(expense = internetExpense, user = janek, amount = BigDecimal("40.00")),
            ExpenseSplit(expense = internetExpense, user = adam, amount = BigDecimal("40.00"))
        )
        expenseRepository.save(internetExpense)

        println("Dodano przykładowe rozliczenia.")
        println("Inicjalizacja bazy zakończona pomyślnie! Baza danych jest gotowa do testów.")
    }
}
