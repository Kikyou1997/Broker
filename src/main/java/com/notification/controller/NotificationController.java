package com.notification.controller;

import com.notification.dto.NotificationRequest;
import com.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

import com.notification.service.LeaderElectionService;
import org.springframework.http.HttpStatus;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService service;
    private final LeaderElectionService leaderElectionService;

    @PostMapping
    public ResponseEntity<?> submit(@RequestBody NotificationRequest request) {
        if (!leaderElectionService.isLeader()) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body("Not Leader");
        }
        Long jobId = service.submitNotification(request);
        return ResponseEntity.accepted().body(Map.of("jobId", jobId));
    }
}
