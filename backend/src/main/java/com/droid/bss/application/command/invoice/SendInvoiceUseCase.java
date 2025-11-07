package com.droid.bss.application.command.invoice;

import com.droid.bss.application.dto.invoice.SendInvoiceCommand;
import com.droid.bss.domain.notification.NotificationService;
import org.springframework.stereotype.Component;

/**
 * Stub class for SendInvoiceUseCase
 * Minimal implementation for testing purposes
 */
@Component
public class SendInvoiceUseCase {

    private final NotificationService notificationService;

    public SendInvoiceUseCase(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    public void handle(SendInvoiceCommand command) {
        notificationService.sendInvoice(command.invoiceId(), command.email(), command.message());
    }
}
