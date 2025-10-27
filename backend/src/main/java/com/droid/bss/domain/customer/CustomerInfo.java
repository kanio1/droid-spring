package com.droid.bss.domain.customer;

import java.util.Objects;
import java.util.regex.Pattern;

public record CustomerInfo(
    String firstName,
    String lastName,
    String pesel,
    String nip
) {
    
    private static final Pattern NAME_PATTERN = Pattern.compile("^[A-Za-zĄąĆćĘęŁłŃńÓóŚśŹźŻż\\s-]{2,50}$");
    private static final Pattern PESEL_PATTERN = Pattern.compile("^\\d{11}$");
    private static final Pattern NIP_PATTERN = Pattern.compile("^\\d{10}$");
    
    public CustomerInfo {
        Objects.requireNonNull(firstName, "First name cannot be null");
        Objects.requireNonNull(lastName, "Last name cannot be null");
        
        validateName(firstName, "First name");
        validateName(lastName, "Last name");
        
        if (pesel != null && !PESEL_PATTERN.matcher(pesel).matches()) {
            throw new IllegalArgumentException("Invalid PESEL format");
        }
        
        if (nip != null && !NIP_PATTERN.matcher(nip).matches()) {
            throw new IllegalArgumentException("Invalid NIP format");
        }
    }
    
    private static void validateName(String name, String fieldName) {
        if (name.trim().isEmpty()) {
            throw new IllegalArgumentException(fieldName + " cannot be empty");
        }
        if (!NAME_PATTERN.matcher(name.trim()).matches()) {
            throw new IllegalArgumentException(fieldName + " contains invalid characters");
        }
    }
}
