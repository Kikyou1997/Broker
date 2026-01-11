package com.notification.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class MockAlarmService implements AlarmService {
    @Override
    public void sendAlarm(String message) {
        log.error("ALARM TRIGGERED: {}", message);
        // In a real implementation, this would send an email, Slack message, or
        // PagerDuty alert.
    }
}
