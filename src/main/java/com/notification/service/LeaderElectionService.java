package com.notification.service;

import com.notification.repository.LeaderElectionRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class LeaderElectionService {

    private final LeaderElectionRepository repository;
    private final String hostId = UUID.randomUUID().toString();
    private static final String SERVICE_NAME = "notification-service";
    private static final int LEASE_SECONDS = 10;

    @PostConstruct
    public void init() {
        log.info("Initialized LeaderElectionService with HostID: {}", hostId);
    }

    @Scheduled(fixedDelay = 2000) // Heartbeat every 2s
    @Transactional
    public void heartbeat() {
        try {
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime leaseExpiry = now.minusSeconds(LEASE_SECONDS);

            int rows = repository.tryAcquireOrRenewLease(SERVICE_NAME, hostId, now, leaseExpiry);
            if (rows > 0 && isLeader()) {
                log.debug("Lease renewed for host {}", hostId);
            }
        } catch (Exception e) {
            log.error("Error during leader election heartbeat", e);
        }
    }

    @Transactional(readOnly = true)
    public boolean isLeader() {
        return repository.isLeader(SERVICE_NAME, hostId);
    }
}
