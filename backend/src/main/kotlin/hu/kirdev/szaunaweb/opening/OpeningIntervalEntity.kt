package hu.kirdev.szaunaweb.opening

import hu.kirdev.szaunaweb.persistence.AuditedEntity
import jakarta.persistence.CascadeType
import jakarta.persistence.CheckConstraint
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import jakarta.validation.constraints.Min
import org.hibernate.annotations.UuidGenerator
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(
    name = "opening_intervals",
    uniqueConstraints = [
        UniqueConstraint(name = "uq_opening_intervals_public_id", columnNames = ["public_id"])
    ],
    check = [
        CheckConstraint(name = "ck_opening_intervals_range", constraint = "interval_end > interval_start")
    ]
)
class OpeningIntervalEntity(
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    @Column(name = "public_id", nullable = false, updatable = false, unique = true)
    var publicId: UUID? = null,

    @Column(name = "interval_start", nullable = false)
    var intervalStart: LocalDateTime,

    @Column(name = "interval_end", nullable = false)
    var intervalEnd: LocalDateTime,

    @field:Min(1)
    @Column(name = "participant_limit", nullable = false)
    var participantLimit: Int = DEFAULT_PARTICIPANT_LIMIT,

    @OneToMany(
        mappedBy = "openingInterval",
        cascade = [CascadeType.PERSIST, CascadeType.MERGE],
        fetch = FetchType.LAZY
    )
    var bookings: MutableList<OpeningBookingEntity> = mutableListOf(),

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "opening_id", nullable = false)
    var opening: OpeningEntity,

    @Column(name = "cancelled", nullable = false)
    var cancelled: Boolean = false,

) : AuditedEntity() {

    override fun toString(): String {
        return this::class.simpleName + "(id = $id, publicId = $publicId, intervalStart = $intervalStart, intervalEnd = $intervalEnd, participantLimit = $participantLimit)"
    }

    companion object {
        const val DEFAULT_PARTICIPANT_LIMIT = 8
    }

}
