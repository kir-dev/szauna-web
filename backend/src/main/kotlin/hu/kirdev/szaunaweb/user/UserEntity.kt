package hu.kirdev.szaunaweb.user

import hu.kirdev.szaunaweb.opening.OpeningParticipantEntity
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
import java.util.UUID

@Entity
@Table(
    name = "users",
    uniqueConstraints = [
        UniqueConstraint(name = "uq_user_public_id", columnNames = ["public_id"]),
        UniqueConstraint(name = "uq_user_auth_sub", columnNames = ["auth_sub"])
    ]
)
data class UserEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0,

    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    @Column(name = "public_id", nullable = false, unique = true, updatable = false)
    var publicId: UUID? = null,

    @Column(name = "auth_sub", nullable = false, unique = true, updatable = false)
    var authSub: String,

    @Column(name = "email", nullable = false, unique = true)
    var email: String,

    @Column(name = "request_reminders", nullable = false)
    var requestReminders: Boolean = true,

    @Column(name = "balance", nullable = false)
    var balance: Int = 0,

    @OneToMany(mappedBy = "user", cascade = [CascadeType.ALL], fetch = FetchType.LAZY, orphanRemoval = true)
    var balanceHistory: MutableList<BalanceHistoryEntity> = mutableListOf(),

    @Column(name = "is_banned", nullable = false)
    var isBanned: Boolean = false,

    @Column(name = "ban_reason")
    var banReason: String? = null,

    @OneToMany(mappedBy = "orderedBy", cascade = [CascadeType.ALL], fetch = FetchType.LAZY, orphanRemoval = true)
    var bookings: MutableList<OpeningParticipantEntity> = mutableListOf(),

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
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
        if (other !is UserEntity) return false
        if (id != other.id) return false
        return true
    }

    override fun toString(): String {
        return this::class.simpleName+"(id=$id, publicId=$publicId, authSub=$authSub)"
    }
}
