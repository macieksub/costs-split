package com.splitcosts.backend.model

import jakarta.persistence.*
import java.math.BigDecimal
import java.time.Instant
import java.util.Objects

@Entity
@Table(name = "settlements")
class Settlement(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", nullable = false)
    val group: Group,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "debtor_user_id", nullable = false)
    val debtor: User,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creditor_user_id", nullable = false)
    val creditor: User,

    @Column(nullable = false, precision = 19, scale = 4)
    var amount: BigDecimal,

    @Column(nullable = false, length = 3)
    var currency: String = "PLN",

    @Column(nullable = false)
    var date: Instant = Instant.now(),

    @Column(nullable = false)
    var approved: Boolean = false
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        other as Settlement
        return id != null && id == other.id
    }

    override fun hashCode(): Int {
        return Objects.hash(id)
    }

    override fun toString(): String {
        return "Settlement(id=$id, amount=$amount, currency='$currency', approved=$approved)"
    }
}
