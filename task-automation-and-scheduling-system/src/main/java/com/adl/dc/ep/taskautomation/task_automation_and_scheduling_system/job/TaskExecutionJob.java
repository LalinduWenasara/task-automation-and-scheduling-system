package com.adl.dc.ep.taskautomation.task_automation_and_scheduling_system.job;

import com.adl.dc.ep.taskautomation.task_automation_and_scheduling_system.domain.Task;
import com.adl.dc.ep.taskautomation.task_automation_and_scheduling_system.domain.TaskExecution;
import com.adl.dc.ep.taskautomation.task_automation_and_scheduling_system.domain.User;
import com.adl.dc.ep.taskautomation.task_automation_and_scheduling_system.dto.OpenWeatherResponseDto;
import com.adl.dc.ep.taskautomation.task_automation_and_scheduling_system.enums.ExecutionStatus;
import com.adl.dc.ep.taskautomation.task_automation_and_scheduling_system.exception.ExternalServiceException;
import com.adl.dc.ep.taskautomation.task_automation_and_scheduling_system.exception.InvalidTaskPayloadException;
import com.adl.dc.ep.taskautomation.task_automation_and_scheduling_system.exception.TaskNotFoundException;
import com.adl.dc.ep.taskautomation.task_automation_and_scheduling_system.exception.UserNotFoundException;
import com.adl.dc.ep.taskautomation.task_automation_and_scheduling_system.repository.TaskExecutionRepository;
import com.adl.dc.ep.taskautomation.task_automation_and_scheduling_system.repository.TaskRepository;
import com.adl.dc.ep.taskautomation.task_automation_and_scheduling_system.repository.UserRepository;
import com.adl.dc.ep.taskautomation.task_automation_and_scheduling_system.service.EmailService;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;


import java.time.LocalDateTime;

@Component
public class TaskExecutionJob implements Job {

    private static final Logger logger = LoggerFactory.getLogger(TaskExecutionJob.class);


    private final TaskRepository taskRepository;

    private final TaskExecutionRepository executionRepository;

    private final UserRepository userRepository;

    private final EmailService emailService;


    @Value("${weather.api-key}")
    private String weatherApiKey;

    @Value("${weather.base-url}")
    private String weatherBaseUrl;

    @Value("${weather.units:metric}")
    private String weatherUnits;

    public TaskExecutionJob(TaskRepository taskRepository, TaskExecutionRepository executionRepository,
                            UserRepository userRepository,
                            EmailService emailService) {
        this.taskRepository = taskRepository;
        this.executionRepository = executionRepository;
        this.userRepository = userRepository;
        this.emailService = emailService;
    }

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        logger.info("TaskExecutionJob.execute started");

        JobDataMap dataMap = context.getMergedJobDataMap();
        // JobDataMap values are stored as Strings (because useProperties = true)
        String taskIdStr = dataMap.getString("taskId");

        if (taskIdStr == null) {
            logger.error("taskId is missing from JobDataMap");
            throw new JobExecutionException("taskId is missing from JobDataMap");
        }

        Long taskId;
        try {
            taskId = Long.valueOf(taskIdStr);
        } catch (NumberFormatException e) {
            logger.error("Invalid taskId '{}' in JobDataMap", taskIdStr, e);
            throw new JobExecutionException("Invalid taskId in JobDataMap: " + taskIdStr, e);
        }

        logger.info("Executing task with id={}", taskId);

        TaskExecution execution = new TaskExecution();
        execution.setTaskId(taskId);
        execution.setStatus(ExecutionStatus.RUNNING);
        execution.setStartTime(LocalDateTime.now());
        execution = executionRepository.save(execution);

        try {
            Task task = taskRepository.findById(taskId)
                    .orElseThrow(() -> {
                        logger.error("Task not found for id={}", taskId);
                        return new TaskNotFoundException(taskId);
                    });

            String result = executeTaskLogic(task);

            execution.setEndTime(LocalDateTime.now());
            execution.setStatus(ExecutionStatus.SUCCESS);
            execution.setResult(result);

            task.setLastExecutedAt(LocalDateTime.now());
            taskRepository.save(task);
            logger.info("Task id={} executed successfully. Result={}", taskId, result);
        } catch (Exception e) {
            logger.error("Error executing task id={}: {}", taskId, e.getMessage(), e);
            execution.setEndTime(LocalDateTime.now());
            execution.setStatus(ExecutionStatus.FAILED);
            execution.setErrorMessage(e.getMessage());
        } finally {
            executionRepository.save(execution);
        }
    }

    private String executeTaskLogic(Task task) {
        switch (task.getTaskType()) {
            case EMAIL:
                return executeEmailTask(task);
            case HTTP_REQUEST:
                return executeHttpTask(task);
            case DATA_SYNC:
                return executeDataSyncTask(task);
            case WEATHER:
                return executeWeatherTask(task);
            default:
                return "Task executed successfully";
        }
    }

    private String executeEmailTask(Task task) {
        emailService.sendTaskNotification(task);
        logger.info("Email sent successfully");
        return "Email sent successfully";
    }

    private String executeHttpTask(Task task) {
        RestTemplate restTemplate = new RestTemplate();
        String url = task.getActionPayload();
        try {
            String response = restTemplate.getForObject(url, String.class);
            return "HTTP request completed: " + response;
        } catch (Exception e) {
            throw new ExternalServiceException("HTTP request failed: " + e.getMessage());
        }
    }

    private String executeDataSyncTask(Task task) {
        return task.getId().toString()+"Data synced successfully";
    }


    private String executeWeatherTask(Task task) {

        // Read the location
        String location = task.getActionPayload();

        if (location == null || location.isBlank()) {
            throw new InvalidTaskPayloadException("Location is required for WEATHER tasks");
        }

        // Build the OpenWeather API URL with required query parameters
        String url = UriComponentsBuilder.fromUriString(weatherBaseUrl)
                .queryParam("q", location)        // location provided by user
                .queryParam("appid", weatherApiKey) // OpenWeather API key
                .queryParam("units", weatherUnits)  // temperature unit (metric/imperial)
                .build()
                .toUriString();

        // Call the OpenWeather API
        OpenWeatherResponseDto weather =
                new RestTemplate().getForObject(url, OpenWeatherResponseDto.class);

        // Validate the API response
        if (weather == null || weather.getMain() == null) {
            throw new ExternalServiceException("Invalid response from OpenWeather API");
        }

        // Fetch the user who owns this task
        User user = userRepository.findById(task.getUserId())
                .orElseThrow(() -> new UserNotFoundException(task.getUserId()));

        // Extract temperature value from the weather response
        double temp = weather.getMain().getTemp();

        // Extract weather description if available (fallback to 'N/A')
        String desc = weather.getWeather() != null && !weather.getWeather().isEmpty()
                ? weather.getWeather().get(0).getDescription()
                : "N/A";

        // Send weather information to the user via email
        emailService.sendSimpleEmail(
                user.getEmail(),
                "Weather Update for " + location,
                "Temperature: %.1f °C\nCondition: %s".formatted(temp, desc)
        );

        // Return execution result
        return "Weather email sent to " + user.getEmail();
    }

}
