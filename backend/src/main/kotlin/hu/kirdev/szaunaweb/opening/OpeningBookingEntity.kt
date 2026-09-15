package hu.kirdev.szaunaweb.opening

import hu.kirdev.szaunaweb.persistence.AuditedEntity
import hu.kirdev.szaunaweb.user.UserEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.Index
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import jakarta.validation.constraints.Min
import org.hibernate.annotations.UuidGenerator
import java.time.Instant
import java.util.UUID

@Entity
@Table(
    name = "opening_bookings",
    uniqueConstraints = [
        UniqueConstraint(name = "uq_opening_bookings_public_id", columnNames = ["public_id"]),
        UniqueConstraint(name = "uq_opening_bookings_user_interval", columnNames = ["user_id", "interval_id"]),
    ],
    indexes = [
        Index(name = "idx_opening_bookings_interval", columnList = "interval_id"),
        Index(name = "idx_opening_bookings_status", columnList = "status")
    ]
)
class OpeningBookingEntity(

    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    @Column(name = "public_id", nullable = false, updatable = false, unique = true)
    var publicId: UUID? = null,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    var orderedBy: UserEntity,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by_id", nullable = false)
    var createdBy: UserEntity,

    @field:Min(1)
    @Column(name = "seat_count", nullable = false)
    var seatCount: Int = 1,

    @Column(name = "appeared_count")
    var appearedCount: Int? = null,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "interval_id", nullable = false)
    var openingInterval: OpeningIntervalEntity,

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    var status: BookingStatus = BookingStatus.ACTIVE,

    @Column(name = "charged_amount")
    var chargedAmount: Int? = null,

    @Column(name = "charged_at")
    var chargedAt: Instant? = null,

    ) : AuditedEntity() {

    override fun toString(): String {
        return this::class.simpleName + "(id = $id, seatCount = $seatCount, appearedCount = $appearedCount, createdAt = $createdAt, updatedAt = $updatedAt)"
    }
}
