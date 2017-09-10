package seng302.visualiser.controllers.dialogs;

import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXSlider;
import java.net.URL;
import java.util.ResourceBundle;

import com.jfoenix.controls.JFXTextField;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import seng302.gameServer.ServerDescription;
import seng302.visualiser.controllers.ViewManager;

public class ServerCreationController implements Initializable {

    //--------FXML BEGIN--------//
    @FXML
    private JFXTextField serverName;

    @FXML
    private JFXSlider maxPlayersSlider;
    @FXML
    private Label maxPlayersLabel;

    @FXML
    private JFXButton submitBtn;
    //---------FXML END---------//

    public void initialize(URL location, ResourceBundle resources) {
        updateMaxPlayerLabel();
        maxPlayersSlider.valueProperty().addListener((observable, oldValue, newValue) -> {
            updateMaxPlayerLabel();
        });

        submitBtn.setOnMouseClicked(event -> submitBtn.setText("CREATING..."));
        submitBtn.setOnMouseReleased(event -> createServer());
    }

    public void createServer() {

        ServerDescription serverDescription = ViewManager.getInstance().getGameClient()
            .runAsHost("localhost", 4941, serverName.getText(), (int) maxPlayersSlider
                .getValue());

        ViewManager.getInstance().setProperty("serverName", serverDescription.getName());
        ViewManager.getInstance().setProperty("mapName", serverDescription.getMapName());
    }

    private void updateMaxPlayerLabel() {
        maxPlayersSlider.setValue(Math.floor(maxPlayersSlider.getValue()));
        maxPlayersLabel.setText(String.format("YOU SELECTED: %.0f", maxPlayersSlider.getValue()));
    }
}