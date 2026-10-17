package lavs.domain;

import java.math.BigDecimal;
import java.time.LocalDate;

// PaymentSummary is a row in a loan's payment list, which doesn't include principal, interest or remaining balance.
public record PaymentSummary(long id, LocalDate date, BigDecimal amountPaid) {
}
