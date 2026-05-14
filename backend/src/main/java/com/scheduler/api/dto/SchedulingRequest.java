package com.scheduler.api.dto;

import com.scheduler.api.entity.ScheduleType;
import com.scheduler.api.entity.TaskType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

@Data
public class SchedulingRequest {

    @NotBlank(message = "Task name is required")
    private String taskName;

    @NotNull(message = "Task type is required")
    private TaskType taskType;

    @NotNull(message = "Schedule type is required")
    private ScheduleType scheduleType;

    // For CRON: cron expression. For RECURRING_MINUTES/HOURS: interval value as string.
    private String scheduleExpression;

    // For WEEKLY: 0=Sunday through 6=Saturday
    private Integer dayOfWeek;

    // For ONE_TIME and WEEKLY: "HH:mm"
    private String timeOfDay;

    // For ONE_TIME: exact execution timestamp
    private LocalDateTime scheduledAt;

    private Map<String, Object> parameters;
}
