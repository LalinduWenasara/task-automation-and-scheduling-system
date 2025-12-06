package com.adl.dc.ep.taskautomation.task_automation_and_scheduling_system.dto;

import com.adl.dc.ep.taskautomation.task_automation_and_scheduling_system.enums.TaskStatus;
import com.adl.dc.ep.taskautomation.task_automation_and_scheduling_system.enums.TaskType;

import java.time.LocalDateTime;

public class TaskResponse {
    private Long id;
    private String name;
    private String description;
    private String cronExpression;
    private TaskType taskType;
    private TaskStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime lastExecutedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCronExpression() {
        return cronExpression;
    }

    public void setCronExpression(String cronExpression) {
        this.cronExpression = cronExpression;
    }

    public TaskType getTaskType() {
        return taskType;
    }

    public void setTaskType(TaskType taskType) {
        this.taskType = taskType;
    }

    public TaskStatus getStatus() {
        return status;
    }

    public void setStatus(TaskStatus status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getLastExecutedAt() {
        return lastExecutedAt;
    }

    public void setLastExecutedAt(LocalDateTime lastExecutedAt) {
        this.lastExecutedAt = lastExecutedAt;
    }
}
