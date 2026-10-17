package lavs.domain;

import java.math.BigDecimal;
import java.time.LocalDate;

// Payment is one loan payment as retrieved from the mainframe
public record Payment(long id, String loanNumber, LocalDate date, BigDecimal amountPaid, BigDecimal principalAmount, BigDecimal interestAmount, BigDecimal amountRemaining) {
}
