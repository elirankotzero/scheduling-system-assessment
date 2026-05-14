package com.scheduler.api.dto;

import com.scheduler.api.entity.TaskType;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TaskDefinition {
    private TaskType taskType;
    private String label;
    private String description;
}
