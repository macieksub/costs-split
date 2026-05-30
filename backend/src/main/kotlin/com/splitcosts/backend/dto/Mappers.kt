package com.splitcosts.backend.dto

import com.splitcosts.backend.model.*

fun User.toDto(): UserDto {
    return UserDto(
        id = this.id ?: 0L,
        username = this.username,
        email = this.email,
        name = this.name
    )
}

fun Group.toDto(): GroupDto {
    return GroupDto(
        id = this.id ?: 0L,
        name = this.name,
        description = this.description,
        admin = this.admin.toDto(),
        members = this.members.map { it.toDto() }
    )
}

fun ExpenseSplit.toDto(): SplitDto {
    return SplitDto(
        id = this.id ?: 0L,
        user = this.user.toDto(),
        amount = this.amount
    )
}

fun Expense.toDto(): ExpenseDto {
    return ExpenseDto(
        id = this.id ?: 0L,
        description = this.description,
        amount = this.amount,
        currency = this.currency,
        paidBy = this.paidBy.toDto(),
        date = this.date,
        splits = this.splits.map { it.toDto() }
    )
}

fun Settlement.toDto(): SettlementDto {
    return SettlementDto(
        id = this.id ?: 0L,
        amount = this.amount,
        currency = this.currency,
        debtor = this.debtor.toDto(),
        creditor = this.creditor.toDto(),
        date = this.date,
        approved = this.approved
    )
}
