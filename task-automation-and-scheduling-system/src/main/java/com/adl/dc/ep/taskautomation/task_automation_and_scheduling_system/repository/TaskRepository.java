package com.adl.dc.ep.taskautomation.task_automation_and_scheduling_system.repository;

import com.adl.dc.ep.taskautomation.task_automation_and_scheduling_system.domain.Task;
import com.adl.dc.ep.taskautomation.task_automation_and_scheduling_system.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TaskRepository  extends JpaRepository<Task, Long> {
}
