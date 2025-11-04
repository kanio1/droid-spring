package com.droid.bss.application.command.payment;

import com.droid.bss.domain.payment.repository.PaymentRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Component
@Transactional
public class DeletePaymentUseCase {

    private final PaymentRepository paymentRepository;

    public DeletePaymentUseCase(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    public boolean handle(String id) {
        UUID paymentId = UUID.fromString(id);

        return paymentRepository.findById(paymentId)
                .map(payment -> {
                    paymentRepository.delete(payment);
                    return true;
                })
                .orElse(false);
    }
}
