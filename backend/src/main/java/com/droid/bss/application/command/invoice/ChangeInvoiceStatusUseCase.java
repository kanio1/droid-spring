package com.droid.bss.application.command.invoice;

import com.droid.bss.application.dto.invoice.ChangeInvoiceStatusCommand;
import com.droid.bss.application.dto.invoice.InvoiceResponse;
import com.droid.bss.domain.invoice.InvoiceEntity;
import com.droid.bss.domain.invoice.InvoiceRepository;
import com.droid.bss.domain.invoice.InvoiceStatus;
import com.droid.bss.domain.invoice.event.InvoiceEventPublisher;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

/**
 * Use case for changing invoice status
 */
@Service
@Transactional
public class ChangeInvoiceStatusUseCase {

    private final InvoiceRepository invoiceRepository;
    private final InvoiceEventPublisher eventPublisher;

    public ChangeInvoiceStatusUseCase(InvoiceRepository invoiceRepository, InvoiceEventPublisher eventPublisher) {
        this.invoiceRepository = invoiceRepository;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Execute the change invoice status use case
     */
    public InvoiceResponse execute(@Valid ChangeInvoiceStatusCommand command) {
        // Find invoice by ID
        InvoiceEntity invoice = invoiceRepository.findById(command.id())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Invoice not found with id: " + command.id()
                ));

        // Check version for optimistic locking
        if (!invoice.getVersion().equals(command.version())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Invoice has been modified by another process. Please refresh and try again."
            );
        }

        InvoiceStatus newStatus = command.status();
        InvoiceStatus currentStatus = invoice.getStatus();

        // Validate status transition
        validateStatusTransition(currentStatus, newStatus);

        // Update status
        invoice.setStatus(newStatus);

        // Set additional fields based on status
        if (newStatus == InvoiceStatus.PAID) {
            if (command.paidDate() == null) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Paid date is required when status is PAID"
                );
            }
            invoice.setPaidDate(command.paidDate());
        } else {
            invoice.setPaidDate(null);
        }

        if (newStatus == InvoiceStatus.SENT) {
            if (command.sentToEmail() == null) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Sent to email is required when status is SENT"
                );
            }
            invoice.setSentToEmail(command.sentToEmail());
            invoice.setSentAt(command.sentAt() != null ? command.sentAt() : java.time.LocalDateTime.now());
        } else {
            invoice.setSentToEmail(null);
            invoice.setSentAt(null);
        }

        // Save updated invoice
        InvoiceEntity savedInvoice = invoiceRepository.save(invoice);

        // Publish appropriate event based on new status
        publishStatusChangeEvent(invoice.getStatus(), savedInvoice, currentStatus);

        // Return response
        return InvoiceResponse.from(savedInvoice);
    }

    /**
     * Publish appropriate event based on invoice status
     */
    private void publishStatusChangeEvent(InvoiceStatus newStatus, InvoiceEntity invoice, InvoiceStatus previousStatus) {
        switch (newStatus) {
            case SENT:
                eventPublisher.publishInvoiceSent(invoice);
                break;
            case PAID:
                eventPublisher.publishInvoicePaid(invoice);
                break;
            default:
                eventPublisher.publishInvoiceStatusChanged(invoice, previousStatus);
                break;
        }
    }

    /**
     * Validate that the status transition is allowed
     */
    private void validateStatusTransition(InvoiceStatus currentStatus, InvoiceStatus newStatus) {
        // Define allowed transitions
        switch (currentStatus) {
            case DRAFT:
                if (newStatus != InvoiceStatus.ISSUED && newStatus != InvoiceStatus.CANCELLED) {
                    throw new ResponseStatusException(
                            HttpStatus.CONFLICT,
                            "Invalid status transition from " + currentStatus + " to " + newStatus
                    );
                }
                break;

            case ISSUED:
                if (newStatus != InvoiceStatus.SENT && newStatus != InvoiceStatus.CANCELLED) {
                    throw new ResponseStatusException(
                            HttpStatus.CONFLICT,
                            "Invalid status transition from " + currentStatus + " to " + newStatus
                    );
                }
                break;

            case SENT:
                if (newStatus != InvoiceStatus.PAID && newStatus != InvoiceStatus.OVERDUE && newStatus != InvoiceStatus.CANCELLED) {
                    throw new ResponseStatusException(
                            HttpStatus.CONFLICT,
                            "Invalid status transition from " + currentStatus + " to " + newStatus
                    );
                }
                break;

            case OVERDUE:
                if (newStatus != InvoiceStatus.PAID && newStatus != InvoiceStatus.CANCELLED) {
                    throw new ResponseStatusException(
                            HttpStatus.CONFLICT,
                            "Invalid status transition from " + currentStatus + " to " + newStatus
                    );
                }
                break;

            case PAID:
                // PAID is a final status - no transitions allowed
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Cannot change status from PAID to " + newStatus
                );

            case CANCELLED:
                // CANCELLED is a final status - no transitions allowed
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Cannot change status from CANCELLED to " + newStatus
                );

            default:
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Unknown current status: " + currentStatus
                );
        }
    }
}
