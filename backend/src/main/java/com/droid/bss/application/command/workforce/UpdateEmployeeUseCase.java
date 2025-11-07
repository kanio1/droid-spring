package com.droid.bss.application.command.workforce;

import com.droid.bss.domain.workforce.EmployeeEntity;
import com.droid.bss.infrastructure.database.repository.EmployeeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Use case for updating employees
 */
@Service
@Transactional
public class UpdateEmployeeUseCase {

    private final EmployeeRepository employeeRepository;

    public UpdateEmployeeUseCase(EmployeeRepository employeeRepository) {
        this.employeeRepository = employeeRepository;
    }

    public EmployeeEntity handle(String employeeId, UpdateEmployeeCommand command) {
        // Get existing employee
        EmployeeEntity employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new IllegalArgumentException("Employee not found: " + employeeId));

        // Update fields if provided
        if (command.firstName() != null) {
            employee.setFirstName(command.firstName());
        }
        if (command.lastName() != null) {
            employee.setLastName(command.lastName());
        }
        if (command.email() != null) {
            employee.setEmail(command.email());
        }
        if (command.phone() != null) {
            employee.setPhone(command.phone());
        }
        if (command.department() != null) {
            employee.setDepartment(command.department());
        }
        if (command.position() != null) {
            employee.setPosition(command.position());
        }
        if (command.managerId() != null) {
            employee.setManagerId(command.managerId());
        }
        if (command.hireDate() != null) {
            employee.setHireDate(command.hireDate());
        }
        if (command.territory() != null) {
            employee.setTerritory(command.territory());
        }
        if (command.serviceArea() != null) {
            employee.setServiceArea(command.serviceArea());
        }
        if (command.address() != null) {
            employee.setAddress(command.address());
        }
        if (command.emergencyContact() != null) {
            employee.setEmergencyContact(command.emergencyContact());
        }
        if (command.emergencyPhone() != null) {
            employee.setEmergencyPhone(command.emergencyPhone());
        }
        if (command.notes() != null) {
            employee.setNotes(command.notes());
        }
        if (command.hourlyRate() != null) {
            employee.setHourlyRate(command.hourlyRate());
        }
        if (command.maxDailyJobs() != null) {
            employee.setMaxDailyJobs(command.maxDailyJobs());
        }
        if (command.onCall() != null) {
            employee.setOnCall(command.onCall());
        }

        return employeeRepository.save(employee);
    }

    // Command DTO
    public record UpdateEmployeeCommand(
            String firstName,
            String lastName,
            String email,
            String phone,
            String department,
            String position,
            String managerId,
            java.time.LocalDate hireDate,
            String territory,
            String serviceArea,
            String address,
            String emergencyContact,
            String emergencyPhone,
            String notes,
            java.math.BigDecimal hourlyRate,
            Integer maxDailyJobs,
            Boolean onCall
    ) {}
}
