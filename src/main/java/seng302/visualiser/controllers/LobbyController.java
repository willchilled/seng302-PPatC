package seng302.visualiser.controllers;

import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXDialog;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Point3D;
import javafx.scene.Group;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Scale;
import javafx.scene.transform.Translate;
import seng302.discoveryServer.DiscoveryServerClient;
import seng302.gameServer.GameStages;
import seng302.gameServer.GameState;
import seng302.model.ClientYacht;
import seng302.model.Colors;
import seng302.model.Limit;
import seng302.model.RaceState;
import seng302.model.mark.CompoundMark;
import seng302.model.mark.Corner;
import seng302.model.stream.xml.parser.RaceXMLData;
import seng302.utilities.Sounds;
import seng302.visualiser.MapPreview;
import seng302.visualiser.controllers.cells.PlayerCell;
import seng302.visualiser.controllers.dialogs.BoatCustomizeController;
import seng302.visualiser.controllers.dialogs.TokenInfoDialogController;
import seng302.visualiser.fxObjects.assets_3D.ModelFactory;
import seng302.visualiser.fxObjects.assets_3D.ModelType;

public class LobbyController implements Initializable {

    private final double INITIAL_MAP_HEIGHT = 770d;
    private final double INITIAL_MAP_WIDTH = 574d;

    //--------FXML BEGIN--------//
    @FXML
    private VBox playerListVBox;
    @FXML
    private ScrollPane playerListScrollPane;
    @FXML
    private JFXButton customizeButton, leaveLobbyButton, beginRaceButton;
    @FXML
    private StackPane serverListMainStackPane;
    @FXML
    private Label serverName;
    @FXML
    private Label mapName;
    @FXML
    private AnchorPane serverMap;
    @FXML
    private Label roomLabel;
    @FXML
    private Label portNumber;
    @FXML
    private Pane speedTokenPane, handlingTokenPane, windWalkerTokenPane, bumperTokenPane, randomTokenPane;
    //---------FXML END---------//

    private RaceState raceState;
    private JFXDialog customizationDialog;
    private JFXDialog tokenInfoDialog;
    public Color playersColor;
    private Map<Integer, ClientYacht> playerBoats;
    private Double mapWidth = INITIAL_MAP_WIDTH, mapHeight = INITIAL_MAP_HEIGHT;
    private MapPreview mapPreview;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        roomLabel.setText("");
        portNumber.setText("");

        this.playerBoats = ViewManager.getInstance().getGameClient().getAllBoatsMap();

        if (this.playersColor == null) {
            this.playersColor = Colors.getColor(ViewManager.getInstance().getGameClient().getServerThread().getClientId() - 1);
        }

        leaveLobbyButton.setOnMouseReleased(event -> leaveLobby());
        beginRaceButton.setOnMouseReleased(event -> beginRace());
        leaveLobbyButton.setOnMouseReleased(event -> {
            Sounds.playButtonClick();
            leaveLobby();
        });

        beginRaceButton.setOnMouseReleased(event -> {
            Sounds.playButtonClick();
            beginRace();
        });

        Platform.runLater(() -> {
            serverName.setText(ViewManager.getInstance().getProperty("serverName"));
            mapName.setText(ViewManager.getInstance().getProperty("mapName"));

            int tries = 0;

            while (DiscoveryServerClient.getRoomCode() == null && tries <= 10){
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                tries ++;
            }

            if (DiscoveryServerClient.getRoomCode() != null){
                setRoomCode(DiscoveryServerClient.getRoomCode());
            }

            ViewManager.getInstance().getPlayerList().addListener((ListChangeListener<String>) c -> Platform.runLater(this::refreshPlayerList));

            ViewManager.getInstance().getPlayerList().setAll(ViewManager.getInstance().getPlayerList().sorted());
        });

        customizeButton.setOnMouseReleased(event -> {
            customizationDialog = createCustomizeDialog();
            Sounds.playButtonClick();
            customizationDialog.show();
        });

        Platform.runLater(() -> {
            Integer playerId = ViewManager.getInstance().getGameClient().getServerThread().getClientId();

            playersColor = Colors.getColor(playerId - 1);
        });

        leaveLobbyButton.setOnMouseEntered(e -> Sounds.playHoverSound());
        customizeButton.setOnMouseEntered(e -> Sounds.playHoverSound());
        beginRaceButton.setOnMouseEntered(e -> Sounds.playHoverSound());

        initMapPreview();
        initTokenPreviews();
    }


    /**
     * Initialises the tokens in the side panel
     */
    private void initTokenPreviews() {
        Group speedToken = ModelFactory.importModel(ModelType.VELOCITY_PICKUP).getAssets();
        Group handlingToken = ModelFactory.importModel(ModelType.HANDLING_PICKUP).getAssets();
        Group windWalkerToken = ModelFactory.importModel(ModelType.WIND_WALKER_PICKUP).getAssets();
        Group bumperToken = ModelFactory.importModel(ModelType.BUMPER_PICKUP).getAssets();
        Group randomToken = ModelFactory.importModel(ModelType.RANDOM_PICKUP).getAssets();

        HashMap<Pane, Group> tokenPanes = new HashMap<>();
        tokenPanes.put(speedTokenPane, speedToken);
        tokenPanes.put(handlingTokenPane, handlingToken);
        tokenPanes.put(windWalkerTokenPane, windWalkerToken);
        tokenPanes.put(bumperTokenPane, bumperToken);
        tokenPanes.put(randomTokenPane, randomToken);

        Scale hoverScale = new Scale(1.2, 1.2, 1.2);

        tokenPanes.entrySet().forEach((entry) -> {
            Pane thisPane = entry.getKey();
            Group thisToken = entry.getValue();

            thisToken.getTransforms().addAll(
                new Translate(40, 50, 0),
                new Scale(13, 13, 13));

            thisPane.setOnMouseEntered(event -> {
                thisToken.getTransforms().add(hoverScale);
            });
            thisPane.setOnMouseExited(event -> {
                thisToken.getTransforms().remove(hoverScale);
            });
            thisPane.setOnMouseReleased(event -> {
                tokenInfoDialog = makeTokenDialog(thisPane);
                tokenInfoDialog.show();
            });

            thisPane.getChildren().add(thisToken);
        });

        //Hacky rotations for wind and random to level it in the plane
        windWalkerToken.getTransforms().addAll(
            new Rotate(-70, new Point3D(1, 0, 0)),
            new Translate(0, 2,0)
        );
        randomToken.getTransforms().addAll(
            new Rotate(-90, new Point3D(1, 0, 0)),
            new Translate(0, 0,1)
        );
    }

    private JFXDialog makeTokenDialog(Pane inducingPane) {
        String header = "...";
        String body = "Nothing to see here";
        ModelType modelType = ModelType.RANDOM_PICKUP;

        if (inducingPane == speedTokenPane) {
            header = "Speed Boost";
            body = "Increases your max velocity";
            modelType = ModelType.VELOCITY_PICKUP;
        } else if (inducingPane == handlingTokenPane) {
            header = "Handling Boost";
            body = "Increases your turing rate";
            modelType = ModelType.HANDLING_PICKUP;
        } else if (inducingPane == windWalkerTokenPane) {
            header = "Wind Walker";
            body = "The wind now rotates with you, giving you your optimal speed in all directions";
            modelType = ModelType.WIND_WALKER_PICKUP;
        } else if (inducingPane == bumperTokenPane) {
            header = "Bumper";
            body = "While this is active, upon hitting another boat, you will power it down for a short time";
            modelType = ModelType.BUMPER_PICKUP;
        } else if (inducingPane == randomTokenPane) {
            header = "Random";
            body = "A 50% chance of becoming any other token and a 50% chance of slowing your boat for a time";
            modelType = ModelType.RANDOM_PICKUP;
        }

        FXMLLoader dialog = new FXMLLoader(
            getClass().getResource("/views/dialogs/TokenInfoDialog.fxml"));

        JFXDialog tokenInfoDialog = null;

        try {
            tokenInfoDialog = new JFXDialog(serverListMainStackPane, dialog.load(),
                JFXDialog.DialogTransition.CENTER);
        } catch (IOException e) {
            e.printStackTrace();
        }

        TokenInfoDialogController controller = dialog.getController();
        controller.setParentController(this);
        controller.setHeader(header);
        controller.setContent(body);
        controller.setToken(modelType);
        return tokenInfoDialog;
    }

    private JFXDialog createCustomizeDialog() {
        FXMLLoader dialog = new FXMLLoader(
                getClass().getResource("/views/dialogs/BoatCustomizeDialog.fxml"));

        JFXDialog customizationDialog = null;

        try {
            customizationDialog = new JFXDialog(serverListMainStackPane, dialog.load(),
                    JFXDialog.DialogTransition.CENTER);
        } catch (IOException e) {
            e.printStackTrace();
        }

        BoatCustomizeController controller = dialog.getController();

        controller.setParentController(this);
        controller.setPlayerColor(this.playersColor);
        controller.setPlayerName(this.playerBoats
            .get(ViewManager.getInstance().getGameClient().getServerThread().getClientId())
            .getBoatName());
        controller.setCurrentBoat(this.playerBoats.get(ViewManager.getInstance().getGameClient().getServerThread().getClientId())
            .getBoatType().toString());

        return customizationDialog;
    }


    /**
     * Initializes a top down preview of the race course map.
     */
    private void initMapPreview() {
        RaceXMLData raceData = ViewManager.getInstance().getGameClient().getCourseData();
        List<Limit> border = raceData.getCourseLimit();
        List<CompoundMark> marks = new ArrayList<>(raceData.getCompoundMarks().values());
        List<Corner> corners = raceData.getMarkSequence();

        mapPreview = new MapPreview(marks, corners, border);
        serverMap.getChildren().clear();
        serverMap.getChildren().add(mapPreview.getAssets());

        mapPreview.setSize(mapWidth, mapHeight);

        serverMap.widthProperty().addListener((observable, oldValue, newValue) -> {
            mapWidth = newValue.doubleValue();
            mapPreview.setSize(mapWidth, mapHeight);
        });
//
        serverMap.heightProperty().addListener((observable, oldValue, newValue) -> {
            mapHeight = newValue.doubleValue();
            mapPreview.setSize(mapWidth, mapHeight);
        });
    }

    /**
     *
     */
    private void beginRace() {
        beginRaceButton.setDisable(true);
        customizeButton.setDisable(true);
        GameState.setCurrentStage(GameStages.PRE_RACE);
        GameState.resetStartTime();
        Platform.runLater(()-> ViewManager.getInstance().getGameClient().startGame());
    }

    /**
     * Refreshes the list of players and their boats, as a series of VBox PlayerCell objects.
     */
    private void refreshPlayerList() {
        playerListVBox.getChildren().clear();
        if (this.playerBoats == null || this.playerBoats.size() == 0) {
            this.playerBoats = ViewManager.getInstance().getGameClient().getAllBoatsMap();
        }
        // TODO: 12/09/2017 ajm412: Make it so that it only removes players who's details have changed.
        for (Integer playerId : playerBoats.keySet()) {
            VBox pane = null;

            ClientYacht yacht = playerBoats.get(playerId);

            FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/views/cells/PlayerCell.fxml"));

            loader.setController(new PlayerCell(playerId, yacht));

            try {
                pane = loader.load();
            } catch (IOException e) {
                e.printStackTrace();
            }

            playerListVBox.getChildren().add(pane);

        }
    }

    private void leaveLobby() {

        ViewManager.getInstance().getGameClient().stopGame();
        ViewManager.getInstance().goToStartView();
    }

    public void disableReadyButton() {
        this.beginRaceButton.setDisable(true);
        this.beginRaceButton.setText("Waiting for host...");
    }

    /**
     * Updates the state of the race and changes the value
     * @param raceState the new race state
     */
    public void updateRaceState(RaceState raceState){
        this.raceState = raceState;
        this.beginRaceButton.setText("Starting in: " + raceState.getRaceTimeStr());
    }

    public void setBoats(Map<Integer, ClientYacht> boats) {
        this.playerBoats = boats;
    }

    public void closeCustomizationDialog() {
        customizationDialog.close();
    }

    public void closeTokenInfoDialog() {
        tokenInfoDialog.close();
    }

    public void setRoomCode(String roomCode) {
        roomLabel.setText("Room: " + roomCode);
    }

    public void setPortNumber(String p){
        portNumber.setText("Port: " + p);
    }
}
