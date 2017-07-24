package seng302.controllers;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Observable;
import java.util.Observer;
import java.util.ResourceBundle;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.control.ListView;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.text.Text;
import seng302.client.ClientState;
import seng302.client.ClientStateQueryingRunnable;
import seng302.gameServer.GameStages;
import seng302.gameServer.GameState;

/**
 * A class describing the actions of the lobby screen
 * Created by wmu16 on 10/07/17.
 */
public class LobbyController implements Initializable, Observer{


    @FXML
    private GridPane lobbyScreen;
    @FXML
    private Text lobbyIpText;
    @FXML
    private ListView firstListView;
    @FXML
    private ListView secondListView;
    @FXML
    private ListView thirdListView;
    @FXML
    private ListView fourthListView;
    @FXML
    private ListView fifthListView;
    @FXML
    private ListView sixthListView;
    @FXML
    private ListView seventhListView;
    @FXML
    private ListView eighthListView;
    @FXML
    private ImageView firstImageView;
    @FXML
    private ImageView secondImageView;
    @FXML
    private ImageView thirdImageView;
    @FXML
    private ImageView fourthImageView;
    @FXML
    private ImageView fifthImageView;
    @FXML
    private ImageView sixthImageView;
    @FXML
    private ImageView seventhImageView;
    @FXML
    private ImageView eighthImageView;

    private static List<ObservableList<String>> competitors = new ArrayList<>();
    private static ObservableList<String> firstCompetitor = FXCollections.observableArrayList();
    private static ObservableList<String> secondCompetitor = FXCollections.observableArrayList();
    private static ObservableList<String> thirdCompetitor = FXCollections.observableArrayList();
    private static ObservableList<String> fourthCompetitor = FXCollections.observableArrayList();
    private static ObservableList<String> fifthCompetitor = FXCollections.observableArrayList();
    private static ObservableList<String> sixthCompetitor = FXCollections.observableArrayList();
    private static ObservableList<String> seventhCompetitor = FXCollections.observableArrayList();
    private static ObservableList<String> eighthCompetitor = FXCollections.observableArrayList();
    private ClientStateQueryingRunnable clientStateQueryingRunnable;

    private void setContentPane(String jfxUrl) {
        try {
            AnchorPane contentPane = (AnchorPane) lobbyScreen.getParent();
            contentPane.getChildren().removeAll();
            contentPane.getChildren().clear();
            contentPane.getStylesheets().add(getClass().getResource("/css/master.css").toString());
            contentPane.getChildren()
                    .addAll((Pane) FXMLLoader.load(getClass().getResource(jfxUrl)));
        } catch (javafx.fxml.LoadException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        if (ClientState.isHost())
            lobbyIpText.setText("Lobby Host IP: " + ClientState.getHostIp());
        else
            lobbyIpText.setText("Connected to IP: ");
        initialiseListView();
//        initialiseLobbyControllerThread();
//        initialiseImageView();  // parrot gif init

        // set up client state query thread, so that when it receives the race-started packet
        // it can switch to the race view
        ClientStateQueryingRunnable clientStateQueryingRunnable = new ClientStateQueryingRunnable();
        clientStateQueryingRunnable.addObserver(this);
        Thread clientStateQueryingThread = new Thread(clientStateQueryingRunnable, "Client State querying thread");
        clientStateQueryingThread.setDaemon(true);
        clientStateQueryingThread.start();
    }

    @Override
    public void update(Observable o, Object arg) {
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
//                switchToRaceView();
                initialiseListView();
//                clientStateQueryingRunnable.terminate();
            }
        });
    }

    private void initialiseListView() {
        firstListView.getItems().clear();
        secondListView.getItems().clear();
        thirdListView.getItems().clear();
        fourthListView.getItems().clear();
        fifthListView.getItems().clear();
        sixthListView.getItems().clear();
        seventhListView.getItems().clear();
        eighthListView.getItems().clear();

        competitors = new ArrayList<>();
        Collections.addAll(competitors, firstCompetitor, secondCompetitor, thirdCompetitor,
            fourthCompetitor, fifthCompetitor, sixthCompetitor, seventhCompetitor, eighthCompetitor);

        for (ObservableList<String> ol : competitors) {
            ol.removeAll();
        }

        firstCompetitor.add(ClientState.getClientSourceId());

        int competitorIndex = 1;
        for (Integer yachtId : ClientState.getBoats().keySet()) {
            // break if there are more than 7 competitors
            if (competitorIndex >= 8) {
                break;
            }
            if (!yachtId.equals(Integer.parseInt(ClientState.getClientSourceId()))) {
                competitors.get(competitorIndex).add(String.valueOf(yachtId));
                competitorIndex++;
            }
        }



        firstListView.setItems(firstCompetitor);
        secondListView.setItems(secondCompetitor);
        thirdListView.setItems(thirdCompetitor);
        fourthListView.setItems(fourthCompetitor);
        fifthListView.setItems(fifthCompetitor);
        sixthListView.setItems(sixthCompetitor);
        seventhListView.setItems(seventhCompetitor);
        eighthListView.setItems(eighthCompetitor);
    }

    private void initialiseLobbyControllerThread() {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                Platform.runLater(new Runnable() {
                    @Override
                    public void run() {

                    }
                });
            }
        });
        thread.start();
    }

    private void initialiseImageView() {
        Image image1 = new Image(getClass().getResourceAsStream("/ParrotGif/alistair.gif"));
        firstImageView.setImage(image1);
        Image image2 = new Image(getClass().getResourceAsStream("/ParrotGif/calum.gif"));
        secondImageView.setImage(image2);
        Image image3 = new Image(getClass().getResourceAsStream("/ParrotGif/haoming.gif"));
        thirdImageView.setImage(image3);
        Image image4 = new Image(getClass().getResourceAsStream("/ParrotGif/kusal.gif"));
        fourthImageView.setImage(image4);
        Image image5 = new Image(getClass().getResourceAsStream("/ParrotGif/michael.gif"));
        fifthImageView.setImage(image5);
        Image image6 = new Image(getClass().getResourceAsStream("/ParrotGif/peter.gif"));
        sixthImageView.setImage(image6);
        Image image7 = new Image(getClass().getResourceAsStream("/ParrotGif/ryan.gif"));
        seventhImageView.setImage(image7);
        Image image8 = new Image(getClass().getResourceAsStream("/ParrotGif/will.gif"));
        eighthImageView.setImage(image8);
    }

    @FXML
    public void leaveLobbyButtonPressed() {
        // TODO: 10/07/17 wmu16 - Finish function!
        setContentPane("/views/StartScreenView.fxml");
        GameState.setCurrentStage(GameStages.CANCELLED);
        // TODO: 20/07/17 wmu16 - Implement some way of terminating the game
        ClientState.setConnectedToHost(false);
    }

    @FXML
    public void readyButtonPressed() {
        setContentPane("/views/RaceView.fxml");
        GameState.setCurrentStage(GameStages.RACING);
    }

    private void switchToRaceView() {
        setContentPane("/views/RaceView.fxml");
    }
}
