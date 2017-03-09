package com.yondu.controller;

import com.yondu.App;
import com.yondu.commons.AppContants;
import com.yondu.model.ApiResponse;
import javafx.animation.PauseTransition;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URL;
import java.util.ResourceBundle;

import static com.yondu.commons.AppContants.APP_TITLE;
import static com.yondu.commons.AppContants.R_LOGO;
import static com.yondu.commons.AppContants.SPLASH_FXML;

/**
 * Created by erwin on 10/11/2016.
 */
public class ActivationController implements Initializable{

    @FXML
    public Button activateButton;
    @FXML
    public TextField merchantKeyTextField;
    @FXML
    public Button cancelButton;
    @FXML
    public VBox rootVBox;

    @Override
    public void initialize(URL location, ResourceBundle resources) {




        activateButton.addEventHandler(MouseEvent.MOUSE_CLICKED, (MouseEvent event) -> {
            activate();
        });
        cancelButton.addEventFilter(MouseEvent.MOUSE_CLICKED, (MouseEvent event) -> {
            closeApp();
        });
    }

    private void closeApp() {
        ((Stage) cancelButton.getScene().getWindow()).close();
    }

    private void activate() {
        rootVBox.setOpacity(.50);
        for (Node n : rootVBox.getChildren()) {
            n.setDisable(true);
        }

        PauseTransition pause = new PauseTransition(
                Duration.seconds(.5)
        );
        pause.setOnFinished(event -> {
            try {
                String merchantKey = merchantKeyTextField.getText();
                ApiResponse apiResponse = App.appContextHolder.getApiService().activateMerchant(merchantKey);
                if (apiResponse.isSuccess()) {
                    File file = new File(App.appContextHolder.getActivationPath());
                    if (!file.exists()) {
                        file.createNewFile();
                    }
                    PrintWriter writer = new PrintWriter(file);
                    writer.write("merchant=" + merchantKey);
                    writer.flush();
                    writer.close();

                    closeApp();
                    relaunch();

                } else {
                    Text text = new Text("Invalid merchant key");
                    Alert alert = new Alert(Alert.AlertType.INFORMATION, "", ButtonType.OK);
                    alert.setTitle(APP_TITLE);
                    alert.initStyle(StageStyle.UTILITY);
                    alert.initOwner(activateButton.getScene().getWindow());
                    alert.setHeaderText("ACTIVATION");
                    alert.getDialogPane().setPadding(new javafx.geometry.Insets(10,10,10,10));
                    alert.getDialogPane().setContent(text);
                    alert.getDialogPane().setPrefWidth(400);
                    alert.show();
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            rootVBox.setOpacity(1);
            for (Node n : rootVBox.getChildren()) {
                n.setDisable(false);
            }
        });
        pause.play();

    }

    private void relaunch() {
        try {
            Stage stage = new Stage();
            Parent root = FXMLLoader.load(App.class.getResource(AppContants.SPLASH_FXML));
            stage.setScene(new Scene(root, 500,300));
            stage.resizableProperty().setValue(false);
            stage.initStyle(StageStyle.UNDECORATED);
            stage.setTitle(APP_TITLE);
            stage.getIcons().add(new Image(App.class.getResource(R_LOGO).toExternalForm()));
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
