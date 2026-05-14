package com.scheduler.api.dto;

import com.scheduler.api.entity.ParameterType;
import com.scheduler.api.entity.TaskType;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ParameterSchemaResponse {
    private Long id;
    private TaskType taskType;
    private String parameterName;
    private ParameterType parameterType;
    private boolean required;
    private String description;
    private String defaultValue;
}
