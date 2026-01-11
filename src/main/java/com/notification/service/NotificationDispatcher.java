package com.notification.service;

import com.notification.domain.NotificationJob;
import com.notification.domain.NotificationStatus;
import com.notification.repository.NotificationJobRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Component
@Slf4j
@RequiredArgsConstructor
public class NotificationDispatcher {

    private final QueueManager queueManager;
    private final NotificationProcessor processor;
    private final NotificationJobRepository repository;

    // Separate executors for different priorities
    // Level 1 (High)
    private final ExecutorService highPriorityExecutor = Executors.newVirtualThreadPerTaskExecutor();

    // Level 2/3 (Standard)
    private final ExecutorService standardPriorityExecutor = Executors.newVirtualThreadPerTaskExecutor();

    private final LeaderElectionService leaderElectionService;

    @PostConstruct
    public void startConsumers() {
        // High Priority Consumer
        Thread highPriorityThread = new Thread(() -> {
            while (true) {
                try {
                    NotificationJob job = queueManager.getHighPriorityQueue().take();
                    dispatchWithTimeout(job, highPriorityExecutor);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });

        highPriorityThread.setName("HighPriorityConsumer");
        highPriorityThread.start();

        // Standard Priority Consumer
        Thread standardPriorityThread = new Thread(() -> {
            while (true) {
                try {
                    QueueManager.JobItem item = queueManager.getStandardPriorityQueue().take();
                    dispatchWithTimeout(item.job(), standardPriorityExecutor);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });

        standardPriorityThread.setName("StandardPriorityConsumer");
        standardPriorityThread.start();
    }

    @Scheduled(fixedDelay = 5000) // Run every 5 seconds
    public void recoveryPoller() {
        if (!leaderElectionService.isLeader()) {
            log.trace("Not leader, skipping recovery poller");
            return;
        }

        log.debug("Running recovery poller...");

        int batchSize = 50;

        List<NotificationJob> retryJobs = repository.findJobsForRecovery(
                List.of(NotificationStatus.FAILED.name()),
                LocalDateTime.now(),
                batchSize);
        for (NotificationJob job : retryJobs) {
            log.info("Recovered failed job for retry {}", job.getId());
            queueManager.push(job);
        }
    }

    private static final int TASK_TIMEOUT_SECONDS = 30;

    private void dispatchWithTimeout(NotificationJob job, ExecutorService executor) {

        java.util.concurrent.CompletableFuture.runAsync(() -> processor.process(job), executor)
                .orTimeout(TASK_TIMEOUT_SECONDS, java.util.concurrent.TimeUnit.SECONDS)
                .exceptionally(ex -> {
                    if (ex instanceof java.util.concurrent.TimeoutException) {
                        log.warn("Job {} timed out after {}s", job.getId(), TASK_TIMEOUT_SECONDS);
                        processor.handleFailureInternal(job, "Timeout",
                                com.notification.domain.FailureReason.UNKNOWN);
                    } else {
                        log.error("Job {} execution error", job.getId(), ex);
                    }
                    return null;
                });
    }
}
