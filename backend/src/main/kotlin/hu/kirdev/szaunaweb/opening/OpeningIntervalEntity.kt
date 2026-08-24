package hu.kirdev.szaunaweb.opening

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.JoinColumns
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import org.hibernate.annotations.UuidGenerator
import org.springframework.data.annotation.LastModifiedDate
import java.time.Instant
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(
    name = "opening_intervals",
    uniqueConstraints = [
        UniqueConstraint(name = "uq_opening_intervals_public_id", columnNames = ["public_id"])
    ]
)
data class OpeningIntervalEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0,

    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    @Column(name = "public_id", nullable = false, updatable = false, unique = true)
    var publicId: UUID? = null,

    @Column(name = "interval_start", nullable = false)
    var intervalStart: LocalDateTime,

    @Column(name = "interval_end", nullable = false)
    var intervalEnd: LocalDateTime,

    @Column(name = "participant_limit", nullable = false)
    var participantLimit: Int = 0,

    @OneToMany(mappedBy = "openingInterval", cascade = [CascadeType.ALL], fetch = FetchType.LAZY, orphanRemoval = true)
    var participants: MutableList<OpeningParticipantEntity> = mutableListOf(),

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now(),

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "opening_id", nullable = false)
    var opening: OpeningEntity
) {
    override fun hashCode(): Int {
        return javaClass.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is OpeningIntervalEntity) return false
        if (id != other.id) return false
        return true
    }

    override fun toString(): String {
        return this::class.simpleName + "(id = $id, publicId = $publicId, intervalStart = $intervalStart, intervalEnd = $intervalEnd, participantLimit = $participantLimit)"
    }
}
