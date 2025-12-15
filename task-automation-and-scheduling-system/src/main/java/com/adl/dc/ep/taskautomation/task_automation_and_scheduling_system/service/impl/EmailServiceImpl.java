package com.adl.dc.ep.taskautomation.task_automation_and_scheduling_system.service.impl;

import com.adl.dc.ep.taskautomation.task_automation_and_scheduling_system.domain.Task;
import com.adl.dc.ep.taskautomation.task_automation_and_scheduling_system.domain.User;
import com.adl.dc.ep.taskautomation.task_automation_and_scheduling_system.repository.UserRepository;
import com.adl.dc.ep.taskautomation.task_automation_and_scheduling_system.service.EmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class EmailServiceImpl implements EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailServiceImpl.class);
    private final JavaMailSender mailSender;
    private final UserRepository userRepository;
    @Value("${spring.mail.username}")
    private String fromEmail;

    public EmailServiceImpl(JavaMailSender mailSender, UserRepository userRepository) {
        this.mailSender = mailSender;
        this.userRepository = userRepository;
    }

    @Override
    public void sendTaskNotification2(Task task) {
        logger.info("half won");
    }


    @Async
    public void sendTaskNotification(Task task) {
        logger.info("sendTaskNotification start");

        Optional<User> user = userRepository.findById(task.getUserId());
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            user.ifPresent(value -> message.setTo(value.getEmail()));
            message.setSubject("Task Executed: " + task.getName());
            message.setText("Your scheduled task '" + task.getName() + "' has been executed successfully.");
            logger.info("sendTaskNotification mid");
            mailSender.send(message);
        } catch (Exception e) {
            logger.info("sendTaskNotification something went wrong");
            logger.info(e.getMessage());
        }
    }


    @Override
    public void sendSimpleEmail(String to, String subject, String body) {
        logger.info("sendSimpleEmail to={}", to);
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
        } catch (Exception e) {
            logger.warn("Failed to send email to {}: {}", to, e.getMessage());
        }
    }
}


