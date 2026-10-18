package lavs.service;

import java.time.LocalDate;

public record PaymentInput (String amount, LocalDate date) {
}
