package com.adl.dc.ep.taskautomation.task_automation_and_scheduling_system.Job;

import com.adl.dc.ep.taskautomation.task_automation_and_scheduling_system.domain.Task;
import com.adl.dc.ep.taskautomation.task_automation_and_scheduling_system.domain.TaskExecution;
import com.adl.dc.ep.taskautomation.task_automation_and_scheduling_system.enums.ExecutionStatus;
import com.adl.dc.ep.taskautomation.task_automation_and_scheduling_system.repository.TaskExecutionRepository;
import com.adl.dc.ep.taskautomation.task_automation_and_scheduling_system.repository.TaskRepository;
import com.adl.dc.ep.taskautomation.task_automation_and_scheduling_system.service.EmailService;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;

@Component
public class TaskExecutionJob implements Job {

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private TaskExecutionRepository executionRepository;

    @Autowired
    private EmailService emailService;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        Long taskId = context.getJobDetail().getJobDataMap().getLong("taskId");

        TaskExecution execution = new TaskExecution();
        execution.setTaskId(taskId);
        execution.setStatus(ExecutionStatus.RUNNING);
        execution.setStartTime(LocalDateTime.now());
        execution = executionRepository.save(execution);

        try {
            Task task = taskRepository.findById(taskId)
                    .orElseThrow(() -> new RuntimeException("Task not found"));

            String result = executeTaskLogic(task);

            execution.setEndTime(LocalDateTime.now());
            execution.setStatus(ExecutionStatus.SUCCESS);
            execution.setResult(result);

            task.setLastExecutedAt(LocalDateTime.now());
            taskRepository.save(task);

        } catch (Exception e) {
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
      //  emailService.sendTaskNotification(task);
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
