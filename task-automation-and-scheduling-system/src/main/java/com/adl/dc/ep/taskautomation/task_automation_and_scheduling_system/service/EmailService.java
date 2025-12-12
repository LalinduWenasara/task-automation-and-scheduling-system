package com.adl.dc.ep.taskautomation.task_automation_and_scheduling_system.service;

import com.adl.dc.ep.taskautomation.task_automation_and_scheduling_system.domain.Task;
import com.adl.dc.ep.taskautomation.task_automation_and_scheduling_system.dto.AuthResponse;
import com.adl.dc.ep.taskautomation.task_automation_and_scheduling_system.dto.LoginRequest;

public interface EmailService {

    void sendTaskNotification2(Task task);

    void sendTaskNotification(Task task);

//    String sendEmail(NotificationEmailRequest emailRequest);
//
//    String sendEmail(Task emailRequest);
}
