package com.adl.dc.ep.taskautomation.task_automation_and_scheduling_system.exception;

public class UserNotFoundException extends RuntimeException {
    public UserNotFoundException(Long userId) {
        super("User not found for id=" + userId);
    }
}
