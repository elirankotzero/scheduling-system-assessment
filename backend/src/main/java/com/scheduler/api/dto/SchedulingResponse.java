package com.scheduler.api.dto;

import com.scheduler.api.entity.ScheduleStatus;
import com.scheduler.api.entity.ScheduleType;
import com.scheduler.api.entity.TaskType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
public class SchedulingResponse {
    private Long id;
    private String taskName;
    private TaskType taskType;
    private ScheduleType scheduleType;
    private String scheduleExpression;
    private Integer dayOfWeek;
    private String timeOfDay;
    private LocalDateTime scheduledAt;
    private Map<String, Object> parameters;
    private ScheduleStatus status;
    private String quartzJobKey;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime lastExecutedAt;
    private LocalDateTime nextExecutionAt;
}
