package com.droid.bss.domain.customer;

import java.util.Objects;
import java.util.regex.Pattern;

public record ContactInfo(
    String email,
    String phone
) {
    
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
    );
    private static final Pattern PHONE_PATTERN = Pattern.compile("^\\+?[0-9\\s-]{9,15}$");
    
    public ContactInfo {
        Objects.requireNonNull(email, "Email cannot be null");
        
        if (!EMAIL_PATTERN.matcher(email.trim()).matches()) {
            throw new IllegalArgumentException("Invalid email format");
        }
        
        if (phone != null && !PHONE_PATTERN.matcher(phone.trim()).matches()) {
            throw new IllegalArgumentException("Invalid phone format");
        }
    }
}
