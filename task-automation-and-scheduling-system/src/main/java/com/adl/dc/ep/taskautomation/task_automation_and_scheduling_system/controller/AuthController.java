package com.adl.dc.ep.taskautomation.task_automation_and_scheduling_system.controller;

import com.adl.dc.ep.taskautomation.task_automation_and_scheduling_system.dto.AuthResponse;
import com.adl.dc.ep.taskautomation.task_automation_and_scheduling_system.dto.LoginRequest;
import com.adl.dc.ep.taskautomation.task_automation_and_scheduling_system.dto.RegisterRequest;
import com.adl.dc.ep.taskautomation.task_automation_and_scheduling_system.service.AuthService;
import com.adl.dc.ep.taskautomation.task_automation_and_scheduling_system.service.impl.AuthServiceImpl;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthServiceImpl authService;

    public AuthController(AuthServiceImpl authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }
}
