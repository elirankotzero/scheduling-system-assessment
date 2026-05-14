package com.scheduler.api.service;

import com.scheduler.api.dto.SchedulingRequest;
import com.scheduler.api.dto.SchedulingResponse;
import com.scheduler.api.entity.*;
import com.scheduler.api.exception.ParameterValidationException;
import com.scheduler.api.exception.SchedulingNotFoundException;
import com.scheduler.api.job.EmailJob;
import com.scheduler.api.job.LogJob;
import com.scheduler.api.repository.ParameterSchemaRepository;
import com.scheduler.api.repository.SchedulingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.*;
import org.quartz.impl.triggers.CronTriggerImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SchedulingService {

    private final SchedulingRepository schedulingRepository;
    private final ParameterSchemaRepository parameterSchemaRepository;
    private final Scheduler quartzScheduler;

    // ------------------------------------------------------------------ CRUD

    @Transactional
    public SchedulingResponse createSchedule(SchedulingRequest request) {
        validateParameters(request.getTaskType(), request.getParameters());

        Scheduling scheduling = Scheduling.builder()
                .taskName(request.getTaskName())
                .taskType(request.getTaskType())
                .scheduleType(request.getScheduleType())
                .scheduleExpression(request.getScheduleExpression())
                .dayOfWeek(request.getDayOfWeek())
                .timeOfDay(request.getTimeOfDay())
                .scheduledAt(request.getScheduledAt())
                .parameters(request.getParameters())
                .status(ScheduleStatus.ACTIVE)
                .build();

        scheduling = schedulingRepository.save(scheduling);

        try {
            String jobKey = scheduleQuartzJob(scheduling);
            scheduling.setQuartzJobKey(jobKey);
            scheduling.setNextExecutionAt(getNextFireTime(jobKey));
            scheduling = schedulingRepository.save(scheduling);
        } catch (SchedulerException e) {
            log.error("Failed to schedule Quartz job for scheduling id={}", scheduling.getId(), e);
            scheduling.setStatus(ScheduleStatus.ERROR);
            scheduling = schedulingRepository.save(scheduling);
        }

        return toResponse(scheduling);
    }

    @Transactional(readOnly = true)
    public List<SchedulingResponse> getAllSchedules() {
        return schedulingRepository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public SchedulingResponse getScheduleById(Long id) {
        return toResponse(findOrThrow(id));
    }

    @Transactional
    public SchedulingResponse updateSchedule(Long id, SchedulingRequest request) {
        Scheduling scheduling = findOrThrow(id);
        validateParameters(request.getTaskType(), request.getParameters());

        // Remove old Quartz job before rescheduling
        if (scheduling.getQuartzJobKey() != null) {
            removeQuartzJob(scheduling.getQuartzJobKey());
        }

        scheduling.setTaskName(request.getTaskName());
        scheduling.setTaskType(request.getTaskType());
        scheduling.setScheduleType(request.getScheduleType());
        scheduling.setScheduleExpression(request.getScheduleExpression());
        scheduling.setDayOfWeek(request.getDayOfWeek());
        scheduling.setTimeOfDay(request.getTimeOfDay());
        scheduling.setScheduledAt(request.getScheduledAt());
        scheduling.setParameters(request.getParameters());
        scheduling.setStatus(ScheduleStatus.ACTIVE);

        scheduling = schedulingRepository.save(scheduling);

        try {
            String jobKey = scheduleQuartzJob(scheduling);
            scheduling.setQuartzJobKey(jobKey);
            scheduling.setNextExecutionAt(getNextFireTime(jobKey));
            scheduling = schedulingRepository.save(scheduling);
        } catch (SchedulerException e) {
            log.error("Failed to reschedule Quartz job for scheduling id={}", id, e);
            scheduling.setStatus(ScheduleStatus.ERROR);
            scheduling = schedulingRepository.save(scheduling);
        }

        return toResponse(scheduling);
    }

    @Transactional
    public void deleteSchedule(Long id) {
        Scheduling scheduling = findOrThrow(id);
        if (scheduling.getQuartzJobKey() != null) {
            removeQuartzJob(scheduling.getQuartzJobKey());
        }
        schedulingRepository.deleteById(id);
    }

    @Transactional
    public SchedulingResponse pauseSchedule(Long id) {
        Scheduling scheduling = findOrThrow(id);
        if (scheduling.getQuartzJobKey() != null) {
            try {
                quartzScheduler.pauseJob(JobKey.jobKey(scheduling.getQuartzJobKey(), "scheduling"));
                scheduling.setStatus(ScheduleStatus.PAUSED);
                scheduling = schedulingRepository.save(scheduling);
            } catch (SchedulerException e) {
                log.error("Failed to pause job for scheduling id={}", id, e);
            }
        }
        return toResponse(scheduling);
    }

    @Transactional
    public SchedulingResponse resumeSchedule(Long id) {
        Scheduling scheduling = findOrThrow(id);
        if (scheduling.getQuartzJobKey() != null) {
            try {
                quartzScheduler.resumeJob(JobKey.jobKey(scheduling.getQuartzJobKey(), "scheduling"));
                scheduling.setStatus(ScheduleStatus.ACTIVE);
                scheduling.setNextExecutionAt(getNextFireTime(scheduling.getQuartzJobKey()));
                scheduling = schedulingRepository.save(scheduling);
            } catch (SchedulerException e) {
                log.error("Failed to resume job for scheduling id={}", id, e);
            }
        }
        return toResponse(scheduling);
    }

    // ------------------------------------------------------------------ Quartz helpers

    private String scheduleQuartzJob(Scheduling scheduling) throws SchedulerException {
        String jobId = "job-" + scheduling.getId();

        JobDataMap dataMap = buildJobDataMap(scheduling);
        Class<? extends Job> jobClass = resolveJobClass(scheduling.getTaskType());

        JobDetail jobDetail = JobBuilder.newJob(jobClass)
                .withIdentity(jobId, "scheduling")
                .usingJobData(dataMap)
                .storeDurably(false)
                .build();

        Trigger trigger = buildTrigger(scheduling, jobId);
        quartzScheduler.scheduleJob(jobDetail, trigger);

        log.info("Scheduled Quartz job '{}' for scheduling id={}", jobId, scheduling.getId());
        return jobId;
    }

    private JobDataMap buildJobDataMap(Scheduling scheduling) {
        JobDataMap map = new JobDataMap();
        map.put("schedulingId", scheduling.getId());
        map.put("taskName", scheduling.getTaskName());
        map.put("scheduleType", scheduling.getScheduleType().name());

        if (scheduling.getParameters() != null) {
            scheduling.getParameters().forEach((k, v) -> map.put(k, v != null ? v.toString() : ""));
        }
        return map;
    }

    private Trigger buildTrigger(Scheduling scheduling, String jobId) {
        String triggerId = "trigger-" + jobId;

        return switch (scheduling.getScheduleType()) {
            case ONE_TIME -> {
                Date fireTime = scheduling.getScheduledAt() != null
                        ? Date.from(scheduling.getScheduledAt().atZone(ZoneId.systemDefault()).toInstant())
                        : new Date(System.currentTimeMillis() + 5000); // 5s from now as fallback
                yield TriggerBuilder.newTrigger()
                        .withIdentity(triggerId, "scheduling")
                        .startAt(fireTime)
                        .withSchedule(SimpleScheduleBuilder.simpleSchedule().withRepeatCount(0))
                        .build();
            }
            case RECURRING_MINUTES -> {
                int interval = Integer.parseInt(scheduling.getScheduleExpression());
                yield TriggerBuilder.newTrigger()
                        .withIdentity(triggerId, "scheduling")
                        .startNow()
                        .withSchedule(SimpleScheduleBuilder.simpleSchedule()
                                .withIntervalInMinutes(interval)
                                .repeatForever())
                        .build();
            }
            case RECURRING_HOURS -> {
                int interval = Integer.parseInt(scheduling.getScheduleExpression());
                yield TriggerBuilder.newTrigger()
                        .withIdentity(triggerId, "scheduling")
                        .startNow()
                        .withSchedule(SimpleScheduleBuilder.simpleSchedule()
                                .withIntervalInHours(interval)
                                .repeatForever())
                        .build();
            }
            case WEEKLY -> {
                // Build cron: "0 <min> <hour> ? * <dayOfWeek>"
                String[] timeParts = (scheduling.getTimeOfDay() != null ? scheduling.getTimeOfDay() : "09:00").split(":");
                String cron = String.format("0 %s %s ? * %d",
                        timeParts[1], timeParts[0], scheduling.getDayOfWeek() + 1); // Quartz DOW: 1=SUN
                yield TriggerBuilder.newTrigger()
                        .withIdentity(triggerId, "scheduling")
                        .withSchedule(CronScheduleBuilder.cronSchedule(cron))
                        .build();
            }
            case CRON -> TriggerBuilder.newTrigger()
                    .withIdentity(triggerId, "scheduling")
                    .withSchedule(CronScheduleBuilder.cronSchedule(scheduling.getScheduleExpression()))
                    .build();
        };
    }

    private Class<? extends Job> resolveJobClass(TaskType taskType) {
        return switch (taskType) {
            case LOG_TASK -> LogJob.class;
            case EMAIL_TASK -> EmailJob.class;
        };
    }

    private void removeQuartzJob(String jobKey) {
        try {
            quartzScheduler.deleteJob(JobKey.jobKey(jobKey, "scheduling"));
        } catch (SchedulerException e) {
            log.warn("Could not remove Quartz job '{}': {}", jobKey, e.getMessage());
        }
    }

    private LocalDateTime getNextFireTime(String jobKey) {
        try {
            List<? extends Trigger> triggers = quartzScheduler.getTriggersOfJob(
                    JobKey.jobKey(jobKey, "scheduling"));
            if (!triggers.isEmpty()) {
                Date next = triggers.get(0).getNextFireTime();
                if (next != null) {
                    return next.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
                }
            }
        } catch (SchedulerException e) {
            log.warn("Could not get next fire time for job '{}'", jobKey);
        }
        return null;
    }

    // ------------------------------------------------------------------ Validation

    private void validateParameters(TaskType taskType, Map<String, Object> parameters) {
        List<String> requiredParams = parameterSchemaRepository
                .findByTaskTypeAndRequired(taskType, true)
                .stream()
                .map(p -> p.getParameterName())
                .collect(Collectors.toList());

        List<String> missing = requiredParams.stream()
                .filter(name -> parameters == null || !parameters.containsKey(name)
                        || parameters.get(name) == null
                        || parameters.get(name).toString().isBlank())
                .collect(Collectors.toList());

        if (!missing.isEmpty()) {
            throw new ParameterValidationException(missing);
        }
    }

    // ------------------------------------------------------------------ Mappers

    private Scheduling findOrThrow(Long id) {
        return schedulingRepository.findById(id)
                .orElseThrow(() -> new SchedulingNotFoundException(id));
    }

    private SchedulingResponse toResponse(Scheduling s) {
        return SchedulingResponse.builder()
                .id(s.getId())
                .taskName(s.getTaskName())
                .taskType(s.getTaskType())
                .scheduleType(s.getScheduleType())
                .scheduleExpression(s.getScheduleExpression())
                .dayOfWeek(s.getDayOfWeek())
                .timeOfDay(s.getTimeOfDay())
                .scheduledAt(s.getScheduledAt())
                .parameters(s.getParameters())
                .status(s.getStatus())
                .quartzJobKey(s.getQuartzJobKey())
                .createdAt(s.getCreatedAt())
                .updatedAt(s.getUpdatedAt())
                .lastExecutedAt(s.getLastExecutedAt())
                .nextExecutionAt(s.getNextExecutionAt())
                .build();
    }
}
