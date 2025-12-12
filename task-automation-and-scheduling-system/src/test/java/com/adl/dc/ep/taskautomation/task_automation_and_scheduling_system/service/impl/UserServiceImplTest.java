package com.adl.dc.ep.taskautomation.task_automation_and_scheduling_system.service.impl;

import com.adl.dc.ep.taskautomation.task_automation_and_scheduling_system.domain.User;
import com.adl.dc.ep.taskautomation.task_automation_and_scheduling_system.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserServiceImpl userService;

    @Test
    void loadUserByUsername_shouldReturnUserDetails_whenUserExists() {
        // ---------- Arrange ----------
        String username = "testuser";
        User user = new User();
        user.setUsername(username);
        user.setPassword("encodedPassword");

        when(userRepository.findByUsername(username)).thenReturn(Optional.of(user));

        // ---------- Act ----------
        UserDetails result = userService.loadUserByUsername(username);

        // ---------- Assert ----------
        assertNotNull(result);
        assertEquals(username, result.getUsername());
        // you can assert password too if needed
        assertEquals("encodedPassword", result.getPassword());

        verify(userRepository, times(1)).findByUsername(username);
    }

    @Test
    void loadUserByUsername_shouldThrowException_whenUserNotFound() {
        // ---------- Arrange ----------
        String username = "unknown";
        when(userRepository.findByUsername(username)).thenReturn(Optional.empty());

        // ---------- Act + Assert ----------
        UsernameNotFoundException ex = assertThrows(
                UsernameNotFoundException.class,
                () -> userService.loadUserByUsername(username)
        );

        assertEquals("User not found: " + username, ex.getMessage());
        verify(userRepository, times(1)).findByUsername(username);
    }

    @Test
    void findByUsername_shouldReturnUser_whenUserExists() {
        // ---------- Arrange ----------
        String username = "testuser2";
        User user = new User();
        user.setUsername(username);
        user.setEmail("test2@example.com");

        when(userRepository.findByUsername(username)).thenReturn(Optional.of(user));

        // ---------- Act ----------
        User result = userService.findByUsername(username);

        // ---------- Assert ----------
        assertNotNull(result);
        assertEquals(username, result.getUsername());
        assertEquals("test2@example.com", result.getEmail());

        verify(userRepository, times(1)).findByUsername(username);
    }

    @Test
    void findByUsername_shouldThrowException_whenUserNotFound() {
        // ---------- Arrange ----------
        String username = "missing-user";
        when(userRepository.findByUsername(username)).thenReturn(Optional.empty());

        // ---------- Act + Assert ----------
        UsernameNotFoundException ex = assertThrows(
                UsernameNotFoundException.class,
                () -> userService.findByUsername(username)
        );

        assertEquals("User not found: " + username, ex.getMessage());
        verify(userRepository, times(1)).findByUsername(username);
    }
}
