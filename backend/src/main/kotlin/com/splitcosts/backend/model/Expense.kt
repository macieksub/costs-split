package com.splitcosts.backend.model

import jakarta.persistence.*
import java.math.BigDecimal
import java.time.Instant
import java.util.Objects

@Entity
@Table(name = "expenses")
class Expense(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", nullable = false)
    val group: Group,

    @Column(nullable = false)
    var description: String,

    @Column(nullable = false, precision = 19, scale = 4)
    var amount: BigDecimal,

    @Column(nullable = false, length = 3)
    var currency: String = "PLN",

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "paid_by_user_id", nullable = false)
    var paidBy: User,

    @Column(nullable = false)
    var date: Instant = Instant.now(),

    @OneToMany(mappedBy = "expense", cascade = [CascadeType.ALL], orphanRemoval = true)
    var splits: MutableList<ExpenseSplit> = mutableListOf()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        other as Expense
        return id != null && id == other.id
    }

    override fun hashCode(): Int {
        return Objects.hash(id)
    }

    override fun toString(): String {
        return "Expense(id=$id, description='$description', amount=$amount, currency='$currency', date=$date)"
    }
}
