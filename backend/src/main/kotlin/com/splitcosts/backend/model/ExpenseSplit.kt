package com.splitcosts.backend.model

import jakarta.persistence.*
import java.math.BigDecimal
import java.util.Objects

@Entity
@Table(name = "expense_splits")
class ExpenseSplit(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "expense_id", nullable = false)
    val expense: Expense,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    val user: User,

    @Column(nullable = false, precision = 19, scale = 4)
    var amount: BigDecimal
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        other as ExpenseSplit
        return id != null && id == other.id
    }

    override fun hashCode(): Int {
        return Objects.hash(id)
    }

    override fun toString(): String {
        return "ExpenseSplit(id=$id, amount=$amount)"
    }
}
