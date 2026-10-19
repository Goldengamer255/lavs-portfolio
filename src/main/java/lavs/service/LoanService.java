package lavs.service;

import java.util.List;
import lavs.domain.CoBorrower;
import lavs.domain.Loan;
import lavs.domain.Page;
import lavs.gateway.OperationResult;

/** Loan writes validate first; execute gateway calls on the shared worker. */
// AI-assisted: service contract scaffolding for team review.
public interface LoanService {
    OperationResult<Loan> get(String loanNumber);
    OperationResult<SaveResult<Loan>> create(String customerNumber, LoanInput input);
    OperationResult<SaveResult<Loan>> update(String loanNumber, LoanInput input);
    ValidationReport validateLoan(LoanInput input);
    OperationResult<List<String>> allowedNextStatuses(String loanNumber);
    /** Returns the echoed status without re-reading the full record. */
    OperationResult<String> changeStatus(String loanNumber, String status);
    OperationResult<Page<CoBorrower>> listCoBorrowers(String loanNumber, long page, long size);
    OperationResult<CoBorrower> addCoBorrower(String loanNumber, String customerNumber, String type);
    OperationResult<Boolean> removeCoBorrower(String loanNumber, long id);

    /**
     * Changes a co-borrower association's type. The default implementation retrieves
     * the existing link, deletes it, adds a replacement, and retrieves the new link.
     * This sequence is not atomic: a failure may leave the original removed or the
     * replacement created but not retrieved. Callers should refresh links before retrying.
     */
    OperationResult<CoBorrower> changeCoBorrowerType(String loanNumber, long id, String type);
}
