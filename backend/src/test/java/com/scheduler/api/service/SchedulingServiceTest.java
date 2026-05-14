package com.scheduler.api.service;

import com.scheduler.api.dto.SchedulingRequest;
import com.scheduler.api.dto.SchedulingResponse;
import com.scheduler.api.entity.*;
import com.scheduler.api.exception.ParameterValidationException;
import com.scheduler.api.exception.SchedulingNotFoundException;
import com.scheduler.api.repository.ParameterSchemaRepository;
import com.scheduler.api.repository.SchedulingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.quartz.*;

import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SchedulingServiceTest {

    @Mock
    private SchedulingRepository schedulingRepository;

    @Mock
    private ParameterSchemaRepository parameterSchemaRepository;

    @Mock
    private Scheduler quartzScheduler;

    @InjectMocks
    private SchedulingService schedulingService;

    private ParameterSchema messageSchema;

    @BeforeEach
    void setUp() {
        messageSchema = ParameterSchema.builder()
                .id(1L)
                .taskType(TaskType.LOG_TASK)
                .parameterName("message")
                .parameterType(ParameterType.STRING)
                .required(true)
                .build();
    }

    @Test
    void createSchedule_withValidParams_returnsResponse() throws SchedulerException {
        SchedulingRequest req = buildLogRequest("Test log message");

        Scheduling saved = buildSchedulingEntity(1L, req);
        when(parameterSchemaRepository.findByTaskTypeAndRequired(TaskType.LOG_TASK, true))
                .thenReturn(List.of(messageSchema));
        when(schedulingRepository.save(any(Scheduling.class))).thenReturn(saved);
        when(quartzScheduler.scheduleJob(any(JobDetail.class), any(Trigger.class))).thenReturn(new Date());
        when(quartzScheduler.getTriggersOfJob(any(JobKey.class))).thenReturn(Collections.emptyList());

        SchedulingResponse response = schedulingService.createSchedule(req);

        assertThat(response).isNotNull();
        assertThat(response.getTaskType()).isEqualTo(TaskType.LOG_TASK);
        verify(schedulingRepository, times(2)).save(any(Scheduling.class));
    }

    @Test
    void createSchedule_missingRequiredParam_throwsParameterValidationException() {
        SchedulingRequest req = new SchedulingRequest();
        req.setTaskName("My log");
        req.setTaskType(TaskType.LOG_TASK);
        req.setScheduleType(ScheduleType.ONE_TIME);
        req.setParameters(Map.of()); // missing "message"

        when(parameterSchemaRepository.findByTaskTypeAndRequired(TaskType.LOG_TASK, true))
                .thenReturn(List.of(messageSchema));

        assertThatThrownBy(() -> schedulingService.createSchedule(req))
                .isInstanceOf(ParameterValidationException.class)
                .hasMessageContaining("message");
    }

    @Test
    void getScheduleById_notFound_throwsNotFoundException() {
        when(schedulingRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> schedulingService.getScheduleById(99L))
                .isInstanceOf(SchedulingNotFoundException.class);
    }

    @Test
    void getScheduleById_found_returnsResponse() {
        Scheduling entity = buildSchedulingEntity(1L, buildLogRequest("hello"));
        when(schedulingRepository.findById(1L)).thenReturn(Optional.of(entity));

        SchedulingResponse response = schedulingService.getScheduleById(1L);

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getTaskName()).isEqualTo("Test Log");
    }

    @Test
    void getAllSchedules_returnsAll() {
        Scheduling e1 = buildSchedulingEntity(1L, buildLogRequest("msg1"));
        Scheduling e2 = buildSchedulingEntity(2L, buildLogRequest("msg2"));
        when(schedulingRepository.findAll()).thenReturn(List.of(e1, e2));

        List<SchedulingResponse> responses = schedulingService.getAllSchedules();

        assertThat(responses).hasSize(2);
    }

    @Test
    void deleteSchedule_notFound_throwsNotFoundException() {
        when(schedulingRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> schedulingService.deleteSchedule(99L))
                .isInstanceOf(SchedulingNotFoundException.class);
    }

    @Test
    void deleteSchedule_found_deletesAndRemovesQuartzJob() throws SchedulerException {
        Scheduling entity = buildSchedulingEntity(1L, buildLogRequest("msg"));
        entity.setQuartzJobKey("job-1");
        when(schedulingRepository.findById(1L)).thenReturn(Optional.of(entity));
        when(quartzScheduler.deleteJob(any(JobKey.class))).thenReturn(true);

        schedulingService.deleteSchedule(1L);

        verify(schedulingRepository).deleteById(1L);
        verify(quartzScheduler).deleteJob(JobKey.jobKey("job-1", "scheduling"));
    }

    @Test
    void updateSchedule_withValidData_updatesAndReschedules() throws SchedulerException {
        Scheduling existing = buildSchedulingEntity(1L, buildLogRequest("old message"));
        existing.setQuartzJobKey("job-1");
        SchedulingRequest updatedReq = buildLogRequest("new message");

        when(schedulingRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(parameterSchemaRepository.findByTaskTypeAndRequired(TaskType.LOG_TASK, true))
                .thenReturn(List.of(messageSchema));
        when(quartzScheduler.deleteJob(any(JobKey.class))).thenReturn(true);
        when(schedulingRepository.save(any(Scheduling.class))).thenReturn(existing);
        when(quartzScheduler.scheduleJob(any(JobDetail.class), any(Trigger.class))).thenReturn(new Date());
        when(quartzScheduler.getTriggersOfJob(any(JobKey.class))).thenReturn(Collections.emptyList());

        SchedulingResponse response = schedulingService.updateSchedule(1L, updatedReq);

        assertThat(response).isNotNull();
        verify(quartzScheduler).deleteJob(any(JobKey.class));
    }

    // ------------------------------------------------------------------ helpers

    private SchedulingRequest buildLogRequest(String message) {
        SchedulingRequest req = new SchedulingRequest();
        req.setTaskName("Test Log");
        req.setTaskType(TaskType.LOG_TASK);
        req.setScheduleType(ScheduleType.ONE_TIME);
        req.setScheduledAt(LocalDateTime.now().plusMinutes(10));
        req.setParameters(Map.of("message", message));
        return req;
    }

    private Scheduling buildSchedulingEntity(Long id, SchedulingRequest req) {
        return Scheduling.builder()
                .id(id)
                .taskName(req.getTaskName())
                .taskType(req.getTaskType())
                .scheduleType(req.getScheduleType())
                .parameters(req.getParameters())
                .status(ScheduleStatus.ACTIVE)
                .build();
    }
}
