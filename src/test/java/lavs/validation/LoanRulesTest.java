package lavs.validation;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class LoanRulesTest {

    private final LoanRules rules = new LoanRules();

    @Test
    public void newCanGoToPendingOrCancelled() {
        // Act
        List<String> result = rules.allowedNextStatuses("New");

        // Assert
        assertEquals(List.of("Pending", "Cancelled"), result);
    }

    @Test
    public void pendingCanGoToActiveCancelledOrNew() {
        // Act
        List<String> result = rules.allowedNextStatuses("Pending");

        // Assert
        assertEquals(List.of("Active", "Cancelled", "New"), result);
    }

    @Test
    public void activeCanOnlyGoToDefaulted() {
        // Act
        List<String> result = rules.allowedNextStatuses("Active");

        // Assert
        assertEquals(List.of("Defaulted"), result);
    }

    @Test
    public void defaultedHasNoNextStatuses() {
        // Act
        List<String> result = rules.allowedNextStatuses("Defaulted");

        // Assert
        assertEquals(List.of(), result);
    }

    @Test
    public void cancelledHasNoNextStatuses() {
        // Act
        List<String> result = rules.allowedNextStatuses("Cancelled");

        // Assert
        assertEquals(List.of(), result);
    }

    @Test
    public void unknownStatusHasNoNextStatuses() {
        // Act
        List<String> result = rules.allowedNextStatuses("Banana");

        // Assert
        assertEquals(List.of(), result);
    }

    @Test
    public void nullStatusHasNoNextStatuses() {
        // Act
        List<String> result = rules.allowedNextStatuses(null);

        // Assert
        assertEquals(List.of(), result);
    }
}
