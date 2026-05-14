package com.scheduler.api.service;

import com.scheduler.api.dto.ParameterSchemaResponse;
import com.scheduler.api.entity.TaskType;
import com.scheduler.api.repository.ParameterSchemaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ParameterSchemaService {

    private final ParameterSchemaRepository parameterSchemaRepository;

    public List<ParameterSchemaResponse> getSchemaForTask(TaskType taskType) {
        return parameterSchemaRepository.findByTaskType(taskType).stream()
                .map(s -> ParameterSchemaResponse.builder()
                        .id(s.getId())
                        .taskType(s.getTaskType())
                        .parameterName(s.getParameterName())
                        .parameterType(s.getParameterType())
                        .required(s.isRequired())
                        .description(s.getDescription())
                        .defaultValue(s.getDefaultValue())
                        .build())
                .collect(Collectors.toList());
    }

    public List<ParameterSchemaResponse> getAllSchemas() {
        return parameterSchemaRepository.findAll().stream()
                .map(s -> ParameterSchemaResponse.builder()
                        .id(s.getId())
                        .taskType(s.getTaskType())
                        .parameterName(s.getParameterName())
                        .parameterType(s.getParameterType())
                        .required(s.isRequired())
                        .description(s.getDescription())
                        .defaultValue(s.getDefaultValue())
                        .build())
                .collect(Collectors.toList());
    }
}
