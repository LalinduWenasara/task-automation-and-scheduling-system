package com.adl.dc.ep.taskautomation.task_automation_and_scheduling_system.controller;

import com.adl.dc.ep.taskautomation.task_automation_and_scheduling_system.dto.AuthResponse;
import com.adl.dc.ep.taskautomation.task_automation_and_scheduling_system.dto.LoginRequest;
import com.adl.dc.ep.taskautomation.task_automation_and_scheduling_system.dto.RegisterRequest;
import com.adl.dc.ep.taskautomation.task_automation_and_scheduling_system.service.impl.AuthServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @InjectMocks
    private AuthController authController;

    @Mock
    private AuthServiceImpl authService;

    @Test
    void register_shouldReturnCreated_andAuthResponse() {
        // given
        RegisterRequest request = mock(RegisterRequest.class);
        AuthResponse authResponse = mock(AuthResponse.class);

        when(authService.register(request)).thenReturn(authResponse);

        // when
        ResponseEntity<AuthResponse> response = authController.register(request);

        // then
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertSame(authResponse, response.getBody());

        verify(authService).register(request);
        verifyNoMoreInteractions(authService);
    }

    @Test
    void login_shouldReturnOk_andAuthResponse() {
        // given
        LoginRequest request = mock(LoginRequest.class);
        AuthResponse authResponse = mock(AuthResponse.class);

        when(authService.login(request)).thenReturn(authResponse);

        // when
        ResponseEntity<AuthResponse> response = authController.login(request);

        // then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(authResponse, response.getBody());

        verify(authService).login(request);
        verifyNoMoreInteractions(authService);
    }
}
