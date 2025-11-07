package com.droid.bss.infrastructure.notification;

import com.droid.bss.domain.notification.NotificationService;
import org.springframework.stereotype.Service;

/**
 * Stub implementation of NotificationService
 * Minimal implementation for testing purposes
 */
@Service
public class SimpleNotificationService implements NotificationService {

    @Override
    public void sendInvoice(String invoiceId, String email, String message) {
        // Stub implementation
        System.out.println("Sending invoice " + invoiceId + " to " + email);
    }
}
