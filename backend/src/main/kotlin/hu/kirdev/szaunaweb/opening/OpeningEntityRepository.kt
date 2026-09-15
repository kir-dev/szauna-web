package hu.kirdev.szaunaweb.opening

import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDateTime
import java.util.UUID

interface OpeningEntityRepository : JpaRepository<OpeningEntity, Long> {

    @EntityGraph(
        attributePaths = [
            "hostedBy",
            "openingType",
            "intervals",
            "intervals.bookings"
        ]
    )
    @Query(
        """
        SELECT o FROM OpeningEntity o 
        WHERE o.status != :cancelledStatus 
            AND o.isPrivate IS false 
            AND o.openingEnd > :now 
        ORDER BY o.openingStart ASC 
        LIMIT 1
    """
    )
    fun findCurrentOrNextOpening(
        @Param("now") now: LocalDateTime,
        @Param("cancelledStatus") cancelledStatus: OpeningStatus = OpeningStatus.CANCELLED
    ): OpeningEntity?

    @EntityGraph(
        attributePaths = [
            "hostedBy",
            "openingType",
            "intervals",
            "intervals.bookings"
        ]
    )
    fun findByPublicId(publicId: UUID): OpeningEntity?

}