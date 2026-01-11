package com.notification.service;

import com.notification.domain.FailureReason;
import com.notification.repository.NotificationJobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class SystemMonitorService {

    private final NotificationJobRepository jobRepository;
    private final LeaderElectionService leaderElectionService;
    private final AlarmService alarmService;

    @Scheduled(fixedRate = 60000) // Run every 1 minute
    public void monitorSystemHealth() {
        if (!leaderElectionService.isLeader()) {
            return;
        }

        LocalDateTime fiveMinutesAgo = LocalDateTime.now().minusMinutes(5);

        long totalJobs = jobRepository.countJobsUpdatedSince(fiveMinutesAgo);
        if (totalJobs == 0) {
            return; // No traffic, no alarm
        }

        long failedJobs = jobRepository.countJobsWithFailureReasonSince(FailureReason.EXTERNAL_SERVICE_UNAVAILABLE,
                fiveMinutesAgo);

        double failureRate = (double) failedJobs / totalJobs;

        if (failureRate > 0.5) {
            String message = String.format(
                    "High Failure Rate Detected! %.2f%% of requests failed due to External Service Unavailable in the last 5 minutes (%d/%d)",
                    failureRate * 100, failedJobs, totalJobs);
            alarmService.sendAlarm(message);
        } else {
            log.info("System Health OK: Failure Rate %.2f%% (%d/%d)", failureRate * 100, failedJobs, totalJobs);
        }
    }
}
