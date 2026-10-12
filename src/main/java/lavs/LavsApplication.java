package lavs;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

public class LavsApplication extends Application {

    @Override
    public void start(Stage stage) {
        StackPane root = new StackPane(new Label("LAVS"));
        Scene scene = new Scene(root, 1000, 700);

        stage.setTitle("Countrywide — Loan Application and Verification System");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}