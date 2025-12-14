package com.adl.dc.ep.taskautomation.task_automation_and_scheduling_system.service.impl;

import com.adl.dc.ep.taskautomation.task_automation_and_scheduling_system.domain.Task;
import com.adl.dc.ep.taskautomation.task_automation_and_scheduling_system.service.EmailService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class EmailServiceImpl implements EmailService {

    private static final Logger logger = LoggerFactory.getLogger(TaskServiceImpl.class);
    private final JavaMailSender mailSender;
    @Value("${spring.mail.username}")
    private String fromEmail;

    public EmailServiceImpl(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Override
    public void sendTaskNotification2(Task task) {
        logger.info("half won");
    }


    @Async
    public void sendTaskNotification(Task task) {
        logger.info("sendTaskNotification start");
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo("pehansaubeysekara@gmail.com");
            message.setSubject("Task Executed: " + task.getName());
            message.setText("Your scheduled task '" + task.getName() + "' has been executed successfully.");
            logger.info("sendTaskNotification mid");
            mailSender.send(message);
        } catch (Exception e) {
            logger.info("sendTaskNotification something went wrong");
            logger.info(e.getMessage());
        }
    }


//    @Override
//    public String sendEmail(Task emailRequest) {
//        try {
//            MimeMessage message = mailSender.createMimeMessage();
//            MimeMessageHelper helper = new MimeMessageHelper(message, true);
//            helper.setSubject(emailRequest.getSubject());
//            helper.setFrom("noreply.salesanddistribution@gmail.com");
//            helper.setTo(emailRequest.getEmail());
//            helper.setText(emailRequest.getBody(), true);
//            mailSender.send(message);
//        } catch (MessagingException e) {
//            LOGGER.warn("Exception occurred while sending email to the user {}", emailRequest.getEmail());
//            throw new RuntimeException(e);
//        }
//        return emailRequest.getEmail();
//    }
}
