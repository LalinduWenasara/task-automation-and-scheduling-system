package com.adl.dc.ep.taskautomation.task_automation_and_scheduling_system.repository;


import com.adl.dc.ep.taskautomation.task_automation_and_scheduling_system.domain.TaskExecution;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TaskExecutionRepository extends JpaRepository<TaskExecution, Long> {

}
