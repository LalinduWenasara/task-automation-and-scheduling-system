package com.adl.dc.ep.taskautomation.task_automation_and_scheduling_system.service;

import com.adl.dc.ep.taskautomation.task_automation_and_scheduling_system.dto.AuthResponse;
import com.adl.dc.ep.taskautomation.task_automation_and_scheduling_system.dto.LoginRequest;
import com.adl.dc.ep.taskautomation.task_automation_and_scheduling_system.dto.RegisterRequest;
import org.springframework.transaction.annotation.Transactional;

public interface AuthService {
    @Transactional
    AuthResponse register(RegisterRequest request);

    /**
     * login.
     *
     * @param request login request
     * @return {AuthResponse}
     */

    AuthResponse login(LoginRequest request);
}
