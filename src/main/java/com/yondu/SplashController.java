package com.yondu;

import com.yondu.commons.AppContextHolder;
import com.yondu.model.ApiResponse;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Task;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.text.Text;
import javafx.stage.StageStyle;
import org.json.simple.JSONObject;

import java.io.*;
import java.net.URL;
import java.util.ResourceBundle;
import static com.yondu.commons.AppContants.*;

/**
 * Created by lynx on 2/23/17.
 */
public class SplashController implements Initializable {

    @FXML
    public ProgressBar progressBar;
    @FXML
    public Label taskLabel;
    @FXML
    public ImageView logoImageView;

    private Long fileSize;

    @Override
    public void initialize(URL location, ResourceBundle resources) {

        this.logoImageView.setImage(new Image(App.class.getResource(RUSH_LOGO).toExternalForm()));

        Task copyWorker = createWorker();
        progressBar.progressProperty().unbind();
        progressBar.progressProperty().bind(copyWorker.progressProperty());
        copyWorker.messageProperty().addListener(new ChangeListener<String>() {
            public void changed(ObservableValue<? extends String> observable,
                                String oldValue, String newValue) {
               taskLabel.setText(newValue);
            }
        });
        copyWorker.setOnSucceeded(new EventHandler<WorkerStateEvent>() {
            @Override
            public void handle(WorkerStateEvent event) {
                if (fileSize != null) {
                    String measure = "mb";
                    Long totalBytes = fileSize;
                    totalBytes = totalBytes / 1000000; //mb
                    if (totalBytes == 0) {
                        totalBytes = fileSize / 1000; //kb
                        measure = "kb";
                    }

                    String message = "There is an update available with a total of " + totalBytes + " " + measure;
                    Text text = new Text(message);
                    Alert alert = new Alert(Alert.AlertType.INFORMATION, "", ButtonType.OK);
                    alert.setTitle(APP_TITLE);
                    alert.initStyle(StageStyle.UTILITY);
                    alert.initOwner(taskLabel.getScene().getWindow());
                    alert.setHeaderText("SOFTWARE UPDATE");
                    alert.getDialogPane().setPadding(new javafx.geometry.Insets(10,10,10,10));
                    alert.getDialogPane().setContent(text);
                    alert.getDialogPane().setPrefWidth(400);
                    alert.show();
                } else {
                    try {
                        Runtime.getRuntime().exec("/usr/lib/jvm/java-8-oracle/bin/java -jar /home/lynx/Yondu/Rush-POS-Sync/rush-pos-1.0-SNAPSHOT");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        new Thread(copyWorker).start();
    }

    public Task createWorker() {
        return new Task() {
            @Override
            protected Object call() throws Exception {
                for (int i = 0; i < 3; i++) {

                    updateProgress(i + 1, 3);
                    updateMessage("Check for system configuration..");
                    checkEnvironmentVariables();
                    Thread.sleep(1000);
                    updateProgress(i + 1, 3);

                    updateMessage("Check for updates..");
                    String merchant = getActivatedMerchant();
                    String version = getVersion();
                    ApiResponse apiResponse = AppContextHolder.apiService.checkSoftwareUpdates(merchant, version);
                    if (apiResponse.isSuccess()) {
                        JSONObject payload = apiResponse.getPayload();
                        if (payload.get("fileSize") != null) {
                            fileSize = (Long) payload.get("fileSize");
                        }
                    } else {
                        updateMessage("Network error..");
                    }
                    Thread.sleep(1000);
                    updateProgress(i + 1, 3);
                }
                return true;
            }
        };
    }

    public void checkEnvironmentVariables() {

    }



    public String getActivatedMerchant() {
        try {
            File activation = new File(App.activationPath);
            BufferedReader br = new BufferedReader(new FileReader(activation));
            String l = "";
            String merchant = null;
            while ((l = br.readLine()) != null) {
                String[] arr = l.split("=");
                merchant = arr[1];
            }
            br.close();
            return merchant;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private String getVersion() {
        String version = null;
        try {
            File versionFile = new File(App.versionFilePath);
            //Get merchant key from activation file
            BufferedReader br = new BufferedReader(new FileReader(versionFile));
            String l = "";
            version = null;
            while ((l = br.readLine()) != null) {
                String[] arr = l.split("=");
                version = arr[1];
            }
            br.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return version;
    }

}
