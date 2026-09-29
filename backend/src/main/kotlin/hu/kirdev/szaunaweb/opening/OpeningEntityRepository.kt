package hu.kirdev.szaunaweb.opening

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDateTime
import java.util.UUID

interface OpeningEntityRepository : JpaRepository<OpeningEntity, Long> {

    fun existsByStatusNotInAndOpeningStartLessThanAndOpeningEndGreaterThan(
        statuses: List<OpeningStatus>,
        end: LocalDateTime,
        start: LocalDateTime
    ): Boolean

    fun existsByPublicIdNotAndStatusNotInAndOpeningStartLessThanAndOpeningEndGreaterThan(
        publicId: UUID,
        statuses: List<OpeningStatus>,
        end: LocalDateTime,
        start: LocalDateTime
    ): Boolean

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
            WHERE (:status IS NULL OR o.status = :status)
            AND (:from IS NULL OR o.openingStart >= :from)
            AND (:to IS NULL OR o.openingEnd <= :to)
            AND (o.status != 'DELETED')
            AND (:hasAccessToAll = true OR o.isPrivate = false)
        """
    )
    fun findOpeningWithFilter(
        @Param("status") status: OpeningStatus?,
        @Param("from") from: LocalDateTime?,
        @Param("to") to: LocalDateTime?,
        pageable: Pageable,
        @Param("hasAccessToAll") hasAccessToAll: Boolean
    ): Page<OpeningEntity>

}