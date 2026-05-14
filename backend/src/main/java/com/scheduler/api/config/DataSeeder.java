package com.scheduler.api.config;

import com.scheduler.api.entity.ParameterSchema;
import com.scheduler.api.entity.ParameterType;
import com.scheduler.api.entity.TaskType;
import com.scheduler.api.repository.ParameterSchemaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataSeeder implements ApplicationRunner {

    private final ParameterSchemaRepository parameterSchemaRepository;

    @Override
    public void run(ApplicationArguments args) {
        if (parameterSchemaRepository.count() > 0) {
            return; // already seeded
        }

        List<ParameterSchema> schemas = List.of(
                // LogTask parameters
                ParameterSchema.builder()
                        .taskType(TaskType.LOG_TASK)
                        .parameterName("message")
                        .parameterType(ParameterType.STRING)
                        .required(true)
                        .description("The message to write to the application log")
                        .build(),

                // EmailTask parameters
                ParameterSchema.builder()
                        .taskType(TaskType.EMAIL_TASK)
                        .parameterName("to")
                        .parameterType(ParameterType.EMAIL)
                        .required(true)
                        .description("Recipient email address")
                        .build(),
                ParameterSchema.builder()
                        .taskType(TaskType.EMAIL_TASK)
                        .parameterName("subject")
                        .parameterType(ParameterType.STRING)
                        .required(true)
                        .description("Email subject line")
                        .build(),
                ParameterSchema.builder()
                        .taskType(TaskType.EMAIL_TASK)
                        .parameterName("body")
                        .parameterType(ParameterType.STRING)
                        .required(false)
                        .description("Optional email body content")
                        .build()
        );

        parameterSchemaRepository.saveAll(schemas);
        log.info("Seeded {} parameter schemas", schemas.size());
    }
}
