package com.droid.bss.application.command.workforce;

import com.droid.bss.domain.workforce.WorkOrderEntity;
import com.droid.bss.domain.workforce.WorkOrderType;
import com.droid.bss.infrastructure.database.repository.WorkOrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Use case for creating work orders
 */
@Service
@Transactional
public class CreateWorkOrderUseCase {

    private final WorkOrderRepository workOrderRepository;

    public CreateWorkOrderUseCase(WorkOrderRepository workOrderRepository) {
        this.workOrderRepository = workOrderRepository;
    }

    public WorkOrderEntity handle(CreateWorkOrderCommand command) {
        // Validate
        if (command.title() == null || command.title().trim().isEmpty()) {
            throw new IllegalArgumentException("Title is required");
        }
        if (command.type() == null) {
            throw new IllegalArgumentException("Work order type is required");
        }

        // Generate work order number if not provided
        String workOrderNumber = command.workOrderNumber() != null ?
                command.workOrderNumber() : generateWorkOrderNumber();

        // Check if number already exists
        if (workOrderRepository.findByWorkOrderNumber(workOrderNumber).isPresent()) {
            throw new IllegalArgumentException("Work order number already exists: " + workOrderNumber);
        }

        // Create work order
        WorkOrderEntity workOrder = new WorkOrderEntity(workOrderNumber, command.type(), command.title());
        workOrder.setDescription(command.description());
        workOrder.setPriority(command.priority());
        workOrder.setCustomerId(command.customerId());
        workOrder.setServiceAddress(command.serviceAddress());
        workOrder.setServiceType(command.serviceType());
        workOrder.setRequiredSkill(command.requiredSkill());
        workOrder.setEstimatedDuration(command.estimatedDuration());
        workOrder.setScheduledDate(command.scheduledDate());
        workOrder.setRequestedDate(command.requestedDate());
        workOrder.setDueDate(command.dueDate());
        workOrder.setCustomerContact(command.customerContact());
        workOrder.setCustomerPhone(command.customerPhone());
        workOrder.setNotes(command.notes());
        workOrder.setAttachments(command.attachments());

        WorkOrderEntity saved = workOrderRepository.save(workOrder);

        // Add default tasks if specified
        if (command.addDefaultTasks() != null && command.addDefaultTasks()) {
            addDefaultTasks(saved);
        }

        return workOrderRepository.save(saved);
    }

    private void addDefaultTasks(WorkOrderEntity workOrder) {
        // Add common tasks based on work order type
        workOrder.addTask("Prepare equipment and tools", true, 1);
        workOrder.addTask("Travel to customer location", true, 2);
        workOrder.addTask("Perform work", true, 3);
        workOrder.addTask("Test and verify service", true, 4);
        workOrder.addTask("Clean up work area", true, 5);
    }

    private String generateWorkOrderNumber() {
        return "WO-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    // Command DTO
    public record CreateWorkOrderCommand(
            String workOrderNumber,
            String title,
            WorkOrderType type,
            String description,
            String priority,
            String customerId,
            String serviceAddress,
            String serviceType,
            String requiredSkill,
            Integer estimatedDuration,
            java.time.LocalDate scheduledDate,
            java.time.LocalDate requestedDate,
            java.time.LocalDate dueDate,
            String customerContact,
            String customerPhone,
            String notes,
            String attachments,
            Boolean addDefaultTasks
    ) {}
}
