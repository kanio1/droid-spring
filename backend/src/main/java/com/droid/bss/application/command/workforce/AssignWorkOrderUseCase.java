package com.droid.bss.application.command.workforce;

import com.droid.bss.domain.workforce.EmployeeEntity;
import com.droid.bss.domain.workforce.WorkOrderEntity;
import com.droid.bss.domain.workforce.WorkOrderStatus;
import com.droid.bss.infrastructure.database.repository.EmployeeRepository;
import com.droid.bss.infrastructure.database.repository.WorkOrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Use case for assigning work orders to technicians
 */
@Service
@Transactional
public class AssignWorkOrderUseCase {

    private final WorkOrderRepository workOrderRepository;
    private final EmployeeRepository employeeRepository;

    public AssignWorkOrderUseCase(
            WorkOrderRepository workOrderRepository,
            EmployeeRepository employeeRepository
    ) {
        this.workOrderRepository = workOrderRepository;
        this.employeeRepository = employeeRepository;
    }

    public WorkOrderEntity handle(String workOrderId, String employeeId, String assignedBy) {
        // Get work order
        WorkOrderEntity workOrder = workOrderRepository.findById(workOrderId)
                .orElseThrow(() -> new IllegalArgumentException("Work order not found: " + workOrderId));

        // Check if work order can be assigned
        if (workOrder.getStatus() != WorkOrderStatus.PENDING && workOrder.getStatus() != WorkOrderStatus.SCHEDULED) {
            throw new IllegalStateException("Work order cannot be assigned in status: " + workOrder.getStatus());
        }

        // Get employee
        EmployeeEntity employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new IllegalArgumentException("Employee not found: " + employeeId));

        // Check if employee is available
        if (employee.getStatus().name() != "ACTIVE") {
            throw new IllegalArgumentException("Employee is not active");
        }

        // Check capacity
        int currentAssignments = employee.getAssignments().size();
        if (currentAssignments >= employee.getMaxDailyJobs()) {
            throw new IllegalArgumentException("Employee has reached maximum daily job capacity");
        }

        // Check required skill
        if (workOrder.getRequiredSkill() != null && !employee.hasSkill(workOrder.getRequiredSkill())) {
            throw new IllegalArgumentException("Employee does not have required skill: " + workOrder.getRequiredSkill());
        }

        // Assign work order
        workOrder.assignToTechnician(employeeId);

        return workOrderRepository.save(workOrder);
    }

    public List<WorkOrderEntity> autoAssign(String workOrderId, String performedBy) {
        // Get work order
        WorkOrderEntity workOrder = workOrderRepository.findById(workOrderId)
                .orElseThrow(() -> new IllegalArgumentException("Work order not found: " + workOrderId));

        // Find suitable technicians
        List<EmployeeEntity> availableTechnicians = employeeRepository.findAvailableTechnicians();

        // Filter by skill if required
        if (workOrder.getRequiredSkill() != null) {
            availableTechnicians = availableTechnicians.stream()
                    .filter(e -> e.hasSkill(workOrder.getRequiredSkill()))
                    .toList();
        }

        // Filter by territory if specified
        if (workOrder.getServiceAddress() != null) {
            availableTechnicians = availableTechnicians.stream()
                    .filter(e -> e.getTerritory() == null ||
                            workOrder.getServiceAddress().contains(e.getTerritory()))
                    .toList();
        }

        // Sort by number of current assignments (least busy first)
        availableTechnicians.sort((e1, e2) -> {
            int count1 = e1.getAssignments().size();
            int count2 = e2.getAssignments().size();
            return Integer.compare(count1, count2);
        });

        // Find first available technician
        Optional<EmployeeEntity> bestMatch = availableTechnicians.stream().findFirst();

        if (bestMatch.isPresent()) {
            handle(workOrderId, bestMatch.get().getId(), performedBy);
        } else {
            throw new IllegalArgumentException("No suitable technician found for work order: " + workOrderId);
        }

        return List.of(workOrderRepository.findById(workOrderId).orElseThrow());
    }

    // Command DTO
    public record AssignWorkOrderCommand(
            String workOrderId,
            String employeeId,
            String assignedBy
    ) {}
}
