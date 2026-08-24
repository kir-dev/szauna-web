package hu.kirdev.szaunaweb.opening
import hu.kirdev.szaunaweb.user.UserEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import java.time.Instant

@Entity
@Table(name = "opening_participants")
data class OpeningParticipantEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    var orderedBy: UserEntity,

    @Column(name = "on_waitingList", nullable = false)
    var onWaitingList: Boolean = false,

    @Column(name = "participants_count", nullable = false)
    var participantsCount: Int = 1,

    @Column(name = "appeared_count")
    var appearedCount: Int?,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "interval_id", nullable = false)
    var openingInterval: OpeningIntervalEntity,

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
        if (other !is OpeningParticipantEntity) return false
        if (id != other.id) return false
        return true
    }

    override fun toString(): String {
        return this::class.simpleName + "(id = $id, participantCount = $participantsCount, appearedCount = $appearedCount, createdAt = $createdAt, updatedAt = $updatedAt)"
    }
}
