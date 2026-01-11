package com.notification.service;

import com.notification.domain.TemplateConfig;
import com.notification.repository.TemplateConfigRepository;
import freemarker.cache.StringTemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.Template;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.StringWriter;
import java.util.Map;
import java.util.Optional;

@Service
@Slf4j
public class TemplateService {

    private final TemplateConfigRepository repository;
    private final Configuration freemarkerConfig;
    private final StringTemplateLoader stringLoader;

    public TemplateService(TemplateConfigRepository repository) {
        this.repository = repository;
        this.stringLoader = new StringTemplateLoader();
        this.freemarkerConfig = new Configuration(Configuration.VERSION_2_3_32);
        this.freemarkerConfig.setTemplateLoader(stringLoader);
        this.freemarkerConfig.setDefaultEncoding("UTF-8");
    }

    public String transform(Long templateId, Map<String, Object> model) {
        if (templateId == null) {
            return null;
        }
        try {
            // Check if template is known to loader, if not (or checking updates), fetch
            // from DB
            // specialized cache logic can be added here. For now, we fetch and put.
            // Note: StringTemplateLoader is thread-safe.
            String templateKey = String.valueOf(templateId);

            // Optimization: In real world, check if loader has it first?
            // FreeMarker Configuration cache is complex.
            // Simple approach: Always fetch content or check local cache.
            // Here we assume fetch.
            Optional<TemplateConfig> configOpt = repository.findById(templateId);
            if (configOpt.isEmpty()) {
                throw new IllegalArgumentException("Template not found: " + templateId);
            }
            String content = configOpt.get().getContent();

            stringLoader.putTemplate(templateKey, content);

            Template template = freemarkerConfig.getTemplate(templateKey);

            StringWriter writer = new StringWriter();
            template.process(model, writer);
            return writer.toString();
        } catch (Exception e) {
            log.error("Error transforming template {}", templateId, e);
            throw new RuntimeException("Transformation failed", e);
        }
    }
}
