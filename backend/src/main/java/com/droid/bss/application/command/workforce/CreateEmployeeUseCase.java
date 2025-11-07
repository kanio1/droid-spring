package com.droid.bss.application.command.workforce;

import com.droid.bss.domain.workforce.EmployeeEntity;
import com.droid.bss.domain.workforce.EmployeeType;
import com.droid.bss.infrastructure.database.repository.EmployeeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Use case for creating employees
 */
@Service
@Transactional
public class CreateEmployeeUseCase {

    private final EmployeeRepository employeeRepository;

    public CreateEmployeeUseCase(EmployeeRepository employeeRepository) {
        this.employeeRepository = employeeRepository;
    }

    public EmployeeEntity handle(CreateEmployeeCommand command) {
        // Validate
        if (command.firstName() == null || command.firstName().trim().isEmpty()) {
            throw new IllegalArgumentException("First name is required");
        }
        if (command.lastName() == null || command.lastName().trim().isEmpty()) {
            throw new IllegalArgumentException("Last name is required");
        }
        if (command.email() == null || command.email().trim().isEmpty()) {
            throw new IllegalArgumentException("Email is required");
        }
        if (command.employeeType() == null) {
            throw new IllegalArgumentException("Employee type is required");
        }

        // Generate employee code if not provided
        String employeeCode = command.employeeCode() != null ?
                command.employeeCode() : generateEmployeeCode();

        // Check if email or code already exists
        if (employeeRepository.findByEmail(command.email()).isPresent()) {
            throw new IllegalArgumentException("Email already exists: " + command.email());
        }
        if (employeeRepository.findByEmployeeCode(employeeCode).isPresent()) {
            throw new IllegalArgumentException("Employee code already exists: " + employeeCode);
        }

        // Create employee
        EmployeeEntity employee = new EmployeeEntity(
                employeeCode,
                command.firstName(),
                command.lastName(),
                command.email(),
                command.employeeType()
        );
        employee.setPhone(command.phone());
        employee.setDepartment(command.department());
        employee.setPosition(command.position());
        employee.setManagerId(command.managerId());
        employee.setHireDate(command.hireDate());
        employee.setTerritory(command.territory());
        employee.setServiceArea(command.serviceArea());
        employee.setAddress(command.address());
        employee.setEmergencyContact(command.emergencyContact());
        employee.setEmergencyPhone(command.emergencyPhone());
        employee.setNotes(command.notes());
        employee.setHourlyRate(command.hourlyRate());
        employee.setMaxDailyJobs(command.maxDailyJobs());
        employee.setOnCall(command.onCall());

        return employeeRepository.save(employee);
    }

    private String generateEmployeeCode() {
        return "EMP-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    // Command DTO
    public record CreateEmployeeCommand(
            String employeeCode,
            String firstName,
            String lastName,
            String email,
            String phone,
            String department,
            EmployeeType employeeType,
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
