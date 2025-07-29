package application;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.Parent;
import javafx.stage.Stage;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        Parent root = FXMLLoader.load(getClass().getResource("/device-view.fxml"));
        Scene scene = new Scene(root);

        scene.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());
        primaryStage.setTitle("NetDash - Network Devices");
        primaryStage.getIcons().add(new Image("https://cdn-icons-png.flaticon.com/512/2224/2224631.png"));

        primaryStage.setScene(scene);
        primaryStage.setMaximized(true);  // ✅ Maximized on launch
        primaryStage.setResizable(true);  // ✅ Allow window resizing
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
