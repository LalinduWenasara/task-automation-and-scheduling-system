package com.adl.dc.ep.taskautomation.task_automation_and_scheduling_system.repository;


import com.adl.dc.ep.taskautomation.task_automation_and_scheduling_system.domain.TaskExecution;
import com.adl.dc.ep.taskautomation.task_automation_and_scheduling_system.enums.ExecutionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TaskExecutionRepository extends JpaRepository<TaskExecution, Long> {
    List<TaskExecution> findByTaskId(Long taskId);
    List<TaskExecution> findByTaskIdAndStatus(Long taskId, ExecutionStatus status);
    List<TaskExecution> findByStartTimeBetween(LocalDateTime start, LocalDateTime end);

}
