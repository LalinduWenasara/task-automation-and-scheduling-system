package com.adl.dc.ep.taskautomation.task_automation_and_scheduling_system.controller;

import com.adl.dc.ep.taskautomation.task_automation_and_scheduling_system.dto.ApiResponse;
import com.adl.dc.ep.taskautomation.task_automation_and_scheduling_system.dto.TaskRequest;
import com.adl.dc.ep.taskautomation.task_automation_and_scheduling_system.dto.TaskResponse;
import com.adl.dc.ep.taskautomation.task_automation_and_scheduling_system.service.TaskService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaskControllerTest {

    @InjectMocks
    private TaskController taskController;

    @Mock
    private TaskService taskService;

    @Test
    void createTask_shouldReturnCreated_andWrappedApiResponse() {
        // given
        TaskRequest request = mock(TaskRequest.class);
        TaskResponse created = mock(TaskResponse.class);

        when(taskService.createTask(request)).thenReturn(created);

        // when
        ResponseEntity<ApiResponse> response = taskController.createTask(request);

        // then
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());

        ApiResponse body = response.getBody();
        assertTrue(body.isSuccess());
        assertEquals("Task created successfully", body.getMessage());
        assertSame(created, body.getData());

        verify(taskService).createTask(request);
        verifyNoMoreInteractions(taskService);
    }

    @Test
    void updateTask_shouldReturnOk_andWrappedApiResponse() {
        // given
        Long taskId = 10L;
        TaskRequest request = mock(TaskRequest.class);
        TaskResponse updated = mock(TaskResponse.class);

        when(taskService.updateTask(taskId, request)).thenReturn(updated);

        // when
        ResponseEntity<ApiResponse> response = taskController.updateTask(taskId, request);

        // then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());

        ApiResponse body = response.getBody();
        assertTrue(body.isSuccess());
        assertEquals("Task updated successfully", body.getMessage());
        assertSame(updated, body.getData());

        verify(taskService).updateTask(taskId, request);
        verifyNoMoreInteractions(taskService);
    }

    @Test
    void deleteTask_shouldReturnOk_andNullData() {
        // given
        Long taskId = 99L;
        doNothing().when(taskService).deleteTask(taskId);

        // when
        ResponseEntity<ApiResponse> response = taskController.deleteTask(taskId);

        // then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());

        ApiResponse body = response.getBody();
        assertTrue(body.isSuccess());
        assertEquals("Task deleted successfully", body.getMessage());
        assertNull(body.getData());

        verify(taskService).deleteTask(taskId);
        verifyNoMoreInteractions(taskService);
    }

}
