package com.adl.dc.ep.taskautomation.task_automation_and_scheduling_system.service.impl;

import com.adl.dc.ep.taskautomation.task_automation_and_scheduling_system.Job.TaskExecutionJob;
import com.adl.dc.ep.taskautomation.task_automation_and_scheduling_system.dto.TaskRequest;
import com.adl.dc.ep.taskautomation.task_automation_and_scheduling_system.dto.TaskResponse;
import com.adl.dc.ep.taskautomation.task_automation_and_scheduling_system.enums.TaskStatus;
import com.adl.dc.ep.taskautomation.task_automation_and_scheduling_system.repository.TaskRepository;
import com.adl.dc.ep.taskautomation.task_automation_and_scheduling_system.service.TaskService;
import org.quartz.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.adl.dc.ep.taskautomation.task_automation_and_scheduling_system.domain.*;

@Service
public class TaskServiceImpl implements TaskService {

    private final TaskRepository taskRepository;
    private final Scheduler scheduler;


    public TaskServiceImpl(TaskRepository taskRepository, Scheduler scheduler) {
        this.taskRepository = taskRepository;
        this.scheduler = scheduler;
    }


    @Transactional
    @Override
    public TaskResponse createTask(TaskRequest request){
        User currentLoggedUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        Task task = new Task();
        task.setName(request.getName());
        task.setDescription(request.getDescription());
        task.setCronExpression(request.getCronExpression());
        task.setTaskType(request.getTaskType());
        task.setActionPayload(request.getActionPayload());
        task.setUserId(currentLoggedUser.getId());
        task.setStatus(TaskStatus.ACTIVE);

        Task savedTask = taskRepository.save(task);

        try {
            scheduleTask(savedTask);
        } catch (SchedulerException e) {
            throw new RuntimeException("Failed to schedule task: " + e.getMessage(), e);
        }

        return mapToResponse(savedTask);
     //   return null;
    }

    private void scheduleTask(Task task) throws SchedulerException {
        JobDetail jobDetail = buildJobDetail(task);
        Trigger trigger = buildJobTrigger(jobDetail, task);
        scheduler.scheduleJob(jobDetail, trigger);
    }


    private JobDetail buildJobDetail(Task task) {
        JobDataMap dataMap = new JobDataMap();
        dataMap.put("taskId", task.getId());

        return JobBuilder.newJob(TaskExecutionJob.class)
                .withIdentity(task.getId().toString(), "user-tasks")
                .withDescription(task.getDescription())
                .setJobData(dataMap)
                .storeDurably()
                .build();
    }

    private Trigger buildJobTrigger(JobDetail jobDetail, Task task) {
        return TriggerBuilder.newTrigger()
                .forJob(jobDetail)
                .withIdentity(task.getId().toString(), "user-triggers")
                .withDescription(task.getDescription())
                .withSchedule(CronScheduleBuilder.cronSchedule(task.getCronExpression()))
                .build();
    }

    private TaskResponse mapToResponse(Task task) {
        TaskResponse response = new TaskResponse();
        response.setId(task.getId());
        response.setName(task.getName());
        response.setDescription(task.getDescription());
        response.setCronExpression(task.getCronExpression());
        response.setTaskType(task.getTaskType());
        response.setStatus(task.getStatus());
        response.setCreatedAt(task.getCreatedAt());
        response.setLastExecutedAt(task.getLastExecutedAt());
        return response;
    }
}


