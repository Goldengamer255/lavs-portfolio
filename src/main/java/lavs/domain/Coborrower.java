package lavs.domain;

public record Coborrower(
    long id,
    String loanNumber,
    String coborrowerNumber,
    String coborrowerName,
    String type
) {}
