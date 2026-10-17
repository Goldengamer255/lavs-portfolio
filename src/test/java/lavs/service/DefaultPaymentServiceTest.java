package lavs.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lavs.domain.Page;
import lavs.domain.Payment;
import lavs.domain.PaymentSummary;
import lavs.gateway.OperationResult;
import lavs.gateway.PaymentGateway;
import lavs.validation.PaymentField;
import lavs.validation.ValidationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

// AI-assisted: test structure and cases outlined by AI, following RepaymentServiceImplTest
@ExtendWith(MockitoExtension.class)
public class DefaultPaymentServiceTest {

    private static final LocalDate DATE = LocalDate.of(2026, 10, 17);

    @Mock
    private PaymentGateway gateway;

    private DefaultPaymentService service;

    @BeforeEach
    public void setUp() {
        service = new DefaultPaymentService(gateway, new ValidationService());
    }

    @Test
    public void addValidatesParsesAndCallsGateway() {
        // Arrange
        Payment payment = payment(8);
        when(gateway.addPayment("L1", new BigDecimal("100"), DATE))
                .thenReturn(OperationResult.success(payment, 14));

        // Act
        OperationResult<SaveResult<Payment>> result =
                service.addPayment("L1", new PaymentInput("100", DATE));

        // Assert
        assertTrue(result.isSuccess());
        assertEquals(payment, result.value().value());
        assertTrue(result.value().validation().errors().isEmpty());
        assertEquals(14, result.transactionId());
        verify(gateway).addPayment("L1", new BigDecimal("100"), DATE);
    }

    @Test
    public void invalidPaymentIsNotSentToGateway() {
        // Act
        OperationResult<SaveResult<Payment>> result =
                service.addPayment("L1", new PaymentInput("  ", DATE));

        // Assert
        assertTrue(result.isSuccess());
        assertNull(result.value().value());
        assertEquals(1, result.value().validation().errors().size());
        assertEquals(PaymentField.AMOUNT.key(),
                result.value().validation().errors().get(0).fieldKey());
        verifyNoInteractions(gateway);
    }

    @Test
    public void gatewayFailureIsPassedBack() {
        // Arrange
        when(gateway.addPayment("L1", new BigDecimal("100"), DATE))
                .thenReturn(OperationResult.failure(3, "Invalid payment", 9));

        // Act
        OperationResult<SaveResult<Payment>> result =
                service.addPayment("L1", new PaymentInput("100", DATE));

        // Assert
        assertFalse(result.isSuccess());
        assertEquals(3, result.errorCode());
        assertEquals("Invalid payment", result.errorMessage());
    }

    @Test
    public void listAllPaymentsFetchesEveryPage() {
        // Arrange: 51 payments in total, so two pages of up to 50
        when(gateway.listPayments("L1", 1, 50))
                .thenReturn(OperationResult.success(new Page<>(summaries(50), 1, 50, 51), 1));
        when(gateway.listPayments("L1", 2, 50))
                .thenReturn(OperationResult.success(new Page<>(summaries(1), 2, 50, 51), 2));

        // Act
        OperationResult<List<PaymentSummary>> result = service.listAllPayments("L1");

        // Assert
        assertTrue(result.isSuccess());
        assertEquals(51, result.value().size());
        verify(gateway, times(2)).listPayments(org.mockito.ArgumentMatchers.eq("L1"),
                org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.eq(50L));
    }

    @Test
    public void listAllPaymentsStopsOnFailure() {
        // Arrange
        when(gateway.listPayments("L1", 1, 50))
                .thenReturn(OperationResult.failure(10, "Unavailable", 0));

        // Act
        OperationResult<List<PaymentSummary>> result = service.listAllPayments("L1");

        // Assert
        assertFalse(result.isSuccess());
        assertEquals(10, result.errorCode());
    }

    @Test
    public void getAndDeleteDelegateToGateway() {
        // Arrange
        OperationResult<Payment> found = OperationResult.success(payment(4), 1);
        OperationResult<Boolean> deleted = OperationResult.success(true, 2);
        when(gateway.retrievePayment("L1", 4)).thenReturn(found);
        when(gateway.deletePayment("L1", 4)).thenReturn(deleted);

        // Assert
        assertSame(found, service.getPayment("L1", 4));
        assertSame(deleted, service.deletePayment("L1", 4));
    }

    // Helper Methods Influenced by AI
    private static Payment payment(long id) {
        return new Payment(id, "L1", DATE, BigDecimal.valueOf(100), BigDecimal.valueOf(60),
                BigDecimal.valueOf(40), BigDecimal.valueOf(900));
    }

    private static List<PaymentSummary> summaries(int count) {
        List<PaymentSummary> list = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            list.add(new PaymentSummary(i, DATE, BigDecimal.valueOf(100)));
        }
        return list;
    }
}