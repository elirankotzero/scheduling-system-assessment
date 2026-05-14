package com.scheduler.api.repository;

import com.scheduler.api.entity.ParameterSchema;
import com.scheduler.api.entity.TaskType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ParameterSchemaRepository extends JpaRepository<ParameterSchema, Long> {
    List<ParameterSchema> findByTaskType(TaskType taskType);
    List<ParameterSchema> findByTaskTypeAndRequired(TaskType taskType, boolean required);
}
