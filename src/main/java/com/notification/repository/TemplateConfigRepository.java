package com.notification.repository;

import com.notification.domain.TemplateConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface TemplateConfigRepository extends JpaRepository<TemplateConfig, Long> {
    Optional<TemplateConfig> findByName(String name);
}
