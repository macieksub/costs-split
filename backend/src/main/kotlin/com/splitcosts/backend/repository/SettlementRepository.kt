package com.splitcosts.backend.repository

import com.splitcosts.backend.model.Settlement
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface SettlementRepository : JpaRepository<Settlement, Long> {
    @Query("SELECT s FROM Settlement s JOIN FETCH s.debtor d JOIN FETCH s.creditor c WHERE s.group.id = :groupId ORDER BY s.date DESC")
    fun findByGroupIdWithDebtorAndCreditor(@Param("groupId") groupId: Long): List<Settlement>
}
