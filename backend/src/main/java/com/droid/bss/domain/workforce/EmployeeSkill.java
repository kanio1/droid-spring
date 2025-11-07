package com.droid.bss.domain.workforce;

import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Employee skills and certifications
 */
@Entity
@Table(name = "employee_skills")
@EntityListeners(AuditingEntityListener.class)
public class EmployeeSkill {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private EmployeeEntity employee;

    @Column(nullable = false)
    private String skillName;

    @Column
    private String skillCategory;

    @Column
    private String level;

    @Column
    private String certificationNumber;

    @Column
    private LocalDate certificationDate;

    @Column
    private LocalDate expirationDate;

    @Column
    private String certifyingBody;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(nullable = false)
    private Boolean active = true;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // Constructors
    public EmployeeSkill() {
    }

    public EmployeeSkill(EmployeeEntity employee, String skillName, String level, String certificationNumber) {
        this.employee = employee;
        this.skillName = skillName;
        this.level = level;
        this.certificationNumber = certificationNumber;
    }

    // Business methods
    public boolean isExpired() {
        return expirationDate != null && LocalDate.now().isAfter(expirationDate);
    }

    public boolean isExpiringSoon(int days) {
        if (expirationDate == null) return false;
        return LocalDate.now().plusDays(days).isAfter(expirationDate);
    }

    public void expire() {
        this.expirationDate = LocalDate.now();
        this.active = false;
    }

    public void renew(LocalDate newExpirationDate) {
        this.expirationDate = newExpirationDate;
        this.active = true;
    }

    /**
     * Check if this skill is active
     */
    public boolean isActive() {
        return active != null && active;
    }

    // Getters and Setters
    public Long getId() { return id; }

    public void setId(Long id) { this.id = id; }

    public EmployeeEntity getEmployee() { return employee; }

    public void setEmployee(EmployeeEntity employee) { this.employee = employee; }

    public String getSkillName() { return skillName; }

    public void setSkillName(String skillName) { this.skillName = skillName; }

    public String getSkillCategory() { return skillCategory; }

    public void setSkillCategory(String skillCategory) { this.skillCategory = skillCategory; }

    public String getLevel() { return level; }

    public void setLevel(String level) { this.level = level; }

    public String getCertificationNumber() { return certificationNumber; }

    public void setCertificationNumber(String certificationNumber) { this.certificationNumber = certificationNumber; }

    public LocalDate getCertificationDate() { return certificationDate; }

    public void setCertificationDate(LocalDate certificationDate) { this.certificationDate = certificationDate; }

    public LocalDate getExpirationDate() { return expirationDate; }

    public void setExpirationDate(LocalDate expirationDate) { this.expirationDate = expirationDate; }

    public String getCertifyingBody() { return certifyingBody; }

    public void setCertifyingBody(String certifyingBody) { this.certifyingBody = certifyingBody; }

    public String getNotes() { return notes; }

    public void setNotes(String notes) { this.notes = notes; }

    public Boolean getActive() { return active; }

    public void setActive(Boolean active) { this.active = active; }

    public LocalDateTime getCreatedAt() { return createdAt; }

    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
