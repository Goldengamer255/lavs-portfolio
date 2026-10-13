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
     * Changes a link's type; not an atomic mainframe operation. Implementations
     * must surface partial failures and refresh links before offering a retry.
     * The safe add/delete ordering must be agreed with the gateway owner.
     */
    OperationResult<CoBorrower> changeCoBorrowerType(String loanNumber, long id, String type);
}
