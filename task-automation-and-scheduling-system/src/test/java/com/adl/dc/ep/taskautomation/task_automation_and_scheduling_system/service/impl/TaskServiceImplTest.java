package com.adl.dc.ep.taskautomation.task_automation_and_scheduling_system.service.impl;

import com.adl.dc.ep.taskautomation.task_automation_and_scheduling_system.Job.TaskExecutionJob;
import com.adl.dc.ep.taskautomation.task_automation_and_scheduling_system.domain.Task;
import com.adl.dc.ep.taskautomation.task_automation_and_scheduling_system.domain.User;
import com.adl.dc.ep.taskautomation.task_automation_and_scheduling_system.dto.TaskRequest;
import com.adl.dc.ep.taskautomation.task_automation_and_scheduling_system.dto.TaskResponse;
import com.adl.dc.ep.taskautomation.task_automation_and_scheduling_system.enums.TaskStatus;
import com.adl.dc.ep.taskautomation.task_automation_and_scheduling_system.enums.TaskType;
import com.adl.dc.ep.taskautomation.task_automation_and_scheduling_system.exception.ResourceNotFoundException;
import com.adl.dc.ep.taskautomation.task_automation_and_scheduling_system.repository.TaskRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.quartz.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaskServiceImplTest {

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private Scheduler scheduler;

    @InjectMocks
    private TaskServiceImpl taskService;


    private static MockedStatic<SecurityContextHolder> mockLoggedUser(Long userId) {
        User user = new User();
        user.setId(userId);

        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(user);

        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);

        MockedStatic<SecurityContextHolder> mocked = mockStatic(SecurityContextHolder.class);
        mocked.when(SecurityContextHolder::getContext).thenReturn(securityContext);
        return mocked;
    }

    private static TaskRequest buildRequest(String name, String desc, String cron, TaskType type, String payload) {
        TaskRequest r = new TaskRequest();
        r.setName(name);
        r.setDescription(desc);
        r.setCronExpression(cron);
        r.setTaskType(type);
        r.setActionPayload(payload);
        return r;
    }

    private static Task task(Long id, Long userId) {
        Task t = new Task();
        t.setId(id);
        t.setUserId(userId);
        t.setName("N" + id);
        t.setDescription("D" + id);
        t.setCronExpression("0 0/5 * * * ?");
        t.setTaskType(TaskType.HTTP_REQUEST);
        t.setActionPayload("P" + id);
        t.setStatus(TaskStatus.ACTIVE);
        t.setCreatedAt(LocalDateTime.now());
        t.setLastExecutedAt(LocalDateTime.now());
        return t;
    }


    @Test
    void createTask_shouldPersistAndSchedule() throws Exception {
        try (MockedStatic<SecurityContextHolder> ignored = mockLoggedUser(42L)) {

            TaskRequest request = buildRequest(
                    "Test Task", "Sample description", "0 0/5 * * * ?",
                    TaskType.HTTP_REQUEST, "{\"url\":\"https://example.com\"}"
            );

            when(taskRepository.save(any(Task.class))).thenAnswer(inv -> {
                Task t = inv.getArgument(0);
                t.setId(1L);
                return t;
            });

            ArgumentCaptor<JobDetail> jobDetailCaptor = ArgumentCaptor.forClass(JobDetail.class);
            ArgumentCaptor<Trigger> triggerCaptor = ArgumentCaptor.forClass(Trigger.class);

            TaskResponse response = taskService.createTask(request);

            assertNotNull(response);
            assertEquals(1L, response.getId());
            assertEquals(TaskStatus.ACTIVE, response.getStatus());

            verify(taskRepository).save(any(Task.class));
            verify(scheduler).scheduleJob(jobDetailCaptor.capture(), triggerCaptor.capture());

            JobDetail jobDetail = jobDetailCaptor.getValue();
            assertEquals("1", jobDetail.getKey().getName());
            assertEquals("user-tasks", jobDetail.getKey().getGroup());
            assertEquals(TaskExecutionJob.class, jobDetail.getJobClass());

            // cover jobDataMap lines
            JobDataMap map = jobDetail.getJobDataMap();
            assertEquals("1", map.getString("taskId"));
            assertEquals(TaskType.HTTP_REQUEST.name(), map.getString("taskType"));
            assertEquals("{\"url\":\"https://example.com\"}", map.getString("actionPayload"));
            assertEquals("42", map.getString("userId"));

            // cover trigger builder + misfire
            Trigger trigger = triggerCaptor.getValue();
            assertEquals("1", trigger.getKey().getName());
            assertEquals("user-triggers", trigger.getKey().getGroup());
            assertTrue(trigger instanceof CronTrigger);
            CronTrigger cronTrigger = (CronTrigger) trigger;
            assertEquals("0 0/5 * * * ?", cronTrigger.getCronExpression());
            assertEquals(CronTrigger.MISFIRE_INSTRUCTION_DO_NOTHING, cronTrigger.getMisfireInstruction());
        }
    }

    @Test
    void createTask_whenActionPayloadNull_shouldStoreEmptyStringInJobDataMap() throws Exception {
        try (MockedStatic<SecurityContextHolder> ignored = mockLoggedUser(7L)) {

            TaskRequest request = buildRequest(
                    "T", "D", "0 0/1 * * * ?", TaskType.HTTP_REQUEST, null
            );

            when(taskRepository.save(any(Task.class))).thenAnswer(inv -> {
                Task t = inv.getArgument(0);
                t.setId(9L);
                return t;
            });

            ArgumentCaptor<JobDetail> jobDetailCaptor = ArgumentCaptor.forClass(JobDetail.class);

            taskService.createTask(request);

            verify(scheduler).scheduleJob(jobDetailCaptor.capture(), any(Trigger.class));

            JobDataMap map = jobDetailCaptor.getValue().getJobDataMap();
            assertEquals("", map.getString("actionPayload")); // covers ternary line
        }
    }

    @Test
    void createTask_whenSchedulerFails_shouldThrowRuntimeException() throws Exception {
        try (MockedStatic<SecurityContextHolder> ignored = mockLoggedUser(100L)) {

            TaskRequest request = buildRequest(
                    "Broken", "Fail scheduling", "0 0/1 * * * ?", TaskType.HTTP_REQUEST, null
            );

            when(taskRepository.save(any(Task.class))).thenAnswer(inv -> {
                Task t = inv.getArgument(0);
                t.setId(99L);
                return t;
            });

            doThrow(new SchedulerException("scheduler down"))
                    .when(scheduler).scheduleJob(any(JobDetail.class), any(Trigger.class));

            RuntimeException ex = assertThrows(RuntimeException.class, () -> taskService.createTask(request));
            assertTrue(ex.getMessage().contains("Failed to schedule task"));
            assertTrue(ex.getCause() instanceof SchedulerException);

            verify(taskRepository).save(any(Task.class));
            verify(scheduler).scheduleJob(any(JobDetail.class), any(Trigger.class));
        }
    }

    @Test
    void updateTask_shouldSaveAndReschedule() throws Exception {
        try (MockedStatic<SecurityContextHolder> ignored = mockLoggedUser(42L)) {

            Task existing = task(5L, 42L);
            when(taskRepository.findById(5L)).thenReturn(Optional.of(existing));
            when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

            TaskRequest request = buildRequest(
                    "NewName", "NewDesc", "0/30 * * * * ?", TaskType.HTTP_REQUEST, "NEW_PAYLOAD"
            );

            TaskResponse response = taskService.updateTask(5L, request);

            assertNotNull(response);
            assertEquals(5L, response.getId());
            assertEquals("NewName", response.getName());
            assertEquals("0/30 * * * * ?", response.getCronExpression());

            // reschedule: deleteJob + scheduleJob
            verify(scheduler).deleteJob(JobKey.jobKey("5", "user-tasks"));
            verify(scheduler).scheduleJob(any(JobDetail.class), any(Trigger.class));
            verify(taskRepository).save(any(Task.class));
        }
    }

    @Test
    void updateTask_whenRescheduleFails_shouldThrowRuntimeException() throws Exception {
        try (MockedStatic<SecurityContextHolder> ignored = mockLoggedUser(42L)) {

            Task existing = task(6L, 42L);
            when(taskRepository.findById(6L)).thenReturn(Optional.of(existing));
            when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

            doThrow(new SchedulerException("delete failed"))
                    .when(scheduler).deleteJob(JobKey.jobKey("6", "user-tasks"));

            TaskRequest request = buildRequest(
                    "X", "Y", "0 0/2 * * * ?", TaskType.HTTP_REQUEST, "P"
            );

            RuntimeException ex = assertThrows(RuntimeException.class,
                    () -> taskService.updateTask(6L, request));

            assertTrue(ex.getMessage().contains("Failed to reschedule task"));
            assertTrue(ex.getCause() instanceof SchedulerException);
        }
    }

    @Test
    void deleteTask_shouldUnscheduleAndDeleteFromRepo() throws Exception {
        try (MockedStatic<SecurityContextHolder> ignored = mockLoggedUser(42L)) {

            Task existing = task(7L, 42L);
            when(taskRepository.findById(7L)).thenReturn(Optional.of(existing));

            taskService.deleteTask(7L);

            verify(scheduler).deleteJob(JobKey.jobKey("7", "user-tasks"));
            verify(taskRepository).delete(existing);
        }
    }

    @Test
    void deleteTask_whenUnscheduleFails_shouldStillDeleteFromRepo() throws Exception {
        try (MockedStatic<SecurityContextHolder> ignored = mockLoggedUser(42L)) {

            Task existing = task(8L, 42L);
            when(taskRepository.findById(8L)).thenReturn(Optional.of(existing));

            doThrow(new SchedulerException("cannot delete job"))
                    .when(scheduler).deleteJob(JobKey.jobKey("8", "user-tasks"));

            taskService.deleteTask(8L);

            // it should swallow scheduler exception (your code has // log)
            verify(taskRepository).delete(existing);
        }
    }

    @Test
    void getTask_shouldReturnTaskResponse() {
        try (MockedStatic<SecurityContextHolder> ignored = mockLoggedUser(42L)) {

            Task existing = task(9L, 42L);
            when(taskRepository.findById(9L)).thenReturn(Optional.of(existing));

            TaskResponse res = taskService.getTask(9L);

            assertNotNull(res);
            assertEquals(9L, res.getId());
            assertEquals(existing.getName(), res.getName());
        }
    }

    @Test
    void getAllUserTasks_shouldReturnOnlyMappedTasksForCurrentUser() {
        try (MockedStatic<SecurityContextHolder> ignored = mockLoggedUser(42L)) {

            when(taskRepository.findByUserId(42L)).thenReturn(List.of(
                    task(1L, 42L),
                    task(2L, 42L)
            ));

            List<TaskResponse> list = taskService.getAllUserTasks();

            assertEquals(2, list.size());
            assertEquals(1L, list.get(0).getId());
            assertEquals(2L, list.get(1).getId());

            verify(taskRepository).findByUserId(42L);
        }
    }

    @Test
    void getTask_whenNotFound_shouldThrowResourceNotFoundException() {
        try (MockedStatic<SecurityContextHolder> ignored = mockLoggedUser(42L)) {

            when(taskRepository.findById(123L)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class, () -> taskService.getTask(123L));
        }
    }

    @Test
    void getTask_whenDifferentUser_shouldThrowAccessDeniedRuntimeException() {
        try (MockedStatic<SecurityContextHolder> ignored = mockLoggedUser(42L)) {

            Task otherUsersTask = task(55L, 999L);
            when(taskRepository.findById(55L)).thenReturn(Optional.of(otherUsersTask));

            RuntimeException ex = assertThrows(RuntimeException.class, () -> taskService.getTask(55L));
            assertTrue(ex.getMessage().toLowerCase().contains("access denied"));
        }
    }
}
