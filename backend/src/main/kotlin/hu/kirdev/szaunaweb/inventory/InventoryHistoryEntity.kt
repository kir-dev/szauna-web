package hu.kirdev.szaunaweb.inventory

import hu.kirdev.szaunaweb.persistence.BaseEntity
import hu.kirdev.szaunaweb.user.UserEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.validation.constraints.Min
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.Instant

@Entity
@EntityListeners(AuditingEntityListener::class)
@Table(name = "inventory_history")
class InventoryHistoryEntity(
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    var type: InventoryHistoryType,

    @Column(name = "message")
    var message: String? = null,

    @field:Min(0)
    @Column(name = "quantity", nullable = false)
    var quantity: Int = 0,

    @Column(name = "total_cost")
    var totalCost: Int? = null,

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: Instant = Instant.now(),

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "inventory_id", nullable = false)
    var inventory: InventoryEntity,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by_id", nullable = false)
    var createdBy: UserEntity,
) : BaseEntity() {

    override fun toString(): String =
        "InventoryHistoryEntity(id=$id, type=${type.name}, quantity=$quantity, totalCost=$totalCost, createdAt=$createdAt)"
}
