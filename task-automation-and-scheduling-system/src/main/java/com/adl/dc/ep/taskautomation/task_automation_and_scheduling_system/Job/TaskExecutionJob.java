package com.adl.dc.ep.taskautomation.task_automation_and_scheduling_system.Job;

import com.adl.dc.ep.taskautomation.task_automation_and_scheduling_system.domain.Task;
import com.adl.dc.ep.taskautomation.task_automation_and_scheduling_system.domain.TaskExecution;
import com.adl.dc.ep.taskautomation.task_automation_and_scheduling_system.enums.ExecutionStatus;
import com.adl.dc.ep.taskautomation.task_automation_and_scheduling_system.repository.TaskExecutionRepository;
import com.adl.dc.ep.taskautomation.task_automation_and_scheduling_system.repository.TaskRepository;
import com.adl.dc.ep.taskautomation.task_automation_and_scheduling_system.service.EmailService;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;

@Component
public class TaskExecutionJob implements Job {

    private static final Logger logger = LoggerFactory.getLogger(TaskExecutionJob.class);

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private TaskExecutionRepository executionRepository;

    @Autowired
    private EmailService emailService;

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
                        return new RuntimeException("Task not found for id=" + taskId);
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
            default:
                return "Task executed successfully";
        }
    }

    private String executeEmailTask(Task task) {
        emailService.sendTaskNotification(task);
        logger.info("hurrayyyyyyyyyyyyyyyyyyyyy222222222");
        return "Email sent successfully";
    }

    private String executeHttpTask(Task task) {
        RestTemplate restTemplate = new RestTemplate();
        String url = task.getActionPayload();
        try {
            String response = restTemplate.getForObject(url, String.class);
            return "HTTP request completed: " + response;
        } catch (Exception e) {
            throw new RuntimeException("HTTP request failed: " + e.getMessage());
        }
    }

    private String executeDataSyncTask(Task task) {
        return "Data synced successfully";
    }
}
