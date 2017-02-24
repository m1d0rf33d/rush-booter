package com.yondu;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import org.json.simple.JSONObject;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;
import static com.yondu.commons.AppContants.*;
/**
 * Created by erwin on 12/14/2016.
 */
public class NotificationController implements Initializable{
    @FXML
    private Label messageLbl;
    @FXML
    private Button yesBtn;
    @FXML
    private Button noBtn;

    private JSONObject dataJSON;
    private String merchant;
    private static String javaExe = "\"c:\\Program Files (x86)\\Rush-POS-Sync\\jre1.8.0_121\\bin\\java.exe\"";


    public NotificationController(String merchant, JSONObject dataJSON) {
        this.dataJSON = dataJSON;
        this.merchant = merchant;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {

        if (System.getenv("ProgramFiles(x86)") == null) {
            javaExe = "\"c:\\Program Files\\Rush-POS-Sync\\jre1.8.0_121\\bin\\java.exe\"";
        }

        String measure = "mb";
        Long totalBytes = (Long) dataJSON.get("fileSize");
        totalBytes = totalBytes / 1000000; //mb
        if (totalBytes == 0) {
            totalBytes =(Long) dataJSON.get("fileSize") / 1000; //kb
            measure = "kb";
        }
        if (totalBytes == 0) {
            totalBytes =(Long) dataJSON.get("fileSize"); //kb
            measure = "bytes";
        }
        messageLbl.setText("There is a software update available with a total of " + totalBytes + " " + measure + ". Would you like to download it now?");

        yesBtn.setOnMouseClicked(e-> {
            try {
                Stage stage = new Stage();
                FXMLLoader  loader  = new FXMLLoader(App.class.getResource(UPDATE_FXML));
                UpdateController updateController = new UpdateController(merchant, dataJSON);
                loader.setController(updateController);
                stage.setScene(new Scene(loader.load(), 400,200));
                stage.setTitle(APP_TITLE);
                stage.resizableProperty().setValue(Boolean.FALSE);
                stage.getIcons().add(new Image(App.class.getResource(R_LOGO).toExternalForm()));
                stage.show();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
            ((Stage)this.noBtn.getScene().getWindow()).close();
        });


        noBtn.setOnMouseClicked(e-> {
            try {
                File lockFile = new File(System.getProperty("user.home") + LOCK_FILE);
                lockFile.delete();
                //launch app
                Runtime.getRuntime().exec(new String[] {"cd", "/usr/lib/jvm/java-8-oracle/bin", "-C", "./java" ,"-Dcom.sun.javafx.isEmbedded=true", "-Dcom.sun.javafx.virtualKeyboard=javafx", "-Dcom.sun.javafx.touch=true", "-jar", System.getProperty("user.home") + JAR_FILE});
                System.exit(0);
            } catch (IOException e1) {
                e1.printStackTrace();
            }

            ((Stage)this.noBtn.getScene().getWindow()).close();
        });
    }
}
