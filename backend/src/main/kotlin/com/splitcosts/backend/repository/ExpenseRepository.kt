package com.splitcosts.backend.repository

import com.splitcosts.backend.model.Expense
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface ExpenseRepository : JpaRepository<Expense, Long> {
    @Query("SELECT DISTINCT e FROM Expense e LEFT JOIN FETCH e.splits s LEFT JOIN FETCH e.paidBy p WHERE e.group.id = :groupId ORDER BY e.date DESC")
    fun findByGroupIdWithSplitsAndPaidBy(@Param("groupId") groupId: Long): List<Expense>
}
