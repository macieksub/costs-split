package com.splitcosts.backend.repository

import com.splitcosts.backend.model.Group
import com.splitcosts.backend.model.User
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface GroupRepository : JpaRepository<Group, Long> {
    @Query("SELECT DISTINCT g FROM Group g LEFT JOIN FETCH g.members m WHERE m = :user OR g.admin = :user")
    fun findByUser(@Param("user") user: User): List<Group>
}
