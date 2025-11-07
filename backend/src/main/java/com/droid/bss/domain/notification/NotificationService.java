package com.droid.bss.domain.notification;

import org.springframework.stereotype.Service;

/**
 * Stub class for NotificationService
 * Minimal implementation for testing purposes
 */
@Service
public interface NotificationService {

    void sendInvoice(String invoiceId, String email, String message);
}
