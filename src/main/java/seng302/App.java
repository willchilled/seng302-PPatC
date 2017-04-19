package seng302;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import seng302.server.ServerThread;

public class App extends Application
{
    @Override
    public void start(Stage primaryStage) throws Exception {
        Parent root = FXMLLoader.load(getClass().getResource("/views/MainView.fxml"));
        primaryStage.setTitle("RaceVision");
        primaryStage.setScene(new Scene(root));

        primaryStage.show();
    }

    public static void main(String[] args) {
        new ServerThread("Racevision Test Server");
        launch(args);
    }
}


