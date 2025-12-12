package com.adl.dc.ep.taskautomation.task_automation_and_scheduling_system.repository;

import com.adl.dc.ep.taskautomation.task_automation_and_scheduling_system.domain.Task;
import com.adl.dc.ep.taskautomation.task_automation_and_scheduling_system.domain.User;
import com.adl.dc.ep.taskautomation.task_automation_and_scheduling_system.enums.TaskStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TaskRepository  extends JpaRepository<Task, Long> {
    List<Task> findByUserId(Long userId);
    List<Task> findByUserIdAndStatus(Long userId, TaskStatus status);
    List<Task> findByStatus(TaskStatus status);
}
