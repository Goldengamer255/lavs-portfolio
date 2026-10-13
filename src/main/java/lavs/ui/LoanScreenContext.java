package lavs.ui;

/** NEW uses customerNumber with null loanNumber; EXISTING uses loanNumber. */
public record LoanScreenContext(ScreenMode mode, String customerNumber, String loanNumber) {}
