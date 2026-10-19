package lavs.service;

import java.time.LocalDate;

/**
 * Raw payment form values awaiting service validation.
 * The amount remains text so invalid input can be reported before conversion
 * to a decimal amount for persistence.
 */
public record PaymentInput (String amount, LocalDate date) {
}
