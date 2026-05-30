package org.example.frontend.model

import kotlinx.serialization.Serializable

@Serializable
data class UserDto(
    val id: Long,
    val username: String,
    val email: String,
    val name: String
)

@Serializable
data class RegisterRequest(
    val username: String,
    val email: String,
    val password: String,
    val name: String
)

@Serializable
data class LoginRequest(
    val usernameOrEmail: String,
    val password: String
)

@Serializable
data class AuthResponse(
    val token: String,
    val user: UserDto
)

@Serializable
data class CreateGroupRequest(
    val name: String,
    val description: String? = null
)

@Serializable
data class GroupDto(
    val id: Long,
    val name: String,
    val description: String?,
    val admin: UserDto,
    val members: List<UserDto>
)

@Serializable
data class AddMemberRequest(
    val usernameOrEmail: String
)

@Serializable
data class CreateExpenseRequest(
    val description: String,
    val amount: Double,
    val currency: String = "PLN",
    val paidByUserId: Long,
    val splits: List<CreateSplitRequest>
)

@Serializable
data class CreateSplitRequest(
    val userId: Long,
    val amount: Double
)

@Serializable
data class SplitDto(
    val id: Long,
    val user: UserDto,
    val amount: Double
)

@Serializable
data class ExpenseDto(
    val id: Long,
    val description: String,
    val amount: Double,
    val currency: String,
    val paidBy: UserDto,
    val date: String, // String ISO representation for easy cross-platform rendering
    val splits: List<SplitDto>
)

@Serializable
data class DebtDto(
    val fromUser: UserDto,
    val toUser: UserDto,
    val amount: Double,
    val currency: String
)

@Serializable
data class MemberBalanceDto(
    val user: UserDto,
    val netBalance: Double,
    val currency: String
)

@Serializable
data class GroupBalancesDto(
    val groupId: Long,
    val balances: List<MemberBalanceDto>,
    val simplifiedDebts: List<DebtDto>
)

@Serializable
data class CreateSettlementRequest(
    val amount: Double,
    val currency: String = "PLN",
    val debtorId: Long,
    val creditorId: Long
)

@Serializable
data class SettlementDto(
    val id: Long,
    val amount: Double,
    val currency: String,
    val debtor: UserDto,
    val creditor: UserDto,
    val date: String,
    val approved: Boolean
)
