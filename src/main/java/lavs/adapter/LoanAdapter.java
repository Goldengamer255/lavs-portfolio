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

public class LoanAdapter implements LoanGateway {
  private final Connection connection;

  public LoanAdapter(Connection connection) {
    this.connection = connection;
  }

  private static LocalDate parseDate(String value) {
    return value == null || value.isBlank() ? null : LocalDate.parse(value);
  }

  private static String formatDate(LocalDate value) {
    return value == null ? null : value.toString();
  }

  private <T> OperationResult<T> send(Message message, Supplier<T> mapResponse) {
    try {
      Status status = message.send(connection);
      if (!status.getWasSuccessful()) {
        return OperationResult.failure(
            status.getErrorCode(), status.getErrorMessage(), status.getTransactionId());
      }
      return OperationResult.success(mapResponse.get(), status.getTransactionId());
    } catch (IOException e) {
      return OperationResult.failure(
          StatusMessage.NETWORK_FAILURE_UNAVAILABLE.getCode(), e.getMessage(), 0);
    }
  }

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

  @Override
  public OperationResult<String> changeStatus(String loanNumber, String newStatus) {
    var message = new mainframe.client.messages.loan.UpdateStatus(loanNumber, newStatus);
    return send(message, message::getStatus);
  }

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

  // Co-borrower operations will be implemented separately.
  @Override
  public OperationResult<Long> addCoborrower(
      String loanNumber, String coborrowerNumber, String type) {
    throw new UnsupportedOperationException("Adding co-borrowers is not implemented");
  }

  @Override
  public OperationResult<Boolean> deleteCoborrower(String loanNumber, long id) {
    throw new UnsupportedOperationException("Deleting co-borrowers is not implemented");
  }

  @Override
  public OperationResult<Page<CoBorrower>> listCoborrowers(
      String loanNumber, long page, long size) {
    throw new UnsupportedOperationException("Listing co-borrowers is not implemented");
  }

  @Override
  public OperationResult<CoBorrower> retrieveCoborrower(String loanNumber, long id) {
    throw new UnsupportedOperationException("Retrieving co-borrowers is not implemented");
  }
}
