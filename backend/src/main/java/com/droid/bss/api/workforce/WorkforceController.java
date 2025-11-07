package com.droid.bss.api.workforce;

import com.droid.bss.application.command.workforce.*;
import com.droid.bss.domain.audit.AuditAction;
import com.droid.bss.domain.workforce.*;
import com.droid.bss.infrastructure.audit.Audited;
import com.droid.bss.infrastructure.database.repository.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * REST API for workforce management
 */
@RestController
@RequestMapping("/api/workforce")
public class WorkforceController {

    private final CreateEmployeeUseCase createEmployeeUseCase;
    private final UpdateEmployeeUseCase updateEmployeeUseCase;
    private final CreateWorkOrderUseCase createWorkOrderUseCase;
    private final AssignWorkOrderUseCase assignWorkOrderUseCase;
    private final EmployeeRepository employeeRepository;
    private final WorkOrderRepository workOrderRepository;
    private final WorkScheduleRepository scheduleRepository;
    private final TimeSheetRepository timeSheetRepository;

    public WorkforceController(
            CreateEmployeeUseCase createEmployeeUseCase,
            UpdateEmployeeUseCase updateEmployeeUseCase,
            CreateWorkOrderUseCase createWorkOrderUseCase,
            AssignWorkOrderUseCase assignWorkOrderUseCase,
            EmployeeRepository employeeRepository,
            WorkOrderRepository workOrderRepository,
            WorkScheduleRepository scheduleRepository,
            TimeSheetRepository timeSheetRepository
    ) {
        this.createEmployeeUseCase = createEmployeeUseCase;
        this.updateEmployeeUseCase = updateEmployeeUseCase;
        this.createWorkOrderUseCase = createWorkOrderUseCase;
        this.assignWorkOrderUseCase = assignWorkOrderUseCase;
        this.employeeRepository = employeeRepository;
        this.workOrderRepository = workOrderRepository;
        this.scheduleRepository = scheduleRepository;
        this.timeSheetRepository = timeSheetRepository;
    }

    // Employee endpoints
    @GetMapping("/employees")
    public ResponseEntity<List<EmployeeEntity>> getAllEmployees(
            @RequestParam(required = false) EmployeeType type,
            @RequestParam(required = false) EmployeeStatus status
    ) {
        List<EmployeeEntity> employees;
        if (type != null) {
            employees = employeeRepository.findByEmployeeType(type);
        } else if (status != null) {
            employees = employeeRepository.findByStatus(status);
        } else {
            employees = employeeRepository.findAll();
        }
        return ResponseEntity.ok(employees);
    }

    @GetMapping("/employees/{id}")
    public ResponseEntity<EmployeeEntity> getEmployeeById(@PathVariable String id) {
        Optional<EmployeeEntity> employee = employeeRepository.findById(id);
        return employee.map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/employees")
    @Audited(action = AuditAction.WORKFORCE_CREATE, entityType = "Employee", description = "Creating new employee")
    public ResponseEntity<EmployeeEntity> createEmployee(@RequestBody CreateEmployeeUseCase.CreateEmployeeCommand command) {
        EmployeeEntity employee = createEmployeeUseCase.handle(command);
        return ResponseEntity.ok(employee);
    }

    @PutMapping("/employees/{id}")
    @Audited(action = AuditAction.WORKFORCE_UPDATE, entityType = "Employee", description = "Updating employee {id}")
    public ResponseEntity<EmployeeEntity> updateEmployee(
            @PathVariable String id,
            @RequestBody UpdateEmployeeUseCase.UpdateEmployeeCommand command
    ) {
        EmployeeEntity employee = updateEmployeeUseCase.handle(id, command);
        return ResponseEntity.ok(employee);
    }

    @PostMapping("/employees/{id}/activate")
    @Audited(action = AuditAction.WORKFORCE_UPDATE, entityType = "Employee", description = "Activating employee {id}")
    public ResponseEntity<EmployeeEntity> activateEmployee(@PathVariable String id) {
        Optional<EmployeeEntity> employeeOpt = employeeRepository.findById(id);
        if (employeeOpt.isPresent()) {
            EmployeeEntity employee = employeeOpt.get();
            employee.activate();
            employeeRepository.save(employee);
            return ResponseEntity.ok(employee);
        }
        return ResponseEntity.notFound().build();
    }

    @PostMapping("/employees/{id}/suspend")
    @Audited(action = AuditAction.WORKFORCE_UPDATE, entityType = "Employee", description = "Suspending employee {id}")
    public ResponseEntity<EmployeeEntity> suspendEmployee(@PathVariable String id) {
        Optional<EmployeeEntity> employeeOpt = employeeRepository.findById(id);
        if (employeeOpt.isPresent()) {
            EmployeeEntity employee = employeeOpt.get();
            employee.suspend();
            employeeRepository.save(employee);
            return ResponseEntity.ok(employee);
        }
        return ResponseEntity.notFound().build();
    }

    @GetMapping("/employees/skills/{skillName}")
    public ResponseEntity<List<EmployeeEntity>> getEmployeesBySkill(@PathVariable String skillName) {
        List<EmployeeEntity> employees = employeeRepository.findBySkill(skillName);
        return ResponseEntity.ok(employees);
    }

    @GetMapping("/employees/available")
    public ResponseEntity<List<EmployeeEntity>> getAvailableTechnicians() {
        List<EmployeeEntity> technicians = employeeRepository.findAvailableTechnicians();
        return ResponseEntity.ok(technicians);
    }

    @GetMapping("/employees/on-call")
    public ResponseEntity<List<EmployeeEntity>> getOnCallTechnicians() {
        List<EmployeeEntity> technicians = employeeRepository.findOnCallTechnicians();
        return ResponseEntity.ok(technicians);
    }

    // Work Order endpoints
    @GetMapping("/work-orders")
    public ResponseEntity<List<WorkOrderEntity>> getAllWorkOrders(
            @RequestParam(required = false) WorkOrderType type,
            @RequestParam(required = false) WorkOrderStatus status,
            @RequestParam(required = false) LocalDate date
    ) {
        List<WorkOrderEntity> workOrders;
        if (type != null) {
            workOrders = workOrderRepository.findByType(type);
        } else if (status != null) {
            workOrders = workOrderRepository.findByStatus(status);
        } else if (date != null) {
            workOrders = workOrderRepository.findByScheduledDate(date);
        } else {
            workOrders = workOrderRepository.findAll();
        }
        return ResponseEntity.ok(workOrders);
    }

    @GetMapping("/work-orders/{id}")
    public ResponseEntity<WorkOrderEntity> getWorkOrderById(@PathVariable String id) {
        Optional<WorkOrderEntity> workOrder = workOrderRepository.findById(id);
        return workOrder.map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/work-orders")
    @Audited(action = AuditAction.WORKFORCE_CREATE, entityType = "WorkOrder", description = "Creating new work order")
    public ResponseEntity<WorkOrderEntity> createWorkOrder(@RequestBody CreateWorkOrderUseCase.CreateWorkOrderCommand command) {
        WorkOrderEntity workOrder = createWorkOrderUseCase.handle(command);
        return ResponseEntity.ok(workOrder);
    }

    @PostMapping("/work-orders/{id}/assign")
    @Audited(action = AuditAction.WORKFORCE_UPDATE, entityType = "WorkOrder", description = "Assigning work order {id}")
    public ResponseEntity<WorkOrderEntity> assignWorkOrder(
            @PathVariable String id,
            @RequestBody AssignWorkOrderUseCase.AssignWorkOrderCommand command
    ) {
        WorkOrderEntity workOrder = assignWorkOrderUseCase.handle(
                id,
                command.employeeId(),
                command.assignedBy()
        );
        return ResponseEntity.ok(workOrder);
    }

    @PostMapping("/work-orders/{id}/auto-assign")
    @Audited(action = AuditAction.WORKFORCE_UPDATE, entityType = "WorkOrder", description = "Auto-assigning work order {id}")
    public ResponseEntity<List<WorkOrderEntity>> autoAssignWorkOrder(
            @PathVariable String id,
            @RequestBody Map<String, String> request
    ) {
        String performedBy = request.get("performedBy");
        List<WorkOrderEntity> workOrders = assignWorkOrderUseCase.autoAssign(id, performedBy);
        return ResponseEntity.ok(workOrders);
    }

    @PostMapping("/work-orders/{id}/start")
    @Audited(action = AuditAction.WORKFORCE_UPDATE, entityType = "WorkOrder", description = "Starting work on work order {id}")
    public ResponseEntity<WorkOrderEntity> startWorkOrder(@PathVariable String id, @RequestBody Map<String, String> request) {
        String employeeId = request.get("employeeId");
        Optional<WorkOrderEntity> workOrderOpt = workOrderRepository.findById(id);
        if (workOrderOpt.isPresent()) {
            WorkOrderEntity workOrder = workOrderOpt.get();
            workOrder.startWork(employeeId);
            workOrderRepository.save(workOrder);
            return ResponseEntity.ok(workOrder);
        }
        return ResponseEntity.notFound().build();
    }

    @PostMapping("/work-orders/{id}/complete")
    @Audited(action = AuditAction.WORKFORCE_UPDATE, entityType = "WorkOrder", description = "Completing work order {id}")
    public ResponseEntity<WorkOrderEntity> completeWorkOrder(
            @PathVariable String id,
            @RequestBody Map<String, String> request
    ) {
        String completionNotes = request.get("completionNotes");
        Optional<WorkOrderEntity> workOrderOpt = workOrderRepository.findById(id);
        if (workOrderOpt.isPresent()) {
            WorkOrderEntity workOrder = workOrderOpt.get();
            workOrder.completeWork(completionNotes);
            workOrderRepository.save(workOrder);
            return ResponseEntity.ok(workOrder);
        }
        return ResponseEntity.notFound().build();
    }

    @GetMapping("/work-orders/pending")
    public ResponseEntity<List<WorkOrderEntity>> getPendingWorkOrders() {
        List<WorkOrderEntity> workOrders = workOrderRepository.findPendingWorkOrders();
        return ResponseEntity.ok(workOrders);
    }

    @GetMapping("/work-orders/overdue")
    public ResponseEntity<List<WorkOrderEntity>> getOverdueWorkOrders() {
        List<WorkOrderEntity> workOrders = workOrderRepository.findOverdueWorkOrders(LocalDate.now());
        return ResponseEntity.ok(workOrders);
    }

    @GetMapping("/work-orders/emergency")
    public ResponseEntity<List<WorkOrderEntity>> getEmergencyWorkOrders() {
        List<WorkOrderEntity> workOrders = workOrderRepository.findEmergencyWorkOrders();
        return ResponseEntity.ok(workOrders);
    }

    // Statistics endpoint
    @GetMapping("/statistics")
    public ResponseEntity<WorkforceStatistics> getWorkforceStatistics() {
        long totalEmployees = employeeRepository.count();
        long activeEmployees = employeeRepository.countByStatus(EmployeeStatus.ACTIVE);
        long technicians = employeeRepository.countByStatus(EmployeeStatus.ACTIVE);
        long onCall = employeeRepository.findOnCallTechnicians().size();

        long pendingWorkOrders = workOrderRepository.countByStatus(WorkOrderStatus.PENDING);
        long inProgressWorkOrders = workOrderRepository.countByStatus(WorkOrderStatus.IN_PROGRESS);
        long overdueWorkOrders = workOrderRepository.findOverdueWorkOrders(LocalDate.now()).size();

        return ResponseEntity.ok(new WorkforceStatistics(
                totalEmployees,
                activeEmployees,
                technicians,
                onCall,
                pendingWorkOrders,
                inProgressWorkOrders,
                overdueWorkOrders
        ));
    }

    // DTOs
    public record WorkforceStatistics(
            long totalEmployees,
            long activeEmployees,
            long technicians,
            long onCall,
            long pendingWorkOrders,
            long inProgressWorkOrders,
            long overdueWorkOrders
    ) {}
}
