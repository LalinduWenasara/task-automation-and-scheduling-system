package com.adl.dc.ep.taskautomation.task_automation_and_scheduling_system.service.impl;

//package com.adl.dc.ep.taskautomation.task_automation_and_scheduling_system.service.impl;

import com.adl.dc.ep.taskautomation.task_automation_and_scheduling_system.Job.TaskExecutionJob;
import com.adl.dc.ep.taskautomation.task_automation_and_scheduling_system.domain.Task;
import com.adl.dc.ep.taskautomation.task_automation_and_scheduling_system.domain.User;
import com.adl.dc.ep.taskautomation.task_automation_and_scheduling_system.dto.TaskRequest;
import com.adl.dc.ep.taskautomation.task_automation_and_scheduling_system.dto.TaskResponse;
import com.adl.dc.ep.taskautomation.task_automation_and_scheduling_system.enums.TaskStatus;
import com.adl.dc.ep.taskautomation.task_automation_and_scheduling_system.enums.TaskType; // <-- adjust if package differs
import com.adl.dc.ep.taskautomation.task_automation_and_scheduling_system.repository.TaskRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.quartz.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.mockStatic;

@ExtendWith(MockitoExtension.class)
class TaskServiceImplTest {

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private Scheduler scheduler;

    @InjectMocks
    private TaskServiceImpl taskService;

    @Test
    void createTask_shouldPersistTaskAndScheduleJob() throws Exception {
        // -------- Arrange --------
        TaskRequest request = new TaskRequest();
        request.setName("Test Task");
        request.setDescription("Sample description");
        request.setCronExpression("0 0/5 * * * ?"); // every 5 minutes
        // TODO: replace TaskType.HTTP with an actual constant from your TaskType enum
        request.setTaskType(TaskType.HTTP_REQUEST);
        request.setActionPayload("{\"url\":\"https://example.com\"}");

        // mock currently logged-in user
        User user = new User();
        user.setId(42L);

        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(user);

        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);

        try (MockedStatic<SecurityContextHolder> securityContextHolderMock =
                     mockStatic(SecurityContextHolder.class)) {

            securityContextHolderMock.when(SecurityContextHolder::getContext)
                    .thenReturn(securityContext);

            // mock repository save to set an ID
            when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> {
                Task t = invocation.getArgument(0);
                t.setId(1L);
                return t;
            });

            ArgumentCaptor<JobDetail> jobDetailCaptor = ArgumentCaptor.forClass(JobDetail.class);
            ArgumentCaptor<Trigger> triggerCaptor = ArgumentCaptor.forClass(Trigger.class);

            // -------- Act --------
            TaskResponse response = taskService.createTask(request);

            // -------- Assert --------
            assertNotNull(response);
            assertEquals(1L, response.getId());
            assertEquals("Test Task", response.getName());
            assertEquals("Sample description", response.getDescription());
            assertEquals("0 0/5 * * * ?", response.getCronExpression());
            assertEquals(TaskStatus.ACTIVE, response.getStatus());
            assertEquals(TaskType.HTTP_REQUEST, response.getTaskType());

            // verify repo save
            verify(taskRepository, times(1)).save(any(Task.class));

            // verify scheduler call and inspect JobDetail & Trigger
            verify(scheduler, times(1)).scheduleJob(jobDetailCaptor.capture(), triggerCaptor.capture());

            JobDetail jobDetail = jobDetailCaptor.getValue();
            assertNotNull(jobDetail);
            assertEquals("1", jobDetail.getKey().getName());
            assertEquals("user-tasks", jobDetail.getKey().getGroup());
            assertEquals(TaskExecutionJob.class, jobDetail.getJobClass());

            JobDataMap jobDataMap = jobDetail.getJobDataMap();
            assertEquals("1", jobDataMap.getString("taskId"));
            assertEquals(TaskType.HTTP_REQUEST.name(), jobDataMap.getString("taskType"));
            assertEquals("{\"url\":\"https://example.com\"}", jobDataMap.getString("actionPayload"));
            assertEquals(String.valueOf(user.getId()), jobDataMap.getString("userId"));

            Trigger trigger = triggerCaptor.getValue();
            assertNotNull(trigger);
            assertEquals("1", trigger.getKey().getName());
            assertEquals("user-triggers", trigger.getKey().getGroup());
            assertEquals("Sample description", trigger.getDescription());

            assertTrue(trigger instanceof CronTrigger);
            CronTrigger cronTrigger = (CronTrigger) trigger;
            assertEquals("0 0/5 * * * ?", cronTrigger.getCronExpression());
            // misfire instruction should be "do nothing"
            assertEquals(CronTrigger.MISFIRE_INSTRUCTION_DO_NOTHING, cronTrigger.getMisfireInstruction());
        }
    }

    @Test
    void createTask_whenSchedulerFails_shouldThrowRuntimeException() throws Exception {
        // -------- Arrange --------
        TaskRequest request = new TaskRequest();
        request.setName("Broken Task");
        request.setDescription("This will fail scheduling");
        request.setCronExpression("0 0/1 * * * ?");
        // TODO: replace TaskType.HTTP with an actual constant from your TaskType enum
        request.setTaskType(TaskType.HTTP_REQUEST);
        request.setActionPayload(null);

        User user = new User();
        user.setId(100L);

        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(user);

        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);

        try (MockedStatic<SecurityContextHolder> securityContextHolderMock =
                     mockStatic(SecurityContextHolder.class)) {

            securityContextHolderMock.when(SecurityContextHolder::getContext)
                    .thenReturn(securityContext);

            when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> {
                Task t = invocation.getArgument(0);
                t.setId(99L);
                return t;
            });

            doThrow(new SchedulerException("scheduler down"))
                    .when(scheduler).scheduleJob(any(JobDetail.class), any(Trigger.class));

            // -------- Act + Assert --------
            RuntimeException ex = assertThrows(RuntimeException.class,
                    () -> taskService.createTask(request));

            assertTrue(ex.getMessage().contains("Failed to schedule task"));
            assertTrue(ex.getCause() instanceof SchedulerException);

            verify(taskRepository, times(1)).save(any(Task.class));
            verify(scheduler, times(1)).scheduleJob(any(JobDetail.class), any(Trigger.class));
        }
    }
}
