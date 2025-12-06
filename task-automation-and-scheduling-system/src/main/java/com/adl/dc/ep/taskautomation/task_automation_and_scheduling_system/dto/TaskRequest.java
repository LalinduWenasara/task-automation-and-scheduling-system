package com.adl.dc.ep.taskautomation.task_automation_and_scheduling_system.dto;

import com.adl.dc.ep.taskautomation.task_automation_and_scheduling_system.enums.TaskType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class TaskRequest {
    @NotBlank(message = "Task name is required")
    private String name;

    private String description;

    @NotBlank(message = "Cron expression is required")
    private String cronExpression;

    @NotNull(message = "Task type is required")
    private TaskType taskType;

    private String actionPayload;

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

    public String getActionPayload() {
        return actionPayload;
    }

    public void setActionPayload(String actionPayload) {
        this.actionPayload = actionPayload;
    }
}
