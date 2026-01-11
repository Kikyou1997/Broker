package com.notification.repository;

import com.notification.domain.NotificationJob;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface NotificationJobRepository extends JpaRepository<NotificationJob, Long> {

    @Query(value = "SELECT * FROM notification_job WHERE status IN (:statuses) AND next_retry_at <= :now ORDER BY priority ASC, next_retry_at ASC LIMIT :limit FOR UPDATE SKIP LOCKED", nativeQuery = true)
    List<NotificationJob> findJobsForRecovery(@Param("statuses") List<String> statuses, @Param("now") LocalDateTime now,
            @Param("limit") int limit);

    @Query("SELECT COUNT(j) FROM NotificationJob j WHERE j.updatedAt >= :since")
    long countJobsUpdatedSince(@Param("since") LocalDateTime since);

    @Query("SELECT COUNT(j) FROM NotificationJob j WHERE j.failureReason = :reason AND j.updatedAt >= :since")
    long countJobsWithFailureReasonSince(@Param("reason") com.notification.domain.FailureReason reason,
            @Param("since") LocalDateTime since);
}
