package com.scheduler.api.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.scheduler.api.dto.SchedulingRequest;
import com.scheduler.api.entity.*;
import com.scheduler.api.repository.ParameterSchemaRepository;
import com.scheduler.api.repository.SchedulingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SchedulingIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SchedulingRepository schedulingRepository;

    @Autowired
    private ParameterSchemaRepository parameterSchemaRepository;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        schedulingRepository.deleteAll();
        // Seed parameter schemas if not present
        if (parameterSchemaRepository.count() == 0) {
            parameterSchemaRepository.saveAll(List.of(
                    ParameterSchema.builder()
                            .taskType(TaskType.LOG_TASK)
                            .parameterName("message")
                            .parameterType(ParameterType.STRING)
                            .required(true)
                            .description("Log message")
                            .build(),
                    ParameterSchema.builder()
                            .taskType(TaskType.EMAIL_TASK)
                            .parameterName("to")
                            .parameterType(ParameterType.EMAIL)
                            .required(true)
                            .description("Recipient")
                            .build(),
                    ParameterSchema.builder()
                            .taskType(TaskType.EMAIL_TASK)
                            .parameterName("subject")
                            .parameterType(ParameterType.STRING)
                            .required(true)
                            .description("Subject")
                            .build()
            ));
        }
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    @Test
    void createSchedule_thenGetAll_returnsList() throws Exception {
        SchedulingRequest req = buildLogRequest("Integration test message");

        String body = mockMvc.perform(post("/api/schedules")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.taskName").value("Integration Log"))
                .andReturn().getResponse().getContentAsString();

        mockMvc.perform(get("/api/schedules"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))));
    }

    @Test
    void createSchedule_missingRequiredParam_returns400() throws Exception {
        SchedulingRequest req = buildLogRequest(null);
        req.setParameters(Map.of()); // missing "message"

        mockMvc.perform(post("/api/schedules")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("message")));
    }

    @Test
    void createAndDelete_schedule_removedFromDB() throws Exception {
        SchedulingRequest req = buildLogRequest("Delete me");

        String responseBody = mockMvc.perform(post("/api/schedules")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long id = objectMapper.readTree(responseBody).get("id").asLong();

        mockMvc.perform(delete("/api/schedules/" + id))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/schedules/" + id))
                .andExpect(status().isNotFound());
    }

    @Test
    void getSchemaForTask_returnsLogTaskSchema() throws Exception {
        mockMvc.perform(get("/api/parameter-schemas/task/LOG_TASK"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].parameterName").value("message"))
                .andExpect(jsonPath("$[0].required").value(true));
    }

    @Test
    void createEmailSchedule_withValidParams_returns201() throws Exception {
        SchedulingRequest req = new SchedulingRequest();
        req.setTaskName("Email Notification");
        req.setTaskType(TaskType.EMAIL_TASK);
        req.setScheduleType(ScheduleType.RECURRING_MINUTES);
        req.setScheduleExpression("30");
        req.setParameters(Map.of(
                "to", "test@example.com",
                "subject", "Daily Report"
        ));

        mockMvc.perform(post("/api/schedules")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.taskType").value("EMAIL_TASK"))
                .andExpect(jsonPath("$.scheduleType").value("RECURRING_MINUTES"));
    }

    // ------------------------------------------------------------------ helpers

    private SchedulingRequest buildLogRequest(String message) {
        SchedulingRequest req = new SchedulingRequest();
        req.setTaskName("Integration Log");
        req.setTaskType(TaskType.LOG_TASK);
        req.setScheduleType(ScheduleType.ONE_TIME);
        req.setScheduledAt(LocalDateTime.now().plusHours(1));
        req.setParameters(message != null ? Map.of("message", message) : null);
        return req;
    }
}
