package com.splitcosts.backend.dto

import java.math.BigDecimal
import java.time.Instant

data class CreateExpenseRequest(
    val description: String,
    val amount: BigDecimal,
    val currency: String = "PLN",
    val paidByUserId: Long,
    val splits: List<CreateSplitRequest>
)

data class CreateSplitRequest(
    val userId: Long,
    val amount: BigDecimal
)

data class ExpenseDto(
    val id: Long,
    val description: String,
    val amount: BigDecimal,
    val currency: String,
    val paidBy: UserDto,
    val date: Instant,
    val splits: List<SplitDto>
)

data class SplitDto(
    val id: Long,
    val user: UserDto,
    val amount: BigDecimal
)
