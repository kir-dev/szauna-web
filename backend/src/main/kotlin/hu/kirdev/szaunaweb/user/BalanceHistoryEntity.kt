package hu.kirdev.szaunaweb.user

import hu.kirdev.szaunaweb.opening.OpeningBookingEntity
import hu.kirdev.szaunaweb.persistence.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.Index
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.Instant

@Entity
@EntityListeners(AuditingEntityListener::class)
@Table(
    name = "balance_history",
    indexes = [
        Index(name = "idx_balance_history_user_created", columnList = "user_id, created_at")
    ]
)
class BalanceHistoryEntity(

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    var type: BalanceChangeType,

    @Column(name = "value_change", nullable = false)
    var valueChange: Int = 0,

    @Column(name = "balance_after", nullable = false)
    var balanceAfter: Int,

    @Column(name = "message")
    var message: String?,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    var user: UserEntity,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by_id", nullable = false)
    var createdBy: UserEntity,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id")
    var booking: OpeningBookingEntity? = null,

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: Instant = Instant.now(),

    ) : BaseEntity() {

    override fun toString(): String {
        return this::class.simpleName + "(id=$id, valueChange=$valueChange, message=$message, balanceAfter=$balanceAfter)"
    }
}
