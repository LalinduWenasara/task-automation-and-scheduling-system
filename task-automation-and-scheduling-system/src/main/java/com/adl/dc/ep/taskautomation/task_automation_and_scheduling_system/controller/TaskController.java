package com.adl.dc.ep.taskautomation.task_automation_and_scheduling_system.controller;


import com.adl.dc.ep.taskautomation.task_automation_and_scheduling_system.dto.ApiResponse;
import com.adl.dc.ep.taskautomation.task_automation_and_scheduling_system.dto.TaskRequest;
import com.adl.dc.ep.taskautomation.task_automation_and_scheduling_system.dto.TaskResponse;
import com.adl.dc.ep.taskautomation.task_automation_and_scheduling_system.service.TaskService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse> createTask(@Valid @RequestBody TaskRequest request) {
        TaskResponse task = taskService.createTask(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse(true, "Task created successfully", task));
    }

}
