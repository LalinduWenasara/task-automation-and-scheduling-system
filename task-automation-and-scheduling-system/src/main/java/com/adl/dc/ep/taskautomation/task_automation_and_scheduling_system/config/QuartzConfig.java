package com.adl.dc.ep.taskautomation.task_automation_and_scheduling_system.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;

import javax.sql.DataSource;
import java.util.Properties;

@Configuration
public class QuartzConfig {

    @Bean
    public SchedulerFactoryBean schedulerFactoryBean(DataSource dataSource) {
        SchedulerFactoryBean factory = new SchedulerFactoryBean();
        factory.setDataSource(dataSource);
        factory.setOverwriteExistingJobs(true);
        factory.setAutoStartup(true);

        Properties quartzProperties = new Properties();
        quartzProperties.setProperty("org.quartz.scheduler.instanceName", "TaskScheduler");
        quartzProperties.setProperty("org.quartz.scheduler.instanceId", "AUTO");
        quartzProperties.setProperty("org.quartz.threadPool.threadCount", "10");

        factory.setQuartzProperties(quartzProperties);
        return factory;
    }
}
