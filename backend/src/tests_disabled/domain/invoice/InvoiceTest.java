package com.droid.bss.domain.invoice;

import com.droid.bss.domain.customer.CustomerId;
import com.droid.bss.domain.order.OrderId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

@DisplayName("Invoice - Aggregate Root Tests")
class InvoiceTest {

    @Test
    @DisplayName("should create invoice with single item")
    void shouldCreateInvoiceWithSingleItem() {
        // Given
        CustomerId customerId = CustomerId.generate();
        OrderId orderId = OrderId.generate();
        InvoiceItem item = createTestItem(orderId, "Service A", 2, new BigDecimal("100.00"));
        String invoiceNumber = "INV-001";

        // When
        Invoice invoice = Invoice.create(invoiceNumber, customerId, item, "ORDER-001");

        // Then
        assertThat(invoice).isNotNull();
        assertThat(invoice.getId()).isNotNull();
        assertThat(invoice.getInvoiceNumber()).isEqualTo(invoiceNumber);
        assertThat(invoice.getCustomerId()).isEqualTo(customerId);
        assertThat(invoice.getStatus()).isEqualTo(InvoiceStatus.DRAFT);
        assertThat(invoice.getItems()).hasSize(1);
        assertThat(invoice.getTotalAmount()).isEqualByComparingTo(new BigDecimal("246.00")); // 2 * 123
    }

    @Test
    @DisplayName("should create invoice with multiple items")
    void shouldCreateInvoiceWithMultipleItems() {
        // Given
        CustomerId customerId = CustomerId.generate();
        OrderId orderId = OrderId.generate();
        InvoiceItem item1 = createTestItem(orderId, "Service A", 1, new BigDecimal("100.00"));
        InvoiceItem item2 = createTestItem(orderId, "Service B", 2, new BigDecimal("50.00"));
        List<InvoiceItem> items = List.of(item1, item2);

        // When
        Invoice invoice = Invoice.create("INV-002", customerId, items, "ORDER-002");

        // Then
        assertThat(invoice.getItems()).hasSize(2);
        assertThat(invoice.getTotalAmount())
            .isEqualByComparingTo(new BigDecimal("123.00").add(new BigDecimal("123.00")));
    }

    @Test
    @DisplayName("should send invoice from draft")
    void shouldSendInvoiceFromDraft() {
        // Given
        Invoice invoice = createTestInvoice();

        // When
        Invoice sent = invoice.send();

        // Then
        assertThat(sent.getStatus()).isEqualTo(InvoiceStatus.SENT);
        assertThat(sent.getVersion()).isEqualTo(2);
    }

    @Test
    @DisplayName("should mark invoice as paid")
    void shouldMarkInvoiceAsPaid() {
        // Given
        Invoice invoice = createTestInvoice().send();

        // When
        Invoice paid = invoice.markAsPaid();

        // Then
        assertThat(paid.getStatus()).isEqualTo(InvoiceStatus.PAID);
        assertThat(paid.getVersion()).isEqualTo(3);
    }

    @Test
    @DisplayName("should cancel draft invoice")
    void shouldCancelDraftInvoice() {
        // Given
        Invoice invoice = createTestInvoice();
        String reason = "Customer request";

        // When
        Invoice cancelled = invoice.cancel(reason);

        // Then
        assertThat(cancelled.getStatus()).isEqualTo(InvoiceStatus.CANCELLED);
        assertThat(cancelled.getNotes()).isEqualTo(reason);
    }

    @Test
    @DisplayName("should cancel sent invoice")
    void shouldCancelSentInvoice() {
        // Given
        Invoice invoice = createTestInvoice().send();

        // When
        Invoice cancelled = invoice.cancel("Error in invoice");

        // Then
        assertThat(cancelled.getStatus()).isEqualTo(InvoiceStatus.CANCELLED);
    }

    @Test
    @DisplayName("should add item to draft invoice")
    void shouldAddItemToDraftInvoice() {
        // Given
        Invoice invoice = createTestInvoice();
        OrderId orderId = OrderId.generate();
        InvoiceItem newItem = createTestItem(orderId, "New Service", 1, new BigDecimal("200.00"));

        // When
        Invoice updated = invoice.addItem(newItem);

        // Then
        assertThat(updated.getItems()).hasSize(2);
        assertThat(updated.getTotalAmount())
            .isGreaterThan(invoice.getTotalAmount());
        assertThat(updated.getVersion()).isEqualTo(2);
    }

    @Test
    @DisplayName("should update item in invoice")
    void shouldUpdateItemInInvoice() {
        // Given
        Invoice invoice = createTestInvoice();
        InvoiceItem originalItem = invoice.getItems().get(0);
        InvoiceItem updatedItem = originalItem.updateQuantity(5);

        // When
        Invoice updated = invoice.updateItem(originalItem.getId(), updatedItem);

        // Then
        assertThat(updated.getItems().get(0).getQuantity()).isEqualTo(5);
        assertThat(updated.getVersion()).isEqualTo(2);
    }

    @Test
    @DisplayName("should check if invoice is draft")
    void shouldCheckIfInvoiceIsDraft() {
        // Given
        Invoice draftInvoice = createTestInvoice();

        // Then
        assertThat(draftInvoice.isDraft()).isTrue();
        assertThat(draftInvoice.isSent()).isFalse();
        assertThat(draftInvoice.isPaid()).isFalse();
    }

    @Test
    @DisplayName("should check if invoice can be modified")
    void shouldCheckIfInvoiceCanBeModified() {
        // Given
        Invoice draftInvoice = createTestInvoice();
        Invoice sentInvoice = draftInvoice.send();

        // Then
        assertThat(draftInvoice.canBeModified()).isTrue();
        assertThat(sentInvoice.canBeModified()).isFalse();
    }

    @Test
    @DisplayName("should check if invoice can be cancelled")
    void shouldCheckIfInvoiceCanBeCancelled() {
        // Given
        Invoice draftInvoice = createTestInvoice();
        Invoice sentInvoice = draftInvoice.send();
        Invoice paidInvoice = sentInvoice.markAsPaid();

        // Then
        assertThat(draftInvoice.canBeCancelled()).isTrue();
        assertThat(sentInvoice.canBeCancelled()).isTrue();
        assertThat(paidInvoice.canBeCancelled()).isFalse();
    }

    @Test
    @DisplayName("should throw exception when sending non-draft invoice")
    void shouldThrowExceptionWhenSendingNonDraftInvoice() {
        // Given
        Invoice invoice = createTestInvoice().send();

        // When & Then
        assertThatThrownBy(() -> invoice.send())
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Only draft invoices can be sent");
    }

    @Test
    @DisplayName("should throw exception for empty items list")
    void shouldThrowExceptionForEmptyItemsList() {
        // Given
        CustomerId customerId = CustomerId.generate();

        // When & Then
        assertThatThrownBy(() -> Invoice.create(
            "INV-EMPTY",
            customerId,
            List.of(),
            "ORDER-EMPTY"
        )).isInstanceOf(IllegalArgumentException.class)
          .hasMessage("Invoice must have at least one item");
    }

    @Test
    @DisplayName("should throw exception when adding item to non-draft invoice")
    void shouldThrowExceptionWhenAddingItemToNonDraftInvoice() {
        // Given
        Invoice invoice = createTestInvoice().send();
        OrderId orderId = OrderId.generate();
        InvoiceItem newItem = createTestItem(orderId, "New Service", 1, new BigDecimal("200.00"));

        // When & Then
        assertThatThrownBy(() -> invoice.addItem(newItem))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Cannot add items to invoice with status: SENT");
    }

    @Test
    @DisplayName("should throw exception for invalid status transition")
    void shouldThrowExceptionForInvalidStatusTransition() {
        // Given
        Invoice invoice = createTestInvoice();

        // When & Then
        assertThatThrownBy(() -> invoice.changeStatus(InvoiceStatus.PAID))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Invalid transition from DRAFT to PAID");
    }

    @Test
    @DisplayName("should throw exception when marking non-sent invoice as paid")
    void shouldThrowExceptionWhenMarkingNonSentInvoiceAsPaid() {
        // Given
        Invoice draftInvoice = createTestInvoice();

        // When & Then
        assertThatThrownBy(() -> draftInvoice.markAsPaid())
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Only sent or viewed invoices can be marked as paid");
    }

    // Helper methods
    private InvoiceItem createTestItem(OrderId orderId, String description, Integer quantity, BigDecimal price) {
        return InvoiceItem.create(
            orderId,
            description,
            quantity,
            price
        );
    }

    private Invoice createTestInvoice() {
        return Invoice.create(
            "TEST-INV-001",
            CustomerId.generate(),
            createTestItem(OrderId.generate(), "Test Service", 1, new BigDecimal("100.00")),
            "TEST-ORDER-001"
        );
    }
}
