CREATE TABLE IF NOT EXISTS leader_election (
    service_name VARCHAR(255) PRIMARY KEY,
    host_id VARCHAR(255) NOT NULL,
    last_seen_active TIMESTAMP NOT NULL
);
