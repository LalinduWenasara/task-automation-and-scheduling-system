package com.adl.dc.ep.taskautomation.task_automation_and_scheduling_system.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;

import javax.sql.DataSource;
import java.util.Properties;

@Configuration
public class QuartzConfig {
    // No explicit SchedulerFactoryBean – Spring Boot uses application.yaml
}
