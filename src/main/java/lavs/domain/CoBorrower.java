package lavs.domain;

public record CoBorrower(
    long id, 
    String loanNumber, 
    String customerNumber, 
    String customerName, 
    String type
) {}
