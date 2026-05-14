package com.scheduler.api.controller;

import com.scheduler.api.dto.ParameterSchemaResponse;
import com.scheduler.api.entity.TaskType;
import com.scheduler.api.service.ParameterSchemaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/parameter-schemas")
@RequiredArgsConstructor
public class ParameterSchemaController {

    private final ParameterSchemaService parameterSchemaService;

    @GetMapping
    public ResponseEntity<List<ParameterSchemaResponse>> getAll() {
        return ResponseEntity.ok(parameterSchemaService.getAllSchemas());
    }

    @GetMapping("/task/{taskType}")
    public ResponseEntity<List<ParameterSchemaResponse>> getByTaskType(@PathVariable TaskType taskType) {
        return ResponseEntity.ok(parameterSchemaService.getSchemaForTask(taskType));
    }
}
