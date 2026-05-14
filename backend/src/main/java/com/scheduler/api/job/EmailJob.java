package com.scheduler.api.job;

import com.scheduler.api.entity.ScheduleStatus;
import com.scheduler.api.repository.SchedulingRepository;
import lombok.extern.slf4j.Slf4j;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.quartz.QuartzJobBean;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

@Slf4j
public class EmailJob extends QuartzJobBean {

    @Autowired
    private SchedulingRepository schedulingRepository;

    @Override
    protected void executeInternal(JobExecutionContext context) throws JobExecutionException {
        String to = context.getMergedJobDataMap().getString("to");
        String subject = context.getMergedJobDataMap().getString("subject");
        Long schedulingId = context.getMergedJobDataMap().getLong("schedulingId");
        String taskName = context.getMergedJobDataMap().getString("taskName");

        log.info("[EmailTask] Executing '{}' (id={}): Sending email to='{}', subject='{}'",
                taskName, schedulingId, to, subject);
        log.info("[EmailTask] [SIMULATED] Email successfully dispatched to '{}'", to);

        Date nextFireTime = context.getTrigger().getNextFireTime();

        schedulingRepository.findById(schedulingId).ifPresent(scheduling -> {
            scheduling.setLastExecutedAt(LocalDateTime.now());

            // Refresh next execution time from the live trigger
            scheduling.setNextExecutionAt(nextFireTime != null
                    ? nextFireTime.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime()
                    : null);

            if ("ONE_TIME".equals(context.getMergedJobDataMap().getString("scheduleType"))) {
                scheduling.setStatus(ScheduleStatus.COMPLETED);
                scheduling.setNextExecutionAt(null);
            }
            schedulingRepository.save(scheduling);
        });
    }
}
