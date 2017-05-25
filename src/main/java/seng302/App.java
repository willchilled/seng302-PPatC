package seng302;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.stage.Stage;
import seng302.models.PolarTable;
import seng302.models.stream.StreamParser;
import seng302.models.stream.StreamReceiver;
import seng302.server.ServerThread;

public class App extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        PolarTable.parsePolarFile(getClass().getResource("/config/acc_polars.csv").getFile());

        Parent root = FXMLLoader.load(getClass().getResource("/views/MainView.fxml"));
        primaryStage.setTitle("RaceVision");
        primaryStage.setScene(new Scene(root, 1530, 960));
        primaryStage.setMaxWidth(1530);
        primaryStage.setMaxHeight(960);
//        primaryStage.setMaximized(true);

        primaryStage.show();
        primaryStage.setOnCloseRequest(e -> {
            StreamParser.appClose();
            StreamReceiver.noMoreBytes();
            System.exit(0);
        });
    }

    public static void main(String[] args) {
        try {

          StreamReceiver sr = null;

          new ServerThread("Racevision Test Server");

          try {
            Thread.sleep(2000);
          } catch (InterruptedException e) {
            e.printStackTrace();
          }

          if (args.length == 1 && args[0].equals("-standalone")) {
            return;
          }

          if (args.length == 3 && args[0].equals("-server")) {

            sr = new StreamReceiver(args[1], Integer.valueOf(args[2]), "RaceStream");

          } else if (args.length == 2 && args[0].equals("-server")) {
            switch (args[1]) {
              case "internal":
                sr = new StreamReceiver("localhost", 4949, "RaceStream");
                break;
              case "staffserver":
                sr = new StreamReceiver("csse-s302staff.canterbury.ac.nz", 4941, "RaceStream");
                break;
              case "official":
                sr = new StreamReceiver("livedata.americascup.com", 4941, "RaceStream");
                break;
            }
          }
          //Change the StreamReceiver in this else block to change the default data source.
          else {
            sr = new StreamReceiver("livedata.americascup.com", 4940, "RaceStream");
          }

          sr.start();
          StreamParser streamParser = new StreamParser("StreamParser");
          streamParser.start();
        }
        catch (Exception e){
          Alert alert = new Alert(AlertType.INFORMATION);
          alert.setTitle("Information Dialog");
          alert.setHeaderText("Fatal Error");
          alert.setContentText("There was an error connecting to the AC35 stream");

          alert.showAndWait();
        }

        launch(args);

    }
}


