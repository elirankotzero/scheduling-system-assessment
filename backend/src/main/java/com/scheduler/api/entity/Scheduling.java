package com.scheduler.api.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;

@Entity
@Table(name = "schedulings")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Scheduling {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false)
    private String taskName;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TaskType taskType;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ScheduleType scheduleType;

    // Cron expression for CRON type, or interval value for RECURRING types
    private String scheduleExpression;

    // Day of week (0=Sunday, 6=Saturday) for WEEKLY type
    private Integer dayOfWeek;

    // Time of day for ONE_TIME and WEEKLY (HH:mm format)
    private String timeOfDay;

    // Specific datetime for ONE_TIME schedules
    private LocalDateTime scheduledAt;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column
    private Map<String, Object> parameters;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private ScheduleStatus status = ScheduleStatus.ACTIVE;

    // Quartz job key reference
    private String quartzJobKey;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private LocalDateTime lastExecutedAt;

    private LocalDateTime nextExecutionAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
