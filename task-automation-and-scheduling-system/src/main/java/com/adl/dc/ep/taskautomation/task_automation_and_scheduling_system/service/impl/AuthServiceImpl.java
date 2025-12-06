package com.adl.dc.ep.taskautomation.task_automation_and_scheduling_system.service.impl;

import com.adl.dc.ep.taskautomation.task_automation_and_scheduling_system.domain.User;
import com.adl.dc.ep.taskautomation.task_automation_and_scheduling_system.dto.AuthResponse;
import com.adl.dc.ep.taskautomation.task_automation_and_scheduling_system.dto.LoginRequest;
import com.adl.dc.ep.taskautomation.task_automation_and_scheduling_system.dto.RegisterRequest;
import com.adl.dc.ep.taskautomation.task_automation_and_scheduling_system.enums.Role;
import com.adl.dc.ep.taskautomation.task_automation_and_scheduling_system.exception.UserExistsException;
import com.adl.dc.ep.taskautomation.task_automation_and_scheduling_system.repository.UserRepository;
import com.adl.dc.ep.taskautomation.task_automation_and_scheduling_system.security.JwtService;
import com.adl.dc.ep.taskautomation.task_automation_and_scheduling_system.service.AuthService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    public AuthServiceImpl(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       AuthenticationManager authenticationManager) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
    }

    @Transactional
    @Override
    public AuthResponse register(RegisterRequest request) {

        if (userRepository.existsByUsername(request.getUsername())) {
            throw new UserExistsException("Username already exists");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new UserExistsException("Email already exists");
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(Role.USER);
        user.setEnabled(true);

        userRepository.save(user);

        String jwtToken = jwtService.generateToken(user);

        return new AuthResponse(jwtToken, user.getUsername(), user.getEmail(), "User registered successfully");
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getUsername(),
                        request.getPassword()
                )
        );

        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow();

        String jwtToken = jwtService.generateToken(user);

        return new AuthResponse(jwtToken, user.getUsername(), user.getEmail(), "Login successful");
    }
}
