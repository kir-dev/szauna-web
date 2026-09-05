package hu.kirdev.szaunaweb.inventory

import hu.kirdev.szaunaweb.persistence.AuditedEntity
import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank

@Entity
@Table(
    name = "inventory",
    uniqueConstraints = [
        UniqueConstraint(name = "uq_inventory_name", columnNames = ["name"]),
    ]
)
class InventoryEntity(

    @field:NotBlank
    @Column(name = "name", nullable = false)
    var name: String,

    @field:Min(0)
    @Column(name = "quantity", nullable = false)
    var quantity: Int = 0,

    @OneToMany(mappedBy = "inventory", cascade = [CascadeType.ALL], fetch = FetchType.LAZY, orphanRemoval = true)
    var history: MutableList<InventoryHistoryEntity> = mutableListOf(),
) : AuditedEntity() {

    override fun toString(): String {
        return this::class.simpleName + "(id = $id, name = $name, quantity = $quantity, updatedAt = $updatedAt)"
    }
}