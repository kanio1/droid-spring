package com.droid.bss.application.query.payment;

import com.droid.bss.application.dto.payment.PaymentDto;
import com.droid.bss.domain.payment.PaymentRepository;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Stub class for GetPaymentByIdUseCase
 * Minimal implementation for testing purposes
 */
@Component
public class GetPaymentByIdUseCase {

    private final PaymentRepository paymentRepository;

    public GetPaymentByIdUseCase(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    public PaymentDto handle(String paymentId) {
        // Stub implementation
        System.out.println("Getting payment by ID: " + paymentId);
        return null;
    }
}
