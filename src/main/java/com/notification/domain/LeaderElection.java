package com.notification.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "leader_election")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LeaderElection {
    @Id
    private String serviceName;
    private String hostId;
    private LocalDateTime lastSeenActive;
}
