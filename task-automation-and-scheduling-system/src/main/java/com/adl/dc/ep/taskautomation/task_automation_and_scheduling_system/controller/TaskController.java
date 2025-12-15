package com.adl.dc.ep.taskautomation.task_automation_and_scheduling_system.controller;


import com.adl.dc.ep.taskautomation.task_automation_and_scheduling_system.dto.ApiResponse;
import com.adl.dc.ep.taskautomation.task_automation_and_scheduling_system.dto.TaskRequest;
import com.adl.dc.ep.taskautomation.task_automation_and_scheduling_system.dto.TaskResponse;
import com.adl.dc.ep.taskautomation.task_automation_and_scheduling_system.service.TaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tasks")
@Tag(name = "Task Management", description = "APIs for managing scheduled tasks")
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @Operation(
            summary = "Create a new task",
            description = "Creates a scheduled task and registers it in the system."
    )
    @PostMapping
    public ResponseEntity<ApiResponse> createTask(@Valid @RequestBody TaskRequest request) {
        TaskResponse task = taskService.createTask(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse(true, "Task created successfully", task));
    }

    @Operation(
            summary = "Update an existing task",
            description = "Updates task details by task ID."
    )
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse> updateTask(@PathVariable Long id,
                                                  @Valid @RequestBody TaskRequest request) {
        TaskResponse task = taskService.updateTask(id, request);
        return ResponseEntity.ok(new ApiResponse(true, "Task updated successfully", task));
    }

    @Operation(
            summary = "Delete a task",
            description = "Deletes a task by ID."
    )
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse> deleteTask(@PathVariable Long id) {
        taskService.deleteTask(id);
        return ResponseEntity.ok(new ApiResponse(true, "Task deleted successfully", null));
    }

    @Operation(
            summary = "Get all tasks",
            description = "Retrieves all tasks belonging to the logged-in user."
    )
    @GetMapping
    public ResponseEntity<ApiResponse> getAllTasks() {
        List<TaskResponse> tasks = taskService.getAllUserTasks();
        return ResponseEntity.ok(new ApiResponse(true, "Tasks retrieved successfully", tasks));
    }

    @Operation(
            summary = "Get task by ID",
            description = "Retrieves a single task by its ID."
    )
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse> getTask(@PathVariable Long id) {
        TaskResponse task = taskService.getTask(id);
        return ResponseEntity.ok(new ApiResponse(true, "Task retrieved successfully", task));
    }

}
