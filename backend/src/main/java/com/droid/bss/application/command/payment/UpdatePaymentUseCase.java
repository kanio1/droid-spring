package com.droid.bss.application.command.payment;

import com.droid.bss.application.dto.payment.UpdatePaymentCommand;
import com.droid.bss.domain.payment.PaymentEntity;
import com.droid.bss.domain.payment.repository.PaymentRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Component
@Transactional
public class UpdatePaymentUseCase {

    private final PaymentRepository paymentRepository;

    public UpdatePaymentUseCase(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    public PaymentEntity handle(UpdatePaymentCommand command) {
        // Find existing payment
        PaymentEntity payment = paymentRepository.findById(UUID.fromString(command.id()))
                .orElseThrow(() -> new IllegalArgumentException("Payment not found: " + command.id()));

        // Update fields if provided
        if (command.amount() != null) {
            payment.setAmount(command.amount());
        }
        if (command.currency() != null) {
            payment.setCurrency(command.currency());
        }
        if (command.paymentMethod() != null) {
            payment.setPaymentMethod(command.paymentMethod());
        }
        if (command.paymentDate() != null) {
            payment.setPaymentDate(command.paymentDate());
        }
        if (command.receivedDate() != null) {
            payment.setReceivedDate(command.receivedDate());
        }
        if (command.referenceNumber() != null) {
            payment.setReferenceNumber(command.referenceNumber());
        }
        if (command.notes() != null) {
            payment.setNotes(command.notes());
        }

        payment.setUpdatedAt(LocalDateTime.now());

        return paymentRepository.save(payment);
    }
}
