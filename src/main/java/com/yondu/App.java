package com.yondu;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;

import java.io.*;

/**
 * Created by erwin on 11/17/2016.
 */
public class App extends Application{

    public static void main(String[] args) {
        try {
            boolean is64bit = false;
            if (System.getProperty("os.name").contains("Windows")) {
                File file = new File(System.getProperty("user.home") + "\\Rush-POS-Sync\\rush-pos-1.0-SNAPSHOT.jar");
                if (!file.exists()) {
                    is64bit = (System.getenv("ProgramFiles(x86)") != null);
                    InputStream inStream = null;
                    OutputStream outStream = null;
                    File baseFile = null;
                    if (is64bit) {
                        baseFile = new File("C:\\Program Files (x86)\\Rush-POS-Sync\\rush-pos-1.0-SNAPSHOT.jar");

                    } else {
                        baseFile = new File("C:\\Program Files\\Rush-POS-Sync\\rush-pos-1.0-SNAPSHOT.jar");
                    }
                    File targetFile = new File (System.getProperty("user.home") + "\\Rush-POS-Sync\\rush-pos-1.0-SNAPSHOT.jar");
                    inStream = new FileInputStream(baseFile);
                    outStream = new FileOutputStream(targetFile);
                    byte[] buffer = new byte[5024];

                    int length;
                    //copy the file content in bytes
                    while ((length = inStream.read(buffer)) > 0){
                        outStream.write(buffer, 0, length);
                    }

                    inStream.close();
                    outStream.close();
                }
            }
            //remove previous update copies
            File file = new File(System.getProperty("user.home") + "\\Rush-POS-Sync\\rush-update.jar");
            if (file.exists()) {
                file.delete();
            }

        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
        launch(args);
      /*  System.exit(0);*/
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        //Let's get the party started
        Parent root = FXMLLoader.load(App.class.getResource("/app/fxml/update.fxml"));
        primaryStage.setScene(new Scene(root, 600,400));
        primaryStage.resizableProperty().setValue(false);
        primaryStage.show();
    }
}
