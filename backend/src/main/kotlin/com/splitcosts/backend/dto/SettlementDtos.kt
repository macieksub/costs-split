package com.splitcosts.backend.dto

import java.math.BigDecimal
import java.time.Instant

data class CreateSettlementRequest(
    val amount: BigDecimal,
    val currency: String = "PLN",
    val debtorId: Long,
    val creditorId: Long
)

data class SettlementDto(
    val id: Long,
    val amount: BigDecimal,
    val currency: String,
    val debtor: UserDto,
    val creditor: UserDto,
    val date: Instant,
    val approved: Boolean
)
