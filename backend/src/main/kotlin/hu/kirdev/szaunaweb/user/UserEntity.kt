package hu.kirdev.szaunaweb.user

import hu.kirdev.szaunaweb.opening.OpeningBookingEntity
import hu.kirdev.szaunaweb.persistence.AuditedEntity
import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import org.hibernate.annotations.UuidGenerator
import java.time.Instant
import java.util.UUID

@Entity
@Table(
    name = "users",
    uniqueConstraints = [
        UniqueConstraint(name = "uq_user_public_id", columnNames = ["public_id"]),
        UniqueConstraint(name = "uq_user_auth_sub", columnNames = ["auth_sub"]),
        UniqueConstraint(name = "uq_user_email", columnNames = ["email"]),
    ]
)
class UserEntity(
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    @Column(name = "public_id", nullable = false, unique = true, updatable = false)
    var publicId: UUID? = null,

    @Column(name = "auth_sub", nullable = false, unique = true, updatable = false)
    var authSub: String,

    @Column(name = "email", nullable = false, unique = true)
    var email: String,

    @Column(name = "display_name", nullable = false)
    var displayName: String,

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    var role: UserRole = UserRole.USER,

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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "banned_by_id")
    var bannedBy: UserEntity? = null,

    @Column(name = "banned_at")
    var bannedAt: Instant? = null,

    @OneToMany(mappedBy = "orderedBy", cascade = [CascadeType.ALL], fetch = FetchType.LAZY, orphanRemoval = true)
    var bookings: MutableList<OpeningBookingEntity> = mutableListOf(),

    ): AuditedEntity(){
    override fun toString(): String = "UserEntity(id=$id, publicId=$publicId, authSub=$authSub, role=$role)"
}