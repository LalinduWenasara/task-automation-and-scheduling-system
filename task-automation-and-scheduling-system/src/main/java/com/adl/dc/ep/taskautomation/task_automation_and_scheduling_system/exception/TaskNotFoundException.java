package com.adl.dc.ep.taskautomation.task_automation_and_scheduling_system.exception;

public class TaskNotFoundException extends RuntimeException {
    public TaskNotFoundException(Long taskId) {
        super("Task not found for id=" + taskId);
    }
}
