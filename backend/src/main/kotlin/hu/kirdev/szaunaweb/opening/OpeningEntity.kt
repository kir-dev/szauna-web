package hu.kirdev.szaunaweb.opening

import hu.kirdev.szaunaweb.persistence.AuditedEntity
import hu.kirdev.szaunaweb.user.UserEntity
import jakarta.persistence.CascadeType
import jakarta.persistence.CheckConstraint
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.Index
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
    name = "openings",
    uniqueConstraints = [
        UniqueConstraint(name = "uq_openings_public_id", columnNames = ["public_id"])
    ],
    indexes = [
        Index(name = "idx_openings_start", columnList = "opening_start"),
        Index(name = "idx_openings_hosted_by", columnList = "hosted_by_id"),
    ],
    check = [
        CheckConstraint(name = "ck_openings_range", constraint = "opening_end > opening_start")
    ]
)
class OpeningEntity(
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    @Column(name = "public_id", nullable = false, updatable = false, unique = true)
    var publicId: UUID? = null,

    @Column(name = "opening_start", nullable = false)
    var openingStart: LocalDateTime,

    @Column(name = "opening_end", nullable = false)
    var openingEnd: LocalDateTime,

    @Column(name = "is_private", nullable = false)
    var isPrivate: Boolean = false,

    @field:Min(0)
    @Column(name = "price", nullable = false)
    var price: Int,

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    var status: OpeningStatus = OpeningStatus.SCHEDULED,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "hosted_by_id", nullable = false)
    var hostedBy: UserEntity,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "opening_type_id", nullable = false)
    var openingType: OpeningTypeEntity,

    @OneToMany(mappedBy = "opening", cascade = [CascadeType.PERSIST, CascadeType.MERGE])
    var intervals: MutableList<OpeningIntervalEntity> = mutableListOf(),

    ) : AuditedEntity() {

    override fun toString(): String {
        return this::class.simpleName + "(id = $id, publicId = $publicId, price = $price, createdAt = $createdAt, updatedAt = $updatedAt)"
    }
}
