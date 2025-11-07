package com.droid.bss.application.command.invoice;

import com.droid.bss.application.dto.invoice.GenerateInvoiceCommand;
import com.droid.bss.application.dto.invoice.InvoiceDto;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Stub class for GenerateInvoiceUseCase
 * Minimal implementation for testing purposes
 */
@Component
public class GenerateInvoiceUseCase {

    public InvoiceDto handle(GenerateInvoiceCommand command) {
        return new InvoiceDto(
            UUID.randomUUID().toString(),
            "INV-2024-" + System.currentTimeMillis(),
            command.customerId(),
            command.invoiceDate(),
            command.dueDate(),
            null,
            "PLN",
            "PENDING",
            null,
            null,
            null
        );
    }
}
