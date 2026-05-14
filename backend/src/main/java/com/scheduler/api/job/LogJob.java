package com.scheduler.api.job;

import com.scheduler.api.entity.ScheduleStatus;
import com.scheduler.api.repository.SchedulingRepository;
import lombok.extern.slf4j.Slf4j;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.quartz.QuartzJobBean;

import java.time.LocalDateTime;

@Slf4j
public class LogJob extends QuartzJobBean {

    @Autowired
    private SchedulingRepository schedulingRepository;

    @Override
    protected void executeInternal(JobExecutionContext context) throws JobExecutionException {
        String message = context.getMergedJobDataMap().getString("message");
        Long schedulingId = context.getMergedJobDataMap().getLong("schedulingId");
        String taskName = context.getMergedJobDataMap().getString("taskName");

        log.info("[LogTask] Executing '{}' (id={}): {}", taskName, schedulingId, message);

        // Update last executed time and status in DB
        schedulingRepository.findById(schedulingId).ifPresent(scheduling -> {
            scheduling.setLastExecutedAt(LocalDateTime.now());
            // Mark ONE_TIME jobs as COMPLETED after execution
            if ("ONE_TIME".equals(context.getMergedJobDataMap().getString("scheduleType"))) {
                scheduling.setStatus(ScheduleStatus.COMPLETED);
            }
            schedulingRepository.save(scheduling);
        });
    }
}
