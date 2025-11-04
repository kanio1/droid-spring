package com.droid.bss.domain.customer;

import com.droid.bss.domain.common.BaseEntity;
import com.droid.bss.domain.customer.Customer;
import com.droid.bss.domain.customer.CustomerInfo;
import com.droid.bss.domain.customer.ContactInfo;
import com.droid.bss.domain.customer.CustomerStatus;
import com.droid.bss.domain.customer.CustomerId;
import jakarta.persistence.*;

import java.util.UUID;

@Entity
@Table(name = "customers")
public class CustomerEntity extends BaseEntity {

    @Column(name = "first_name", nullable = false)
    private String firstName;

    @Column(name = "last_name", nullable = false)
    private String lastName;

    @Column(name = "pesel", unique = true)
    private String pesel;

    @Column(name = "nip", unique = true)
    private String nip;

    @Column(name = "email", nullable = false)
    private String email;

    @Column(name = "phone")
    private String phone;

    @Enumerated(EnumType.ORDINAL)
    @Column(name = "status", nullable = false)
    private CustomerStatus status;
    
    public CustomerEntity() {}

    public CustomerEntity(
            UUID id,
            String firstName,
            String lastName,
            String pesel,
            String nip,
            String email,
            String phone,
            CustomerStatus status
    ) {
        setId(id);
        this.firstName = firstName;
        this.lastName = lastName;
        this.pesel = pesel;
        this.nip = nip;
        this.email = email;
        this.phone = phone;
        this.status = status;
    }
    
    public static CustomerEntity from(Customer customer) {
        CustomerId customerId = customer.getId();
        CustomerInfo personalInfo = customer.getPersonalInfo();
        ContactInfo contactInfo = customer.getContactInfo();

        return new CustomerEntity(
                customerId.value(),
                personalInfo.firstName(),
                personalInfo.lastName(),
                personalInfo.pesel(),
                personalInfo.nip(),
                contactInfo.email(),
                contactInfo.phone(),
                customer.getStatus()
        );
    }

    public Customer toDomain() {
        CustomerInfo personalInfo = new CustomerInfo(firstName, lastName, pesel, nip);
        ContactInfo contactInfo = new ContactInfo(email, phone);

        // Use package-private factory method
        return Customer.testCustomer(
                new CustomerId(getId()),
                personalInfo,
                contactInfo,
                status,
                getCreatedAt(),
                getVersion().intValue()
        );
    }
    
    // Getters and setters
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getPesel() { return pesel; }
    public void setPesel(String pesel) { this.pesel = pesel; }

    public String getNip() { return nip; }
    public void setNip(String nip) { this.nip = nip; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public CustomerStatus getStatus() { return status; }
    public void setStatus(CustomerStatus status) { this.status = status; }
}
