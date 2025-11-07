package com.droid.bss.application.command.payment;

import com.droid.bss.application.dto.payment.UpdatePaymentCommand;
import com.droid.bss.domain.payment.PaymentEntity;
import com.droid.bss.domain.payment.PaymentId;
import com.droid.bss.domain.payment.repository.PaymentRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Component
@Transactional
public class UpdatePaymentUseCase {

    private final PaymentRepository paymentRepository;

    public UpdatePaymentUseCase(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    public UUID handle(UpdatePaymentCommand command) {
        // Find existing payment
        PaymentId paymentId = new PaymentId(UUID.fromString(command.id()));
        PaymentEntity entity = paymentRepository.findById(paymentId.value())
                .orElseThrow(() -> new IllegalArgumentException("Payment not found: " + command.id()));

        // For immutable aggregate, we need to create a new instance
        // However, update payment is complex - we can only update certain fields
        // Let's use a factory method or return unchanged for immutable fields
        // For now, we'll just return the ID without updating (as Payment is immutable)

        // Note: Payment is designed as immutable, so direct updates are not supported
        // Instead, use ChangePaymentStatusUseCase for status changes
        // or DeletePaymentUseCase and CreatePaymentUseCase for other changes

        throw new UnsupportedOperationException("Payment is immutable. Use DeletePaymentUseCase and CreatePaymentUseCase to replace payment, or ChangePaymentStatusUseCase to change status.");
    }
}
