package com.scheduler.api.repository;

import com.scheduler.api.entity.Scheduling;
import com.scheduler.api.entity.ScheduleStatus;
import com.scheduler.api.entity.TaskType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SchedulingRepository extends JpaRepository<Scheduling, Long> {
    List<Scheduling> findByStatus(ScheduleStatus status);
    List<Scheduling> findByTaskType(TaskType taskType);
    Optional<Scheduling> findByQuartzJobKey(String quartzJobKey);
}
