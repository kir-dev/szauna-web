package hu.kirdev.szaunaweb.inventory

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import org.springframework.data.annotation.LastModifiedDate
import java.time.Instant

@Entity
@Table(name = "inventory")
data class InventoryEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0,

    @Column(name = "name", nullable = false)
    var name: String,

    @Column(name = "quantity", nullable = false)
    var quantity: Int = 0,

    @OneToMany(mappedBy = "inventory", cascade = [CascadeType.ALL], fetch = FetchType.LAZY, orphanRemoval = true)
    var history: MutableList<InventoryHistoryEntity> = mutableListOf(),

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now(),
) {

    override fun hashCode(): Int {
        return javaClass.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is InventoryEntity) return false
        if (id != other.id) return false
        return true
    }

    override fun toString(): String {
        return this::class.simpleName + "(id = $id, name = $name, quantity = $quantity, updatedAt = $updatedAt)"
    }
}