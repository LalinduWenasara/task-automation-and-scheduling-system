package com.adl.dc.ep.taskautomation.task_automation_and_scheduling_system.service;

import com.adl.dc.ep.taskautomation.task_automation_and_scheduling_system.domain.Task;

public interface EmailService {

    void sendTaskNotification2(Task task);

    void sendTaskNotification(Task task);

    void sendSimpleEmail(String to, String subject, String body);


}
