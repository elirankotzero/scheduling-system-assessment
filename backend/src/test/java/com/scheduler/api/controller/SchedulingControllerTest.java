package com.scheduler.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.scheduler.api.dto.SchedulingRequest;
import com.scheduler.api.dto.SchedulingResponse;
import com.scheduler.api.entity.*;
import com.scheduler.api.exception.GlobalExceptionHandler;
import com.scheduler.api.exception.SchedulingNotFoundException;
import com.scheduler.api.service.SchedulingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class SchedulingControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock
    private SchedulingService schedulingService;

    @InjectMocks
    private SchedulingController schedulingController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(schedulingController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    @Test
    void getAll_returnsListOfSchedules() throws Exception {
        SchedulingResponse r1 = buildResponse(1L, "Log Task");
        SchedulingResponse r2 = buildResponse(2L, "Email Task");
        when(schedulingService.getAllSchedules()).thenReturn(List.of(r1, r2));

        mockMvc.perform(get("/api/schedules"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[1].id").value(2));
    }

    @Test
    void getById_existing_returnsSchedule() throws Exception {
        when(schedulingService.getScheduleById(1L)).thenReturn(buildResponse(1L, "My Task"));

        mockMvc.perform(get("/api/schedules/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.taskName").value("My Task"));
    }

    @Test
    void getById_notFound_returns404() throws Exception {
        when(schedulingService.getScheduleById(99L)).thenThrow(new SchedulingNotFoundException(99L));

        mockMvc.perform(get("/api/schedules/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void create_validRequest_returns201() throws Exception {
        SchedulingRequest req = buildRequest();
        SchedulingResponse resp = buildResponse(1L, "Test");
        when(schedulingService.createSchedule(any())).thenReturn(resp);

        mockMvc.perform(post("/api/schedules")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void create_missingTaskName_returns400() throws Exception {
        SchedulingRequest req = buildRequest();
        req.setTaskName("");

        mockMvc.perform(post("/api/schedules")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void update_existing_returnsUpdated() throws Exception {
        SchedulingRequest req = buildRequest();
        SchedulingResponse resp = buildResponse(1L, "Updated");
        when(schedulingService.updateSchedule(eq(1L), any())).thenReturn(resp);

        mockMvc.perform(put("/api/schedules/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void delete_existing_returns204() throws Exception {
        doNothing().when(schedulingService).deleteSchedule(1L);

        mockMvc.perform(delete("/api/schedules/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    void pause_existing_returnsPaused() throws Exception {
        SchedulingResponse resp = buildResponse(1L, "Task");
        resp.setStatus(ScheduleStatus.PAUSED);
        when(schedulingService.pauseSchedule(1L)).thenReturn(resp);

        mockMvc.perform(post("/api/schedules/1/pause"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAUSED"));
    }

    // ------------------------------------------------------------------ helpers

    private SchedulingRequest buildRequest() {
        SchedulingRequest req = new SchedulingRequest();
        req.setTaskName("Test Task");
        req.setTaskType(TaskType.LOG_TASK);
        req.setScheduleType(ScheduleType.ONE_TIME);
        req.setScheduledAt(LocalDateTime.now().plusHours(1));
        req.setParameters(Map.of("message", "hello"));
        return req;
    }

    private SchedulingResponse buildResponse(Long id, String name) {
        return SchedulingResponse.builder()
                .id(id)
                .taskName(name)
                .taskType(TaskType.LOG_TASK)
                .scheduleType(ScheduleType.ONE_TIME)
                .status(ScheduleStatus.ACTIVE)
                .parameters(Map.of("message", "test"))
                .createdAt(LocalDateTime.now())
                .build();
    }
}
