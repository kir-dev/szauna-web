package hu.kirdev.szaunaweb.opening

import hu.kirdev.szaunaweb.persistence.AuditedEntity
import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import jakarta.validation.constraints.NotBlank

@Entity
@Table(
    name = "opening_types",
    uniqueConstraints = [
        UniqueConstraint(name = "uq_opening_type_name", columnNames = ["name"])
    ]
)
class OpeningTypeEntity(
    @field:NotBlank
    @Column(name = "name", nullable = false)
    var name: String,

    @Column(name = "description")
    var description: String?,

    @Column(name = "default_price", nullable = false)
    var defaultPrice: Int,

    @Column(name = "active", nullable = false)
    var active: Boolean = true,

    @OneToMany(mappedBy = "openingType", fetch = FetchType.LAZY)
    var openings: MutableList<OpeningEntity> = mutableListOf()

) : AuditedEntity() {

    override fun toString(): String {
        return this::class.simpleName + "(id = $id, name = $name, description = $description)"
    }

}