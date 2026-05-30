package com.splitcosts.backend.controller

import com.splitcosts.backend.dto.CreateSettlementRequest
import com.splitcosts.backend.dto.GroupBalancesDto
import com.splitcosts.backend.dto.SettlementDto
import com.splitcosts.backend.security.UserPrincipal
import com.splitcosts.backend.service.BalanceService
import com.splitcosts.backend.service.SettlementService
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/groups/{groupId}")
class BalanceController(
    private val balanceService: BalanceService,
    private val settlementService: SettlementService
) {

    @GetMapping("/balances")
    fun getBalances(
        @PathVariable groupId: Long,
        @AuthenticationPrincipal principal: UserPrincipal
    ): ResponseEntity<GroupBalancesDto> {
        val balances = balanceService.getGroupBalances(groupId, principal.user)
        return ResponseEntity.ok(balances)
    }

    @PostMapping("/settlements")
    fun createSettlement(
        @PathVariable groupId: Long,
        @RequestBody request: CreateSettlementRequest,
        @AuthenticationPrincipal principal: UserPrincipal
    ): ResponseEntity<SettlementDto> {
        val settlement = settlementService.createSettlement(groupId, request, principal.user)
        return ResponseEntity.ok(settlement)
    }

    @PostMapping("/settlements/{settlementId}/approve")
    fun approveSettlement(
        @PathVariable groupId: Long,
        @PathVariable settlementId: Long,
        @AuthenticationPrincipal principal: UserPrincipal
    ): ResponseEntity<SettlementDto> {
        val settlement = settlementService.approveSettlement(groupId, settlementId, principal.user)
        return ResponseEntity.ok(settlement)
    }

    @GetMapping("/settlements")
    fun getSettlements(
        @PathVariable groupId: Long,
        @AuthenticationPrincipal principal: UserPrincipal
    ): ResponseEntity<List<SettlementDto>> {
        val settlements = settlementService.getSettlementsForGroup(groupId, principal.user)
        return ResponseEntity.ok(settlements)
    }
}
