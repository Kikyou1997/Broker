package com.notification.service;

import com.notification.domain.NotificationJob;
import com.notification.domain.NotificationStatus;
import com.notification.dto.NotificationRequest;
import com.notification.repository.NotificationJobRepository;
import com.notification.repository.TemplateConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationJobRepository repository;
    private final TemplateConfigRepository templateRepository;
    private final QueueManager queueManager;

    @Transactional
    public Long submitNotification(NotificationRequest request) {
        NotificationJob job = new NotificationJob();
        job.setTargetUrl(request.getTargetUrl());
        job.setHeaders(request.getHeaders());
        job.setPayload(request.getPayload());

        if (request.getTemplateName() != null) {
            var template = templateRepository.findByName(request.getTemplateName())
                    .orElseThrow(
                            () -> new IllegalArgumentException("Template not found: " + request.getTemplateName()));
            job.setTemplateId(template.getId());
        }

        job.setStatus(NotificationStatus.PENDING);

        job.setPriority(request.getPriority() != null ? request.getPriority() : 3);
        job.setNextRetryAt(LocalDateTime.now());

        job = repository.save(job);

        // Push to memory queue (After transaction commit in a real world - here
        // simpfied or use TransactionSynchronization)
        // ideally we should use
        // TransactionSynchronizationManager.registerSynchronization ... afterCommit
        // For simplicity now, we push here. If tx fails, queue might have an ID that
        // doesn't exist (handled by consumer check).
        queueManager.push(job);

        log.info("Submitted job id={} priority={}", job.getId(), job.getPriority());
        return job.getId();
    }
}
