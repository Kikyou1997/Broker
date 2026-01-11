package com.notification.dto;

import lombok.Data;
import java.util.Map;

@Data
public class NotificationRequest {
    private String targetUrl;
    private Map<String, String> headers;
    private Map<String, Object> payload;
    private String templateName;
    private Integer priority; // 1 (High), 2 (Normal), 3 (Low)
}
