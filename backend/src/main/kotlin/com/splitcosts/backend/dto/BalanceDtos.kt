package com.splitcosts.backend.dto

import java.math.BigDecimal

data class DebtDto(
    val fromUser: UserDto,
    val toUser: UserDto,
    val amount: BigDecimal,
    val currency: String
)

data class MemberBalanceDto(
    val user: UserDto,
    val netBalance: BigDecimal,
    val currency: String
)

data class GroupBalancesDto(
    val groupId: Long,
    val balances: List<MemberBalanceDto>,
    val simplifiedDebts: List<DebtDto>
)
