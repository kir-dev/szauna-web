package hu.kirdev.szaunaweb.inventory

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(name = "inventory_history")
data class InventoryHistoryEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0,

    @Column(name = "type", nullable = false)
    @Enumerated(EnumType.STRING)
    var type: InventoryHistoryType,

    @Column(name = "message")
    var message: String? = null,

    @Column(name = "quantity", nullable = false)
    var quantity: Int = 0,

    @Column(name = "total_cost")
    var totalCost: Int? = null,

    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: Instant = Instant.now(),

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "inventory_id", nullable = false)
    var inventory: InventoryEntity
) {

    override fun hashCode(): Int {
        return javaClass.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is InventoryHistoryEntity) return false
        if (id != other.id) return false
        return true
    }

    override fun toString(): String {
        return this::class.simpleName + "(id = $id, type=${type.name}, quantity=$quantity, totalCost=$totalCost, createdAt=$createdAt )"
    }

}
