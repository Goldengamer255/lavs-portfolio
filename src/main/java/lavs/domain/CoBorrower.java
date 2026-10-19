package lavs.domain;

/**
 * Association between a loan and an additional borrowing customer.
 * The {@code id} identifies the association; {@code customerNumber} identifies
 * the customer linked to the loan.
 */
public record CoBorrower(
    long id, 
    String loanNumber, 
    String customerNumber, 
    String customerName, 
    String type
) {}
