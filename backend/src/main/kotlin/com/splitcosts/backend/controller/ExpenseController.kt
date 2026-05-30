package com.splitcosts.backend.controller

import com.splitcosts.backend.dto.CreateExpenseRequest
import com.splitcosts.backend.dto.ExpenseDto
import com.splitcosts.backend.security.UserPrincipal
import com.splitcosts.backend.service.ExpenseService
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/groups/{groupId}/expenses")
class ExpenseController(private val expenseService: ExpenseService) {

    @PostMapping
    fun createExpense(
        @PathVariable groupId: Long,
        @RequestBody request: CreateExpenseRequest,
        @AuthenticationPrincipal principal: UserPrincipal
    ): ResponseEntity<ExpenseDto> {
        val expense = expenseService.createExpense(groupId, request, principal.user)
        return ResponseEntity.ok(expense)
    }

    @GetMapping
    fun getExpenses(
        @PathVariable groupId: Long,
        @AuthenticationPrincipal principal: UserPrincipal
    ): ResponseEntity<List<ExpenseDto>> {
        val expenses = expenseService.getExpensesForGroup(groupId, principal.user)
        return ResponseEntity.ok(expenses)
    }

    @DeleteMapping("/{expenseId}")
    fun deleteExpense(
        @PathVariable groupId: Long,
        @PathVariable expenseId: Long,
        @AuthenticationPrincipal principal: UserPrincipal
    ): ResponseEntity<Void> {
        expenseService.deleteExpense(groupId, expenseId, principal.user)
        return ResponseEntity.noContent().build()
    }
}
