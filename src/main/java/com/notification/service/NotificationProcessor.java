package com.notification.service;

import com.notification.domain.NotificationJob;
import com.notification.domain.NotificationStatus;
import com.notification.domain.FailureReason;
import com.notification.repository.NotificationJobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.ResourceAccessException;

import java.time.LocalDateTime;

@Component
@Slf4j
@RequiredArgsConstructor
public class NotificationProcessor {

    private final NotificationJobRepository repository;
    private final TemplateService templateService;
    private final RestClient.Builder restClientBuilder;

    // Requires new transaction to ensure status update is committed independently
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void process(NotificationJob job) {
        // Build client here or use pre-built if configured.
        // Ideally inject RestClient if it's singleton, but for testing we might want to
        // mock the builder or the client.
        // If we inject Builder, we can build it.
        RestClient restClient = restClientBuilder.build();

        if (job == null) {
            return;
        }

        if (job.getStatus() == NotificationStatus.SUCCESS || job.getStatus() == NotificationStatus.MAX_RETRIES) {
            return; // Already done
        }

        try {
            String payloadBody;
            if (job.getTemplateId() != null) {
                payloadBody = templateService.transform(job.getTemplateId(), job.getPayload());
            } else {
                payloadBody = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(job.getPayload());
            }

            // Perform HTTP Request
            restClient.post()
                    .uri(job.getTargetUrl())
                    .headers(headers -> {
                        if (job.getHeaders() != null) {
                            job.getHeaders().forEach(headers::add);
                        }
                    })
                    .body(payloadBody)
                    .retrieve()
                    .toBodilessEntity();

            // Success
            job.setStatus(NotificationStatus.SUCCESS);
            log.info("Job {} success", job.getId());

        } catch (RestClientResponseException e) {
            log.error("Job {} failed with status {}: {}", job.getId(), e.getStatusCode(), e.getMessage());
            FailureReason reason = e.getStatusCode().is4xxClientError() ? FailureReason.CLIENT_ERROR
                    : FailureReason.EXTERNAL_SERVICE_UNAVAILABLE;
            handleFailureInternal(job, e.getMessage(), reason);
        } catch (ResourceAccessException e) {
            log.error("Job {} failed with I/O error: {}", job.getId(), e.getMessage());
            handleFailureInternal(job, e.getMessage(), FailureReason.EXTERNAL_SERVICE_UNAVAILABLE);
        } catch (Exception e) {
            log.error("Job {} failed: {}", job.getId(), e.getMessage());
            handleFailureInternal(job, e.getMessage(), FailureReason.UNKNOWN);
        }
        repository.save(job);
    }

    // Made public/package-private for Dispatcher timeout handling
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleFailureInternal(NotificationJob job, String errorMessage, FailureReason reason) {
        if (job == null)
            return;

        // Refresh job from DB to get latest version?
        // Or assume passed job is OK. If timeout happens, job object might be stale?
        // Safest is to reload, but we modified QueueManager to pass job.
        // Let's reload to be safe against concurrency since this is an error path.
        // But if we reload, we lose the changes made in process()? process() hasn't
        // saved yet if it timed out.
        // So reloading is correct.

        try {
            job = repository.findById(job.getId()).orElse(job);
        } catch (Exception e) {
            log.error("Cannot find job {}", job.getId());
            return;
        }

        log.warn("Marking job {} failed due to: {} ({})", job.getId(), reason, errorMessage);

        job.setFailureErrorMessage(errorMessage);
        job.setFailureReason(reason);

        job.setAttemptCount(job.getAttemptCount() + 1);
        if (job.getAttemptCount() >= 5) {
            job.setStatus(NotificationStatus.MAX_RETRIES);
        } else {
            job.setStatus(NotificationStatus.FAILED);
            // Exponential backoff
            long seconds = (long) Math.pow(2, job.getAttemptCount());
            job.setNextRetryAt(LocalDateTime.now().plusSeconds(seconds));
        }
        repository.save(job);
    }
}
