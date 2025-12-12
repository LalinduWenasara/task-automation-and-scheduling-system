package com.adl.dc.ep.taskautomation.task_automation_and_scheduling_system.service;

import com.adl.dc.ep.taskautomation.task_automation_and_scheduling_system.dto.TaskRequest;
import com.adl.dc.ep.taskautomation.task_automation_and_scheduling_system.dto.TaskResponse;
import org.springframework.transaction.annotation.Transactional;

public interface TaskService {
    @Transactional
    TaskResponse createTask(TaskRequest request);

    @Transactional
    void deleteTask(Long taskId);

    @Transactional
    TaskResponse updateTask(Long taskId, TaskRequest request);
}
