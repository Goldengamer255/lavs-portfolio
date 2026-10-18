package lavs.service;

import lavs.domain.LoanSummary;
import lavs.domain.Page;
import lavs.domain.RepaymentScheduleItem;
import lavs.gateway.LoanGateway;
import lavs.gateway.OperationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

// AI-assisted: validation checks and gateway delegation structure outlined by AI; code written by me.
@ExtendWith(MockitoExtension.class)
public class RepaymentServiceImplTest {

    @Mock
    private LoanGateway gateway;

    private RepaymentServiceImpl service;

    @BeforeEach
    public void setUp() {
        service = new RepaymentServiceImpl(gateway);
    }

    @Test
    public void getSummaryDelegatesToGateway() {
        // Arrange
        LoanSummary summary = new LoanSummary("L1", "Jane", 0, 0, 0, null, 0);
        OperationResult<LoanSummary> expected = OperationResult.success(summary, 1);
        when(gateway.getSummary("L1")).thenReturn(expected);

        // Act
        OperationResult<LoanSummary> result = service.getSummary("L1");

        // Assert
        assertSame(expected, result);
        verify(gateway).getSummary("L1");
    }

    @Test
    public void getSummaryWithBlankLoanNumberFails() {
        // Act
        OperationResult<LoanSummary> result = service.getSummary("  ");

        // Assert
        assertFalse(result.isSuccess());
        assertEquals(3, result.errorCode());
        verifyNoInteractions(gateway);
    }

    @Test
    public void getSummaryWithNullLoanNumberFails() {
        // Act
        OperationResult<LoanSummary> result = service.getSummary(null);

        // Assert
        assertFalse(result.isSuccess());
        assertEquals(3, result.errorCode());
        verifyNoInteractions(gateway);
    }

    @Test
    public void getSummaryPassesGatewayFailureThrough() {
        // Arrange
        when(gateway.getSummary("L1")).thenReturn(OperationResult.failure(10, "unavailable", 5));

        // Act
        OperationResult<LoanSummary> result = service.getSummary("L1");

        // Assert
        assertTrue(result.isUnavailable());
    }

    @Test
    public void getScheduleDelegatesToGateway() {
        // Arrange
        Page<RepaymentScheduleItem> page = new Page<>(List.of(), 1, 10, 0);
        OperationResult<Page<RepaymentScheduleItem>> expected = OperationResult.success(page, 1);
        when(gateway.getRepaymentSchedule("L1", 1, 10)).thenReturn(expected);

        // Act
        OperationResult<Page<RepaymentScheduleItem>> result = service.getSchedule("L1", 1, 10);

        // Assert
        assertSame(expected, result);
        verify(gateway).getRepaymentSchedule("L1", 1, 10);
    }

    @Test
    public void getScheduleWithBlankLoanNumberFails() {
        // Act
        OperationResult<Page<RepaymentScheduleItem>> result = service.getSchedule("  ", 1, 10);

        // Assert
        assertFalse(result.isSuccess());
        assertEquals(3, result.errorCode());
        verifyNoInteractions(gateway);
    }

    @Test
    public void getScheduleWithPageZeroFails() {
        // Act
        OperationResult<Page<RepaymentScheduleItem>> result = service.getSchedule("L1", 0, 10);

        // Assert
        assertFalse(result.isSuccess());
        assertEquals(3, result.errorCode());
        verifyNoInteractions(gateway);
    }

    @Test
    public void getScheduleWithSizeZeroFails() {
        // Act
        OperationResult<Page<RepaymentScheduleItem>> result = service.getSchedule("L1", 1, 0);

        // Assert
        assertFalse(result.isSuccess());
        assertEquals(3, result.errorCode());
        verifyNoInteractions(gateway);
    }

    @Test
    public void getSchedulePassesGatewayFailureThrough() {
        // Arrange
        when(gateway.getRepaymentSchedule("L1", 1, 10)).thenReturn(OperationResult.failure(10, "unavailable", 5));

        // Act
        OperationResult<Page<RepaymentScheduleItem>> result = service.getSchedule("L1", 1, 10);

        // Assert
        assertTrue(result.isUnavailable());
    }

    @Test
    public void getScheduleWithNullLoanNumberFails() {
        // Act
        OperationResult<Page<RepaymentScheduleItem>> result = service.getSchedule(null, 1, 10);

        // Assert
        assertFalse(result.isSuccess());
        assertEquals(3, result.errorCode());
        verifyNoInteractions(gateway);
    }
}
