package com.droid.bss.application.command.payment;

import com.droid.bss.application.dto.payment.DeletePaymentCommand;
import com.droid.bss.domain.payment.PaymentId;
import com.droid.bss.domain.payment.repository.PaymentRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Use case for deleting payment (soft delete)
 */
@Component
@Transactional
public class DeletePaymentUseCase {

    private final PaymentRepository paymentRepository;

    public DeletePaymentUseCase(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    public void handle(DeletePaymentCommand command) {
        PaymentId paymentId = new PaymentId(UUID.fromString(command.id()));
        paymentRepository.deleteById(paymentId.value());
    }
}
