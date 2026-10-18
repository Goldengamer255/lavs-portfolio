package lavs.ui;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.TabPane;
import lavs.gateway.DocumentGateway;
import lavs.service.AuthenticationService;
import lavs.service.CategoryService;
import lavs.service.CustomerService;
import lavs.service.DocumentService;
import lavs.service.LoanService;
import lavs.service.NoteService;
import lavs.service.RepaymentService;
import org.junit.jupiter.api.Test;

class CustomerDetailsFxmlTest {
  @Test
  void opensDocumentsTabAlongsideRelationshipsAndNotes() throws Exception {
    var completed = new CountDownLatch(1);
    var failure = new AtomicReference<Throwable>();
    Platform.startup(() -> {
      try {
        var loader = new FXMLLoader(getClass().getResource("/lavs/view/CustomerDetailsView.fxml"));
        loader.setControllerFactory(new ControllerFactory(
            mock(AuthenticationService.class),
            mock(CustomerService.class),
            new DocumentService(mock(DocumentGateway.class)),
            mock(LoanService.class),
            mock(CategoryService.class),
            mock(NoteService.class),
            mock(RepaymentService.class),
            mock(Navigator.class),
            Runnable::run));
        Parent root = loader.load();
        var tabs = (TabPane) root.lookup("#detailsTabs");
        assertNotNull(tabs);
        assertTrue(tabs.getTabs().stream().anyMatch(tab -> "Relationships".equals(tab.getText())));
        assertTrue(tabs.getTabs().stream().anyMatch(tab -> "Notes".equals(tab.getText())));
        var documents = tabs.getTabs().stream()
            .filter(tab -> "Documents".equals(tab.getText()))
            .findFirst().orElseThrow();
        tabs.getSelectionModel().select(documents);
        assertSame(documents, tabs.getSelectionModel().getSelectedItem());
        assertNotNull(documents.getContent());
      } catch (Throwable error) {
        failure.set(error);
      } finally {
        completed.countDown();
      }
    });
    try {
      assertTrue(completed.await(15, TimeUnit.SECONDS), "JavaFX screen did not load");
      if (failure.get() != null) {
        throw new AssertionError("Customer screen failed to open", failure.get());
      }
    } finally {
      Platform.exit();
    }
  }
}
