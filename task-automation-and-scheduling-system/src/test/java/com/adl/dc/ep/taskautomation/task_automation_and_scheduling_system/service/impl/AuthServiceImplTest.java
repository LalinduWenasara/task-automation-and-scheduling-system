package com.adl.dc.ep.taskautomation.task_automation_and_scheduling_system.service.impl;

import com.adl.dc.ep.taskautomation.task_automation_and_scheduling_system.domain.User;
import com.adl.dc.ep.taskautomation.task_automation_and_scheduling_system.dto.AuthResponse;
import com.adl.dc.ep.taskautomation.task_automation_and_scheduling_system.dto.LoginRequest;
import com.adl.dc.ep.taskautomation.task_automation_and_scheduling_system.dto.RegisterRequest;
import com.adl.dc.ep.taskautomation.task_automation_and_scheduling_system.enums.Role;
import com.adl.dc.ep.taskautomation.task_automation_and_scheduling_system.exception.UserExistsException;
import com.adl.dc.ep.taskautomation.task_automation_and_scheduling_system.repository.UserRepository;
import com.adl.dc.ep.taskautomation.task_automation_and_scheduling_system.security.JwtService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private AuthenticationManager authenticationManager;

    @InjectMocks
    private AuthServiceImpl authService;

    @Test
    void register_shouldCreateUserAndReturnAuthResponse() {

        RegisterRequest request = new RegisterRequest();
        request.setUsername("testuser");
        request.setEmail("test@example.com");
        request.setPassword("plainPassword");

        when(userRepository.existsByUsername("testuser")).thenReturn(false);
        when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
        when(passwordEncoder.encode("plainPassword")).thenReturn("encodedPassword");

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);

        when(jwtService.generateToken(any(User.class))).thenReturn("jwt-token");


        AuthResponse response = authService.register(request);

        // Assert
        assertNotNull(response);
        assertEquals("jwt-token", response.getToken());
        assertEquals("testuser", response.getUsername());
        assertEquals("test@example.com", response.getEmail());
        assertEquals("User registered successfully", response.getMessage());

        // verify user save and captured values
        verify(userRepository, times(1)).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();
        assertEquals("testuser", savedUser.getUsername());
        assertEquals("test@example.com", savedUser.getEmail());
        assertEquals("encodedPassword", savedUser.getPassword());
        assertEquals(Role.USER, savedUser.getRole());
        assertTrue(savedUser.isEnabled());

        verify(jwtService, times(1)).generateToken(savedUser);
    }

    @Test
    void register_whenUsernameExists_shouldThrowUserExistsException() {

        RegisterRequest request = new RegisterRequest();
        request.setUsername("existingUser");
        request.setEmail("newemail@example.com");
        request.setPassword("password");

        when(userRepository.existsByUsername("existingUser")).thenReturn(true);

        UserExistsException ex = assertThrows(
                UserExistsException.class,
                () -> authService.register(request)
        );

        assertEquals("Username already exists", ex.getMessage());

        verify(userRepository, times(1)).existsByUsername("existingUser");
        // no further calls
        verify(userRepository, never()).existsByEmail(any());
        verify(userRepository, never()).save(any());
        verify(jwtService, never()).generateToken(any());
    }

    @Test
    void register_whenEmailExists_shouldThrowUserExistsException() {
        // ---------- Arrange ----------
        RegisterRequest request = new RegisterRequest();
        request.setUsername("newUser");
        request.setEmail("existing@example.com");
        request.setPassword("password");

        when(userRepository.existsByUsername("newUser")).thenReturn(false);
        when(userRepository.existsByEmail("existing@example.com")).thenReturn(true);

        // ---------- Act + Assert ----------
        UserExistsException ex = assertThrows(
                UserExistsException.class,
                () -> authService.register(request)
        );

        assertEquals("Email already exists", ex.getMessage());

        verify(userRepository, times(1)).existsByUsername("newUser");
        verify(userRepository, times(1)).existsByEmail("existing@example.com");
        verify(userRepository, never()).save(any());
        verify(jwtService, never()).generateToken(any());
    }

    @Test
    void login_shouldAuthenticateAndReturnAuthResponse() {
        // ---------- Arrange ----------
        LoginRequest request = new LoginRequest();
        request.setUsername("testuser");
        request.setPassword("password");

        User user = new User();
        user.setUsername("testuser");
        user.setEmail("test@example.com");

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(jwtService.generateToken(user)).thenReturn("jwt-login-token");

        AuthResponse response = authService.login(request);

        assertNotNull(response);
        assertEquals("jwt-login-token", response.getToken());
        assertEquals("testuser", response.getUsername());
        assertEquals("test@example.com", response.getEmail());
        assertEquals("Login successful", response.getMessage());

        // verify authenticate call
        verify(authenticationManager, times(1)).authenticate(
                new UsernamePasswordAuthenticationToken("testuser", "password")
        );
        verify(userRepository, times(1)).findByUsername("testuser");
        verify(jwtService, times(1)).generateToken(user);
    }

    @Test
    void login_whenUserNotFound_shouldThrowException() {
        // ---------- Arrange ----------
        LoginRequest request = new LoginRequest();
        request.setUsername("unknown");
        request.setPassword("password");

        when(userRepository.findByUsername("unknown")).thenReturn(Optional.empty());

        // ---------- Act + Assert ----------
        // login() calls orElseThrow() with default, which throws NoSuchElementException
        assertThrows(
                java.util.NoSuchElementException.class,
                () -> authService.login(request)
        );

        verify(authenticationManager, times(1)).authenticate(
                new UsernamePasswordAuthenticationToken("unknown", "password")
        );
        verify(userRepository, times(1)).findByUsername("unknown");
        verify(jwtService, never()).generateToken(any());
    }
}
