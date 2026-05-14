package com.scheduler.api.controller;

import com.scheduler.api.dto.TaskDefinition;
import com.scheduler.api.entity.TaskType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Exposes the predefined, read-only task catalogue.
 * Task types are code-level constants; this endpoint allows the UI to discover
 * them dynamically without hardcoding the list on the frontend.
 */
@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    private static final List<TaskDefinition> TASKS = List.of(
            TaskDefinition.builder()
                    .taskType(TaskType.LOG_TASK)
                    .label("Log Task")
                    .description("Writes a message to the application log")
                    .build(),
            TaskDefinition.builder()
                    .taskType(TaskType.EMAIL_TASK)
                    .label("Email Task")
                    .description("Sends a dummy email notification")
                    .build()
    );

    @GetMapping
    public ResponseEntity<List<TaskDefinition>> getTasks() {
        return ResponseEntity.ok(TASKS);
    }
}
