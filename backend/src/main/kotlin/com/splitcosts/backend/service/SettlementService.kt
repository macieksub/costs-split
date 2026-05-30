package com.splitcosts.backend.service

import com.splitcosts.backend.dto.CreateSettlementRequest
import com.splitcosts.backend.dto.SettlementDto
import com.splitcosts.backend.dto.toDto
import com.splitcosts.backend.model.Settlement
import com.splitcosts.backend.model.User
import com.splitcosts.backend.repository.SettlementRepository
import com.splitcosts.backend.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class SettlementService(
    private val settlementRepository: SettlementRepository,
    private val groupService: GroupService,
    private val userRepository: UserRepository
) {

    @Transactional
    fun createSettlement(groupId: Long, request: CreateSettlementRequest, currentUser: User): SettlementDto {
        val group = groupService.getGroupEntity(groupId)
        groupService.verifyMember(group, currentUser)

        val debtor = userRepository.findById(request.debtorId)
            .orElseThrow { IllegalArgumentException("Debtor with id ${request.debtorId} not found.") }
        
        val creditor = userRepository.findById(request.creditorId)
            .orElseThrow { IllegalArgumentException("Creditor with id ${request.creditorId} not found.") }

        if (group.members.none { it.id == debtor.id }) {
            throw IllegalArgumentException("Debtor must be a member of the group.")
        }
        if (group.members.none { it.id == creditor.id }) {
            throw IllegalArgumentException("Creditor must be a member of the group.")
        }

        val settlement = Settlement(
            group = group,
            debtor = debtor,
            creditor = creditor,
            amount = request.amount,
            currency = request.currency,
            approved = false // Initially unapproved, creditor must approve it
        )

        return settlementRepository.save(settlement).toDto()
    }

    @Transactional
    fun approveSettlement(groupId: Long, settlementId: Long, currentUser: User): SettlementDto {
        val group = groupService.getGroupEntity(groupId)
        groupService.verifyMember(group, currentUser)

        val settlement = settlementRepository.findById(settlementId)
            .orElseThrow { NoSuchElementException("Settlement with id $settlementId not found.") }

        if (settlement.group.id != groupId) {
            throw IllegalArgumentException("Settlement does not belong to this group.")
        }

        // Only the creditor (the receiver of the money) can approve the settlement
        if (settlement.creditor.id != currentUser.id) {
            throw SecurityException("Only the creditor (${settlement.creditor.name}) can approve and confirm this payment.")
        }

        settlement.approved = true
        return settlementRepository.save(settlement).toDto()
    }

    @Transactional(readOnly = true)
    fun getSettlementsForGroup(groupId: Long, currentUser: User): List<SettlementDto> {
        val group = groupService.getGroupEntity(groupId)
        groupService.verifyMember(group, currentUser)
        return settlementRepository.findByGroupIdWithDebtorAndCreditor(groupId).map { it.toDto() }
    }
}
