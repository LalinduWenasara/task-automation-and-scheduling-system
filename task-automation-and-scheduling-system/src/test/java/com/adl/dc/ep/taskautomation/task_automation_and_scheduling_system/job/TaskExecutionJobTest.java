package com.adl.dc.ep.taskautomation.task_automation_and_scheduling_system.job;

import com.adl.dc.ep.taskautomation.task_automation_and_scheduling_system.domain.Task;
import com.adl.dc.ep.taskautomation.task_automation_and_scheduling_system.domain.TaskExecution;
import com.adl.dc.ep.taskautomation.task_automation_and_scheduling_system.domain.User;
import com.adl.dc.ep.taskautomation.task_automation_and_scheduling_system.dto.OpenWeatherResponseDto;
import com.adl.dc.ep.taskautomation.task_automation_and_scheduling_system.enums.ExecutionStatus;
import com.adl.dc.ep.taskautomation.task_automation_and_scheduling_system.enums.TaskType;
import com.adl.dc.ep.taskautomation.task_automation_and_scheduling_system.repository.TaskExecutionRepository;
import com.adl.dc.ep.taskautomation.task_automation_and_scheduling_system.repository.TaskRepository;
import com.adl.dc.ep.taskautomation.task_automation_and_scheduling_system.repository.UserRepository;
import com.adl.dc.ep.taskautomation.task_automation_and_scheduling_system.service.EmailService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.MockedConstruction;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaskExecutionJobTest {

    @Mock private TaskRepository taskRepository;
    @Mock private TaskExecutionRepository executionRepository;
    @Mock private UserRepository userRepository;
    @Mock private EmailService emailService;
    @Mock private JobExecutionContext context;

    @Captor private ArgumentCaptor<TaskExecution> executionCaptor;
    @Captor private ArgumentCaptor<Task> taskCaptor;

    private TaskExecutionJob newJob() {
        TaskExecutionJob job = new TaskExecutionJob(taskRepository, executionRepository, userRepository, emailService);

        // Inject @Value fields for tests
        ReflectionTestUtils.setField(job, "weatherApiKey", "test-api-key");
        ReflectionTestUtils.setField(job, "weatherBaseUrl", "https://api.openweathermap.org/data/2.5/weather");
        ReflectionTestUtils.setField(job, "weatherUnits", "metric");

        return job;
    }

    private static Task task(long id, TaskType type, String payload) {
        Task t = new Task();
        t.setId(id);
        t.setTaskType(type);
        t.setActionPayload(payload);
        return t;
    }

    @Test
    void execute_whenTaskIdMissing_shouldThrowJobExecutionException() {
        JobDataMap map = new JobDataMap();
        when(context.getMergedJobDataMap()).thenReturn(map);

        JobExecutionException ex = assertThrows(JobExecutionException.class, () -> newJob().execute(context));
        assertTrue(ex.getMessage().contains("taskId is missing"));

        verifyNoInteractions(executionRepository, taskRepository, userRepository, emailService);
    }

    @Test
    void execute_whenTaskIdInvalid_shouldThrowJobExecutionException() {
        JobDataMap map = new JobDataMap();
        map.put("taskId", "abc");
        when(context.getMergedJobDataMap()).thenReturn(map);

        JobExecutionException ex = assertThrows(JobExecutionException.class, () -> newJob().execute(context));
        assertTrue(ex.getMessage().contains("Invalid taskId"));

        verifyNoInteractions(executionRepository, taskRepository, userRepository, emailService);
    }

    @Test
    void execute_whenEmailTask_shouldSendEmail_markSuccess_updateTaskAndExecution() throws Exception {
        long taskId = 10L;
        JobDataMap map = new JobDataMap();
        map.put("taskId", String.valueOf(taskId));
        when(context.getMergedJobDataMap()).thenReturn(map);
        when(executionRepository.save(any(TaskExecution.class))).thenAnswer(inv -> inv.getArgument(0));
        Task t = task(taskId, TaskType.EMAIL, "ignored");
        when(taskRepository.findById(taskId)).thenReturn(Optional.of(t));
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));
        newJob().execute(context);
        verify(emailService, times(1)).sendTaskNotification(any(Task.class));
        verify(taskRepository).save(taskCaptor.capture());
        assertNotNull(taskCaptor.getValue().getLastExecutedAt());
        verify(executionRepository, times(2)).save(executionCaptor.capture());
        TaskExecution finalSave = executionCaptor.getAllValues().get(1);
        assertEquals(taskId, finalSave.getTaskId());
        assertEquals(ExecutionStatus.SUCCESS, finalSave.getStatus());
        assertNotNull(finalSave.getEndTime());
        assertEquals("Email sent successfully", finalSave.getResult());
        assertNull(finalSave.getErrorMessage());
    }

    @Test
    void execute_whenDataSyncTask_shouldMarkSuccess_withDataSyncResult() throws Exception {
        long taskId = 20L;
        JobDataMap map = new JobDataMap();
        map.put("taskId", String.valueOf(taskId));
        when(context.getMergedJobDataMap()).thenReturn(map);

        when(executionRepository.save(any(TaskExecution.class))).thenAnswer(inv -> inv.getArgument(0));

        Task t = task(taskId, TaskType.DATA_SYNC, null);
        when(taskRepository.findById(taskId)).thenReturn(Optional.of(t));
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

        newJob().execute(context);

        verifyNoInteractions(emailService);

        verify(executionRepository, times(2)).save(executionCaptor.capture());
        TaskExecution finalSave = executionCaptor.getAllValues().get(1);

        assertEquals(ExecutionStatus.SUCCESS, finalSave.getStatus());
        assertEquals(taskId + "Data synced successfully", finalSave.getResult());

        verify(taskRepository).save(any(Task.class));
    }

    @Test
    void execute_whenHttpRequestTask_withNullUrl_shouldMarkFailed() throws Exception {
        long taskId = 30L;
        JobDataMap map = new JobDataMap();
        map.put("taskId", String.valueOf(taskId));
        when(context.getMergedJobDataMap()).thenReturn(map);

        when(executionRepository.save(any(TaskExecution.class))).thenAnswer(inv -> inv.getArgument(0));

        Task t = task(taskId, TaskType.HTTP_REQUEST, null);
        when(taskRepository.findById(taskId)).thenReturn(Optional.of(t));

        newJob().execute(context);

        // Should NOT update task lastExecutedAt on failure
        verify(taskRepository, never()).save(any(Task.class));

        verify(executionRepository, times(2)).save(executionCaptor.capture());
        TaskExecution finalSave = executionCaptor.getAllValues().get(1);

        assertEquals(ExecutionStatus.FAILED, finalSave.getStatus());
        assertNotNull(finalSave.getEndTime());
        assertNotNull(finalSave.getErrorMessage());
        assertFalse(finalSave.getErrorMessage().toLowerCase().contains("url is required"));
    }




    @Test
    void execute_whenWeatherTask_locationMissing_shouldMarkFailed() throws Exception {
        long taskId = 50L;
        JobDataMap map = new JobDataMap();
        map.put("taskId", String.valueOf(taskId));
        when(context.getMergedJobDataMap()).thenReturn(map);

        when(executionRepository.save(any(TaskExecution.class))).thenAnswer(inv -> inv.getArgument(0));

        Task t = task(taskId, TaskType.WEATHER, "   "); // blank location
        t.setUserId(99L);
        when(taskRepository.findById(taskId)).thenReturn(Optional.of(t));

        newJob().execute(context);

        verify(executionRepository, times(2)).save(executionCaptor.capture());
        TaskExecution finalSave = executionCaptor.getAllValues().get(1);

        assertEquals(ExecutionStatus.FAILED, finalSave.getStatus());
        assertTrue(finalSave.getErrorMessage().toLowerCase().contains("location is required"));

        verifyNoInteractions(userRepository);
        verify(emailService, never()).sendSimpleEmail(anyString(), anyString(), anyString());
    }

    @Test
    void execute_whenWeatherTask_shouldCallOpenWeather_andSendEmail_andMarkSuccess() throws Exception {
        long taskId = 60L;
        long userId = 7L;
        String location = "Colombo";

        JobDataMap map = new JobDataMap();
        map.put("taskId", String.valueOf(taskId));
        when(context.getMergedJobDataMap()).thenReturn(map);

        when(executionRepository.save(any(TaskExecution.class))).thenAnswer(inv -> inv.getArgument(0));

        Task t = task(taskId, TaskType.WEATHER, location);
        t.setUserId(userId);

        when(taskRepository.findById(taskId)).thenReturn(Optional.of(t));
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

        User u = new User();
        u.setId(userId);
        u.setEmail("user@test.com");
        when(userRepository.findById(userId)).thenReturn(Optional.of(u));

        // Mock OpenWeather DTO
        OpenWeatherResponseDto.Main main = mock(OpenWeatherResponseDto.Main.class);
        when(main.getTemp()).thenReturn(29.5);

        OpenWeatherResponseDto.Weather w0 = mock(OpenWeatherResponseDto.Weather.class);
        when(w0.getDescription()).thenReturn("clear sky");

        OpenWeatherResponseDto dto = mock(OpenWeatherResponseDto.class);
        when(dto.getMain()).thenReturn(main);
        when(dto.getWeather()).thenReturn(List.of(w0));

        // Intercept new RestTemplate() inside production code
        try (MockedConstruction<RestTemplate> mocked = Mockito.mockConstruction(
                RestTemplate.class,
                (mock, ctx) -> when(mock.getForObject(anyString(), eq(OpenWeatherResponseDto.class))).thenReturn(dto)
        )) {
            newJob().execute(context);
        }

        verify(emailService, times(1)).sendSimpleEmail(
                eq("user@test.com"),
                contains("Weather Update for " + location),
                contains("Temperature:")
        );

        verify(executionRepository, times(2)).save(executionCaptor.capture());
        TaskExecution finalSave = executionCaptor.getAllValues().get(1);
        assertEquals(ExecutionStatus.SUCCESS, finalSave.getStatus());
        assertNotNull(finalSave.getResult());
        assertTrue(finalSave.getResult().contains("Weather email sent"));

        verify(taskRepository).save(taskCaptor.capture());
        assertNotNull(taskCaptor.getValue().getLastExecutedAt());
    }
}
