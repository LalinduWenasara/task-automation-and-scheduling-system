package com.adl.dc.ep.taskautomation.task_automation_and_scheduling_system.service.impl;

import com.adl.dc.ep.taskautomation.task_automation_and_scheduling_system.domain.Task;
import com.adl.dc.ep.taskautomation.task_automation_and_scheduling_system.domain.User;
import com.adl.dc.ep.taskautomation.task_automation_and_scheduling_system.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailServiceImplTest {

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private UserRepository userRepository;

    private EmailServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new EmailServiceImpl(mailSender, userRepository);
        ReflectionTestUtils.setField(service, "fromEmail", "noreply@test.com");
    }

    @Test
    void sendSimpleEmail_sendsMessage() {
        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);

        service.sendSimpleEmail("to@test.com", "Subject", "Body");

        verify(mailSender).send(captor.capture());
        SimpleMailMessage msg = captor.getValue();

        assertEquals("noreply@test.com", msg.getFrom());
        assertArrayEquals(new String[]{"to@test.com"}, msg.getTo());
        assertEquals("Subject", msg.getSubject());
        assertEquals("Body", msg.getText());
    }

    @Test
    void sendSimpleEmail_mailSenderThrows_isSwallowed() {
        doThrow(new RuntimeException("smtp down")).when(mailSender).send(any(SimpleMailMessage.class));

        assertDoesNotThrow(() ->
                service.sendSimpleEmail("to@test.com", "Subject", "Body")
        );

        verify(mailSender).send(any(SimpleMailMessage.class));
    }

    @Test
    void sendTaskNotification_userExists_setsRecipientAndSends() {
        Task task = new Task();
        task.setUserId(10L);
        task.setName("Daily Report");

        User user = new User();
        user.setId(10L);
        user.setEmail("owner@test.com");

        when(userRepository.findById(10L)).thenReturn(Optional.of(user));

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);

        assertDoesNotThrow(() -> service.sendTaskNotification(task));

        verify(userRepository).findById(10L);
        verify(mailSender).send(captor.capture());

        SimpleMailMessage msg = captor.getValue();
        assertEquals("noreply@test.com", msg.getFrom());
        assertArrayEquals(new String[]{"owner@test.com"}, msg.getTo());
        assertEquals("Task Executed: Daily Report", msg.getSubject());
        assertEquals("Your scheduled task 'Daily Report' has been executed successfully.", msg.getText());
    }

    @Test
    void sendTaskNotification_userMissing_stillCallsMailSender_withoutTo() {
        Task task = new Task();
        task.setUserId(999L);
        task.setName("No Owner");

        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);

        assertDoesNotThrow(() -> service.sendTaskNotification(task));

        verify(userRepository).findById(999L);
        verify(mailSender).send(captor.capture());

        SimpleMailMessage msg = captor.getValue();
        assertEquals("noreply@test.com", msg.getFrom());
        assertNull(msg.getTo());
        assertEquals("Task Executed: No Owner", msg.getSubject());
        assertEquals("Your scheduled task 'No Owner' has been executed successfully.", msg.getText());
    }

    @Test
    void sendTaskNotification_mailSenderThrows_isSwallowed() {
        Task task = new Task();
        task.setUserId(11L);
        task.setName("Fail Mail");

        User user = new User();
        user.setId(11L);
        user.setEmail("fail@test.com");

        when(userRepository.findById(11L)).thenReturn(Optional.of(user));
        doThrow(new RuntimeException("smtp error")).when(mailSender).send(any(SimpleMailMessage.class));

        assertDoesNotThrow(() -> service.sendTaskNotification(task));

        verify(userRepository).findById(11L);
        verify(mailSender).send(any(SimpleMailMessage.class));
    }

    @Test
    void sendTaskNotification2_doesNotHitDependencies() {
        Task task = new Task();
        task.setUserId(1L);
        task.setName("Anything");

        assertDoesNotThrow(() -> service.sendTaskNotification2(task));

        verifyNoInteractions(userRepository, mailSender);
    }
}
