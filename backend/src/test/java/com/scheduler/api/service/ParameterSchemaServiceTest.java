package com.scheduler.api.service;

import com.scheduler.api.dto.ParameterSchemaResponse;
import com.scheduler.api.entity.ParameterSchema;
import com.scheduler.api.entity.ParameterType;
import com.scheduler.api.entity.TaskType;
import com.scheduler.api.repository.ParameterSchemaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ParameterSchemaServiceTest {

    @Mock
    private ParameterSchemaRepository parameterSchemaRepository;

    @InjectMocks
    private ParameterSchemaService parameterSchemaService;

    @Test
    void getSchemaForTask_returnsMatchingSchemas() {
        ParameterSchema schema = ParameterSchema.builder()
                .id(1L)
                .taskType(TaskType.LOG_TASK)
                .parameterName("message")
                .parameterType(ParameterType.STRING)
                .required(true)
                .description("Log message")
                .build();

        when(parameterSchemaRepository.findByTaskType(TaskType.LOG_TASK))
                .thenReturn(List.of(schema));

        List<ParameterSchemaResponse> result = parameterSchemaService.getSchemaForTask(TaskType.LOG_TASK);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getParameterName()).isEqualTo("message");
        assertThat(result.get(0).isRequired()).isTrue();
        assertThat(result.get(0).getParameterType()).isEqualTo(ParameterType.STRING);
    }

    @Test
    void getSchemaForTask_noSchemas_returnsEmpty() {
        when(parameterSchemaRepository.findByTaskType(TaskType.EMAIL_TASK))
                .thenReturn(List.of());

        List<ParameterSchemaResponse> result = parameterSchemaService.getSchemaForTask(TaskType.EMAIL_TASK);

        assertThat(result).isEmpty();
    }

    @Test
    void getAllSchemas_returnsAllTaskSchemas() {
        List<ParameterSchema> all = List.of(
                ParameterSchema.builder().id(1L).taskType(TaskType.LOG_TASK)
                        .parameterName("message").parameterType(ParameterType.STRING).required(true).build(),
                ParameterSchema.builder().id(2L).taskType(TaskType.EMAIL_TASK)
                        .parameterName("to").parameterType(ParameterType.EMAIL).required(true).build(),
                ParameterSchema.builder().id(3L).taskType(TaskType.EMAIL_TASK)
                        .parameterName("subject").parameterType(ParameterType.STRING).required(true).build()
        );

        when(parameterSchemaRepository.findAll()).thenReturn(all);

        List<ParameterSchemaResponse> result = parameterSchemaService.getAllSchemas();

        assertThat(result).hasSize(3);
        assertThat(result).extracting(ParameterSchemaResponse::getTaskType)
                .containsExactly(TaskType.LOG_TASK, TaskType.EMAIL_TASK, TaskType.EMAIL_TASK);
    }
}
