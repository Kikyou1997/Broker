CREATE TABLE IF NOT EXISTS template_config (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE,
    content TEXT,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS notification_job (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    target_url VARCHAR(2048) NOT NULL,
    headers JSON,
    payload JSON,
    template_id BIGINT,
    status VARCHAR(50) NOT NULL,
    attempt_count INT DEFAULT 0,
    next_retry_at TIMESTAMP(6),
    created_at TIMESTAMP(6) DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    priority INT DEFAULT 3,
    INDEX idx_status_priority_next_retry (status, priority, next_retry_at),
    INDEX idx_status_next_retry (status, next_retry_at),
    FOREIGN KEY (template_id) REFERENCES template_config(id),
    ADD COLUMN failure_error_message TEXT,
    ADD COLUMN failure_reason VARCHAR(50)
);
