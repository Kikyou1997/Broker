package com.notification.repository;

import com.notification.domain.LeaderElection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface LeaderElectionRepository extends JpaRepository<LeaderElection, String> {

    @Modifying
    @Query(value = "INSERT INTO leader_election (service_name, host_id, last_seen_active) VALUES (:serviceName, :hostId, :now) "
            +
            "ON DUPLICATE KEY UPDATE host_id = IF(last_seen_active < :leaseExpiry, :hostId, host_id), " +
            "last_seen_active = IF(host_id = :hostId, :now, last_seen_active)", nativeQuery = true)
    int tryAcquireOrRenewLease(@Param("serviceName") String serviceName,
            @Param("hostId") String hostId,
            @Param("now") LocalDateTime now,
            @Param("leaseExpiry") LocalDateTime leaseExpiry);

    // Check if the current host is the leader
    @Query("SELECT COUNT(l) > 0 FROM LeaderElection l WHERE l.serviceName = :serviceName AND l.hostId = :hostId")
    boolean isLeader(@Param("serviceName") String serviceName, @Param("hostId") String hostId);
}
