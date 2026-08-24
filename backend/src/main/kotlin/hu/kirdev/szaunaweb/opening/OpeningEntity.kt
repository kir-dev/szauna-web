package hu.kirdev.szaunaweb.opening

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import org.hibernate.annotations.UuidGenerator
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import java.time.Instant
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(
    name = "openings",
    uniqueConstraints = [
        UniqueConstraint(name = "uq_openings_public_id", columnNames = ["public_id"])
    ]
)
data class OpeningEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id:Long = 0,

    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    @Column(name = "public_id", nullable = false, updatable = false, unique = true)
    var publicId: UUID? = null,

    @Column(name = "opening_start", nullable = false)
    var openingStart: LocalDateTime,

    @Column(name = "opening_end", nullable = false)
    var openingEnd: LocalDateTime,

    @Column(name = "is_private", nullable = false)
    var isPrivate: Boolean = false,

    @Column(name = "price", nullable = false)
    var price: Int,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "opening_type_id", nullable = false)
    var openingType: OpeningTypeEntity,

    @OneToMany(mappedBy = "opening", cascade = [CascadeType.ALL], orphanRemoval = true)
    var intervals: MutableList<OpeningIntervalEntity> = mutableListOf(),

    @CreatedDate
    @Column(name = "created_at", updatable = false, nullable = false)
    var createdAt: Instant = Instant.now(),

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now(),
){
    override fun hashCode(): Int {
        return javaClass.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is OpeningEntity) return false
        if(id != other.id) return false
        return true
    }

    override fun toString(): String {
        return this::class.simpleName + "(id = $id, publicId = $publicId, price = $price, createdAt = $createdAt, updatedAt = $updatedAt)"
    }
}
