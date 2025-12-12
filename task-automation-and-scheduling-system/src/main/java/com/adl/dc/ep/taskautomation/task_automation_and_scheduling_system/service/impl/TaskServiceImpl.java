package com.adl.dc.ep.taskautomation.task_automation_and_scheduling_system.service.impl;

import com.adl.dc.ep.taskautomation.task_automation_and_scheduling_system.Job.TaskExecutionJob;
import com.adl.dc.ep.taskautomation.task_automation_and_scheduling_system.dto.TaskRequest;
import com.adl.dc.ep.taskautomation.task_automation_and_scheduling_system.dto.TaskResponse;
import com.adl.dc.ep.taskautomation.task_automation_and_scheduling_system.enums.TaskStatus;
import com.adl.dc.ep.taskautomation.task_automation_and_scheduling_system.exception.ResourceNotFoundException;
import com.adl.dc.ep.taskautomation.task_automation_and_scheduling_system.repository.TaskRepository;
import com.adl.dc.ep.taskautomation.task_automation_and_scheduling_system.service.TaskService;
import org.quartz.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.adl.dc.ep.taskautomation.task_automation_and_scheduling_system.domain.*;

@Service
public class TaskServiceImpl implements TaskService {

    private static final Logger logger = LoggerFactory.getLogger(TaskServiceImpl.class);
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
        logger.debug("scheduleTask");
        JobDetail jobDetail = buildJobDetail(task);
        Trigger trigger = buildJobTrigger(jobDetail, task);
        scheduler.scheduleJob(jobDetail, trigger);
    }


//    private JobDetail buildJobDetail(Task task) {
//        JobDataMap dataMap = new JobDataMap();
// //       dataMap.put("taskId", task.getId());
//
////        return JobBuilder.newJob(TaskExecutionJob.class)
////                .withIdentity(task.getId().toString(), "user-tasks")
////                .withDescription(task.getDescription())
////                .setJobData(dataMap)
////                .storeDurably()
////                .build();
//
//        dataMap.put("taskId", String.valueOf(task.getId()));
//        dataMap.put("taskType", task.getTaskType().name());
//        dataMap.put("actionPayload", task.getActionPayload() != null ? task.getActionPayload() : "");
//        dataMap.put("userId", String.valueOf(task.getUserId()));
//
//        return JobBuilder.newJob(TaskExecutionJob.class)
//                .withIdentity("task-" + task.getId(), "user-tasks")
//                .withDescription(task.getDescription())
//                .usingJobData(dataMap)
//                .storeDurably()
//                .build();
//    }

//    private Trigger buildJobTrigger(JobDetail jobDetail, Task task) {
//        logger.info("buildJobTrigger");
//        return TriggerBuilder.newTrigger()
//                .forJob(jobDetail)
//                .withIdentity(task.getId().toString(), "user-triggers")
//                .withDescription(task.getDescription())
//                .withSchedule(CronScheduleBuilder.cronSchedule(task.getCronExpression()))
//                .build();
//    }
//private JobDetail buildJobDetail(Task task) {
//    JobDataMap dataMap = new JobDataMap();
//    dataMap.put("taskId", String.valueOf(task.getId()));
//    dataMap.put("taskType", task.getTaskType().name());
//    dataMap.put("actionPayload", task.getActionPayload() != null ? task.getActionPayload() : "");
//    dataMap.put("userId", String.valueOf(task.getUserId()));
//
//    return JobBuilder.newJob(TaskExecutionJob.class)
//            .withIdentity(task.getId().toString(), "user-tasks")
//            .withDescription(task.getDescription())
//            .usingJobData(dataMap)
//            .storeDurably()
//            .build();
//}
//
//
//
//    private Trigger buildJobTrigger(JobDetail jobDetail, Task task) {
//        logger.debug("Building trigger for task id={} with cron='{}'",
//                task.getId(), task.getCronExpression());
//
//        return TriggerBuilder.newTrigger()
//                .forJob(jobDetail)
//                // use simple numeric id as before
//                .withIdentity(task.getId().toString(), "user-triggers")
//                .withDescription(task.getDescription())
//                .withSchedule(CronScheduleBuilder.cronSchedule(task.getCronExpression()))
//                .build();
//    }
//

private JobDetail buildJobDetail(Task task) {
    JobDataMap dataMap = new JobDataMap();
    dataMap.put("taskId", String.valueOf(task.getId()));
    dataMap.put("taskType", task.getTaskType().name());
    dataMap.put("actionPayload", task.getActionPayload() != null ? task.getActionPayload() : "");
    dataMap.put("userId", String.valueOf(task.getUserId()));

    return JobBuilder.newJob(TaskExecutionJob.class)
            .withIdentity(task.getId().toString(), "user-tasks")
            .withDescription(task.getDescription())
            .usingJobData(dataMap)
            .storeDurably()
            .build();
}


    private Trigger buildJobTrigger(JobDetail jobDetail, Task task) {
        logger.debug("Building trigger for task id={} with cron='{}'",
                task.getId(), task.getCronExpression());

        return TriggerBuilder.newTrigger()
                .forJob(jobDetail)
                .withIdentity(task.getId().toString(), "user-triggers")
                .withDescription(task.getDescription())
                .withSchedule(
                        CronScheduleBuilder
                                .cronSchedule(task.getCronExpression())
                                .withMisfireHandlingInstructionDoNothing()
                )
                .build();
    }


    private TaskResponse mapToResponse(Task task) {
        logger.info("mapToResponse executed");
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

    private void rescheduleTask(Task task) throws SchedulerException {
        unscheduleTask(task.getId());
        scheduleTask(task);
    }

    private void unscheduleTask(Long taskId) throws SchedulerException {
        JobKey jobKey = JobKey.jobKey(taskId.toString(), "user-tasks");
        scheduler.deleteJob(jobKey);
    }

    private Task getTaskByIdAndUser(Long taskId) {
        User currentUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found"));

        if (!task.getUserId().equals(currentUser.getId())) {
            throw new RuntimeException("Access denied");
        }

        return task;
    }


    @Transactional
    @Override
    public void deleteTask(Long taskId) {
        Task task = getTaskByIdAndUser(taskId);

        try {
            unscheduleTask(taskId);
        } catch (SchedulerException e) {
            // log
        }

        taskRepository.delete(task);
    }

    @Transactional
    @Override
    public TaskResponse updateTask(Long taskId, TaskRequest request){

    Task task = getTaskByIdAndUser(taskId);
    task.setName(request.getName());
    task.setDescription(request.getDescription());
    task.setCronExpression(request.getCronExpression());
        task.setTaskType(request.getTaskType());
        task.setActionPayload(request.getActionPayload());

        Task updatedTask = taskRepository.save(task);

        try {
            rescheduleTask(updatedTask);
        } catch (SchedulerException e) {
            throw new RuntimeException("Failed to reschedule task: " + e.getMessage(), e);
        }

        return mapToResponse(updatedTask);
    }
}


