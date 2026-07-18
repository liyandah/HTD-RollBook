package org.salvationarmy.whatsapp.repository;

import org.salvationarmy.whatsapp.entity.Payment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, UUID> {
    Page<Payment> findByMemberIdOrderByRecordedAtDesc(UUID memberId, Pageable pageable);
    List<Payment> findByMemberIdOrderByRecordedAtDesc(UUID memberId);
    Page<Payment> findByCategoryIdOrderByRecordedAtDesc(UUID categoryId, Pageable pageable);
    Page<Payment> findByProjectIdOrderByRecordedAtDesc(UUID projectId, Pageable pageable);
    Page<Payment> findByEventIdOrderByRecordedAtDesc(UUID eventId, Pageable pageable);
    Page<Payment> findByRecordedByUserIdOrderByRecordedAtDesc(UUID recordedByUserId, Pageable pageable);
    
    @Query("SELECT SUM(p.amount) FROM Payment p WHERE p.recordedAt >= :start AND p.recordedAt < :end")
    BigDecimal sumAmountByDateRange(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
    
    @Query("SELECT SUM(p.amount) FROM Payment p WHERE p.categoryId = :categoryId AND p.recordedAt >= :start AND p.recordedAt < :end")
    BigDecimal sumAmountByCategoryAndDateRange(@Param("categoryId") UUID categoryId, 
                                               @Param("start") LocalDateTime start, 
                                               @Param("end") LocalDateTime end);
    
    @Query("SELECT SUM(p.amount) FROM Payment p WHERE p.projectId = :projectId")
    BigDecimal sumAmountByProject(@Param("projectId") UUID projectId);
    
    @Query("SELECT SUM(p.amount) FROM Payment p WHERE p.eventId = :eventId")
    BigDecimal sumAmountByEvent(@Param("eventId") UUID eventId);
    
    @Query("SELECT p FROM Payment p WHERE p.recordedAt >= :start AND p.recordedAt < :end ORDER BY p.recordedAt DESC")
    List<Payment> findByDateRange(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
}
