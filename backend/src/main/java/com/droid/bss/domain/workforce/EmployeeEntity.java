package com.droid.bss.domain.workforce;

import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Employee/Technician entity for workforce management
 */
@Entity
@Table(name = "employees")
@EntityListeners(AuditingEntityListener.class)
public class EmployeeEntity {

    @Id
    private String id;

    @Column(nullable = false, unique = true)
    private String employeeCode;

    @Column(nullable = false)
    private String firstName;

    @Column(nullable = false)
    private String lastName;

    @Column(nullable = false, unique = true)
    private String email;

    @Column
    private String phone;

    @Column
    private String department;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EmployeeType employeeType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EmployeeStatus status;

    @Column
    private String position;

    @Column
    private String managerId;

    @Column
    private LocalDate hireDate;

    @Column
    private LocalDate terminationDate;

    @Column
    private String territory;

    @Column
    private String serviceArea;

    @Column
    private String address;

    @Column
    private String emergencyContact;

    @Column
    private String emergencyPhone;

    @Column
    private String notes;

    @Column(precision = 10, scale = 2)
    private java.math.BigDecimal hourlyRate = java.math.BigDecimal.ZERO;

    @Column
    private Integer maxDailyJobs = 5;

    @Column
    private Boolean onCall = false;

    @OneToMany(mappedBy = "employee", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<EmployeeSkill> skills = new ArrayList<>();

    @OneToMany(mappedBy = "employee", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<WorkOrderAssignment> assignments = new ArrayList<>();

    @OneToMany(mappedBy = "employee", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TimeSheetEntry> timeSheetEntries = new ArrayList<>();

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    // Constructors
    public EmployeeEntity() {
    }

    public EmployeeEntity(String employeeCode, String firstName, String lastName, String email, EmployeeType employeeType) {
        this.employeeCode = employeeCode;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.employeeType = employeeType;
        this.status = EmployeeStatus.ACTIVE;
    }

    // Business methods
    public void activate() {
        this.status = EmployeeStatus.ACTIVE;
    }

    public void suspend() {
        this.status = EmployeeStatus.SUSPENDED;
    }

    public void terminate() {
        this.status = EmployeeStatus.TERMINATED;
        this.terminationDate = LocalDate.now();
    }

    public void addSkill(String skillName, String level, String certificationNumber) {
        EmployeeSkill skill = new EmployeeSkill(this, skillName, level, certificationNumber);
        skills.add(skill);
    }

    public void removeSkill(String skillName) {
        skills.removeIf(s -> s.getSkillName().equals(skillName));
    }

    public boolean hasSkill(String skillName) {
        return skills.stream().anyMatch(s -> s.getSkillName().equals(skillName) && s.isActive());
    }

    public void updateAvailability(String dayOfWeek, Boolean available) {
        // Implementation for weekly availability
    }

    // Getters and Setters
    public String getId() { return id; }

    public void setId(String id) { this.id = id; }

    public String getEmployeeCode() { return employeeCode; }

    public void setEmployeeCode(String employeeCode) { this.employeeCode = employeeCode; }

    public String getFirstName() { return firstName; }

    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }

    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getEmail() { return email; }

    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }

    public void setPhone(String phone) { this.phone = phone; }

    public String getDepartment() { return department; }

    public void setDepartment(String department) { this.department = department; }

    public EmployeeType getEmployeeType() { return employeeType; }

    public void setEmployeeType(EmployeeType employeeType) { this.employeeType = employeeType; }

    public EmployeeStatus getStatus() { return status; }

    public void setStatus(EmployeeStatus status) { this.status = status; }

    public String getPosition() { return position; }

    public void setPosition(String position) { this.position = position; }

    public String getManagerId() { return managerId; }

    public void setManagerId(String managerId) { this.managerId = managerId; }

    public LocalDate getHireDate() { return hireDate; }

    public void setHireDate(LocalDate hireDate) { this.hireDate = hireDate; }

    public LocalDate getTerminationDate() { return terminationDate; }

    public void setTerminationDate(LocalDate terminationDate) { this.terminationDate = terminationDate; }

    public String getTerritory() { return territory; }

    public void setTerritory(String territory) { this.territory = territory; }

    public String getServiceArea() { return serviceArea; }

    public void setServiceArea(String serviceArea) { this.serviceArea = serviceArea; }

    public String getAddress() { return address; }

    public void setAddress(String address) { this.address = address; }

    public String getEmergencyContact() { return emergencyContact; }

    public void setEmergencyContact(String emergencyContact) { this.emergencyContact = emergencyContact; }

    public String getEmergencyPhone() { return emergencyPhone; }

    public void setEmergencyPhone(String emergencyPhone) { this.emergencyPhone = emergencyPhone; }

    public String getNotes() { return notes; }

    public void setNotes(String notes) { this.notes = notes; }

    public java.math.BigDecimal getHourlyRate() { return hourlyRate; }

    public void setHourlyRate(java.math.BigDecimal hourlyRate) { this.hourlyRate = hourlyRate; }

    public Integer getMaxDailyJobs() { return maxDailyJobs; }

    public void setMaxDailyJobs(Integer maxDailyJobs) { this.maxDailyJobs = maxDailyJobs; }

    public Boolean getOnCall() { return onCall; }

    public void setOnCall(Boolean onCall) { this.onCall = onCall; }

    public List<EmployeeSkill> getSkills() { return skills; }

    public void setSkills(List<EmployeeSkill> skills) { this.skills = skills; }

    public List<WorkOrderAssignment> getAssignments() { return assignments; }

    public void setAssignments(List<WorkOrderAssignment> assignments) { this.assignments = assignments; }

    public List<TimeSheetEntry> getTimeSheetEntries() { return timeSheetEntries; }

    public void setTimeSheetEntries(List<TimeSheetEntry> timeSheetEntries) { this.timeSheetEntries = timeSheetEntries; }

    public LocalDateTime getCreatedAt() { return createdAt; }

    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }

    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public String getFullName() {
        return firstName + " " + lastName;
    }
}
