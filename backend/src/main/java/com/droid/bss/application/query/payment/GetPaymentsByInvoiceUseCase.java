package com.droid.bss.application.query.payment;

import com.droid.bss.application.dto.payment.PaymentDto;
import com.droid.bss.domain.payment.PaymentRepository;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Stub class for GetPaymentsByInvoiceUseCase
 * Minimal implementation for testing purposes
 */
@Component
public class GetPaymentsByInvoiceUseCase {

    private final PaymentRepository paymentRepository;

    public GetPaymentsByInvoiceUseCase(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    public List<PaymentDto> handle(String invoiceId) {
        // Stub implementation
        System.out.println("Getting payments for invoice: " + invoiceId);
        return List.of();
    }
}
