package com.scheduler.api.config;

import org.springframework.boot.autoconfigure.quartz.SchedulerFactoryBeanCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.util.Properties;

/**
 * Applies JDBC job-store-specific Quartz properties only in non-test environments.
 * Keeping them out of application.yml prevents RAMJobStore (used in tests) from
 * failing on unknown setter properties like useProperties or driverDelegateClass.
 */
@Configuration
@Profile("!test")
public class QuartzJdbcConfig {

    @Bean
    public SchedulerFactoryBeanCustomizer quartzJdbcCustomizer() {
        return factory -> {
            Properties props = new Properties();
            props.setProperty("org.quartz.jobStore.driverDelegateClass",
                    "org.quartz.impl.jdbcjobstore.PostgreSQLDelegate");
            props.setProperty("org.quartz.jobStore.tablePrefix", "QRTZ_");
            props.setProperty("org.quartz.jobStore.isClustered", "false");
            props.setProperty("org.quartz.jobStore.useProperties", "false");
            factory.setQuartzProperties(props);
        };
    }
}
