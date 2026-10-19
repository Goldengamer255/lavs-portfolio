package lavs.adapter;

import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.function.Supplier;
import lavs.domain.CoBorrower;
import lavs.domain.Loan;
import lavs.domain.LoanSummary;
import lavs.domain.LoanSummaryItem;
import lavs.domain.NewLoan;
import lavs.domain.Page;
import lavs.domain.RepaymentScheduleItem;
import lavs.gateway.LoanGateway;
import lavs.gateway.OperationResult;
import mainframe.client.Connection;
import mainframe.client.Message;
import mainframe.client.Status;
import mainframe.client.StatusMessage;

/**
 * Maps loan, repayment, and co-borrower gateway operations to mainframe messages.
 *
 * <p>Requests are synchronous and use an externally managed connection. Callers
 * handle business validation and background scheduling. Successful responses are
 * converted to domain records, while failures retain the server's error details
 * and transaction identifier.</p>
 */
public class LoanAdapter implements LoanGateway {
  private final Connection connection;

  /**
   * Creates an adapter using the application's shared mainframe connection.
   *
   * @param connection connection used for loan-related requests; closed by its owner
   */
  public LoanAdapter(Connection connection) {
    this.connection = connection;
  }

  /** Parses an ISO date response, treating null or blank values as absent. */
  private static LocalDate parseDate(String value) {
    return value == null || value.isBlank() ? null : LocalDate.parse(value);
  }

  /** Formats an optional date as ISO text without replacing an absent value. */
  private static String formatDate(LocalDate value) {
    return value == null ? null : value.toString();
  }

  // Ai assisted helper function to send a message and map the response to an OperationResult
  /**
   * Sends a request and reads its populated response fields only on success.
   * Response mapping errors propagate to the caller; only I/O failures are
   * converted to a network-unavailable result.
   *
   * @param message mainframe request and response container
   * @param mapResponse deferred conversion of response fields into a domain value
   * @param <T> gateway result type
   * @return mapped response or mainframe/transport failure details
   */
  private <T> OperationResult<T> send(Message message, Supplier<T> mapResponse) {
    try {
      Status status = message.send(connection);
      if (!status.getWasSuccessful()) {
        return OperationResult.failure(
            status.getErrorCode(), status.getErrorMessage(), status.getTransactionId());
      }
      return OperationResult.success(mapResponse.get(), status.getTransactionId());
    } catch (IOException e) {
      // A transport failure supplies no confirmed server transaction identifier.
      return OperationResult.failure(
          StatusMessage.NETWORK_FAILURE_UNAVAILABLE.getCode(), e.getMessage(), 0);
    }
  }

  /** Creates a loan using the server response for its assigned number and saved terms. */
  @Override
  public OperationResult<Loan> create(NewLoan input) {
    var message =
        new mainframe.client.messages.loan.Create(
            input.customerNumber(),
            input.paymentAmount(),
            input.paymentFrequency(),
            input.principal(),
            input.rateAmount(),
            input.rateFrequency(),
            input.rateType(),
            formatDate(input.renewalDate()),
            formatDate(input.startDate()));
    return send(
        message,
        () ->
            new Loan(
                message.getLoanNumber(),
                message.getCustomerNumber(),
                message.getPrincipal(),
                message.getPaymentAmount(),
                message.getRateAmount(),
                message.getRateType(),
                message.getRateFrequency(),
                message.getPaymentFrequency(),
                parseDate(message.getStartDate()),
                parseDate(message.getRenewalDate()),
                message.getStatus()));
  }

  @Override
  public OperationResult<Loan> retrieve(String loanNumber) {
    var message = new mainframe.client.messages.loan.Retrieve(loanNumber);
    return send(
        message,
        () ->
            new Loan(
                message.getLoanNumber(),
                message.getCustomerNumber(),
                message.getPrincipal(),
                message.getPaymentAmount(),
                message.getRateAmount(),
                message.getRateType(),
                message.getRateFrequency(),
                message.getPaymentFrequency(),
                parseDate(message.getStartDate()),
                parseDate(message.getRenewalDate()),
                message.getStatus()));
  }

  /** Returns the status reported by the mainframe after the requested transition. */
  @Override
  public OperationResult<String> changeStatus(String loanNumber, String newStatus) {
    var message = new mainframe.client.messages.loan.UpdateStatus(loanNumber, newStatus);
    return send(message, message::getStatus);
  }

  /** Retrieves repayment totals supplied by the mainframe without recalculating them locally. */
  @Override
  public OperationResult<LoanSummary> getSummary(String loanNumber) {
    var message = new mainframe.client.messages.loanSummary.Retrieve(loanNumber);
    return send(
        message,
        () ->
            new LoanSummary(
                message.getLoanNumber(),
                message.getCustomerName(),
                message.getTotalAmountPaid(),
                message.getTotalInterestPaid(),
                message.getNumberOfPayments(),
                parseDate(message.getFinalPaymentDate()),
                message.getFinalPaymentAmount()));
  }

  /** Maps updated terms from the response while retaining the supplied loan and customer identifiers. */
  @Override
  public OperationResult<Loan> update(Loan loan) {
    var message =
        new mainframe.client.messages.loan.Update(
            loan.customerNumber(),
            loan.loanNumber(),
            loan.paymentAmount(),
            loan.paymentFrequency(),
            loan.principal(),
            loan.rateAmount(),
            loan.rateFrequency(),
            loan.rateType(),
            formatDate(loan.renewalDate()),
            formatDate(loan.startDate()));
    return send(
        message,
        () ->
            new Loan(
                loan.loanNumber(),
                loan.customerNumber(),
                message.getPrincipal(),
                message.getPaymentAmount(),
                message.getRateAmount(),
                message.getRateType(),
                message.getRateFrequency(),
                message.getPaymentFrequency(),
                parseDate(message.getStartDate()),
                parseDate(message.getRenewalDate()),
                message.getStatus()));
  }

  /**
   * Retrieves a schedule page, preserving the server's page number and total
   * count alongside the requested page size for UI navigation.
   */
  @Override
  public OperationResult<Page<RepaymentScheduleItem>> getRepaymentSchedule(
      String loanNumber, long page, long size) {
    var message = new mainframe.client.messages.loanRepaymentSchedule.List(loanNumber, page, size);
    return send(
        message,
        () -> {
          var items = new ArrayList<RepaymentScheduleItem>();
          for (var item : message.getItems()) {
            items.add(
                new RepaymentScheduleItem(
                    item.getPaymentNumber(),
                    parseDate(item.getDate()),
                    item.getAmountPaid(),
                    item.getPrincipalAmount(),
                    item.getInterestAmount(),
                    item.getAmountRemaining()));
          }
          return new Page<>(items, message.getPage(), size, message.getTotal());
        });
  }

  @Override
  public OperationResult<Page<LoanSummaryItem>> listLoans(
      String customerNumber, long page, long size) {
    var message = new mainframe.client.messages.loan.List(customerNumber, page, size);
    return send(
        message,
        () -> {
          var items = new ArrayList<LoanSummaryItem>();
          // The loan list exposes principal as text; domain summaries store it numerically.
          for (var item : message.getItems()) {
            items.add(
                new LoanSummaryItem(
                    item.getLoanNumber(),
                    Double.parseDouble(item.getPrincipal()),
                    parseDate(item.getStartDate()),
                    item.getStatus()));
          }
          return new Page<>(items, message.getPage(), size, message.getTotal());
        });
  }

  /** Returns the new co-borrower association identifier, not the customer's number. */
  @Override
  public OperationResult<Long> addCoborrower(
      String loanNumber, String coborrowerNumber, String type) {
    var message =
        new mainframe.client.messages.loan.AddCoborrower(coborrowerNumber, loanNumber, type);
    return send(message, message::getId);
  }

  /** Deletes the association; a true result acknowledges a successful mainframe status. */
  @Override
  public OperationResult<Boolean> deleteCoborrower(String loanNumber, long id) {
    var message = new mainframe.client.messages.loan.DeleteCoborrower(id, loanNumber);
    return send(message, () -> true);
  }

  @Override
  public OperationResult<Page<CoBorrower>> listCoborrowers(
      String loanNumber, long page, long size) {
    var message = new mainframe.client.messages.loanCoborrower.List(loanNumber, page, size);
    return send(
        message,
        () -> {
          var items = new ArrayList<CoBorrower>();
          for (var item : message.getItems()) {
            items.add(
                new CoBorrower(
                    item.getId(),
                    message.getLoanNumber(),
                    item.getCoborrowerNumber(),
                    item.getCoborrowerName(),
                    item.getType()));
          }
          return new Page<>(items, message.getPage(), size, message.getTotal());
        });
  }

  @Override
  public OperationResult<CoBorrower> retrieveCoborrower(String loanNumber, long id) {
    var message = new mainframe.client.messages.loanCoborrower.Retrieve(id, loanNumber);
    return send(
        message,
        () ->
            new CoBorrower(
                message.getId(),
                message.getLoanNumber(),
                message.getCoborrowerNumber(),
                message.getCoborrowerName(),
                message.getType()));
  }
}
