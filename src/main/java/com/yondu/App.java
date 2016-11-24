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
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;

/** This is a very nice application that's all I can say..
 *
 */
public class App extends Application{

    //Let's get the party started :))
    public static void main(String[] args) {
        try {
            //I only created this for windows..
            if (System.getProperty("os.name").contains("Windows")) {
                boolean is64bit;
                is64bit = (System.getenv("ProgramFiles(x86)") != null);

                //check if there is already a running instance
                File lockFile = new File(System.getProperty("user.home") + AppConstants.LOCK_PATH);
                if (lockFile.exists()) {
                    try {
                        //Maximize the running application then exit app
                        if (is64bit) {
                            Runtime.getRuntime().exec("cmd /c start  C:\\\"Program Files (x86)\"\\Rush-POS-Sync\\max.vbs");
                        } else {
                            Runtime.getRuntime().exec("cmd /c start C:\\\"Program Files\"\\Rush-POS-Sync\\max.vbs");
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    System.exit(0);
                } else {
                    lockFile.createNewFile();

                    //Create initial project setup
                    File dir = new File(System.getProperty("user.home") + "\\Rush-POS-Sync");
                    if (!dir.exists()) {
                        dir.mkdir();
                        Path path = FileSystems.getDefault().getPath(dir.getAbsolutePath());
                        Files.setAttribute(path, "dos:hidden", true);
                    }
                    //Transfer files to User Directory since we cannot write inside Program Files directory unless we are fckng administrator ok?
                    copyRushToUserDirectory(is64bit);

                    //Check if application is already activated
                    File activateFile = new File(System.getProperty("user.home") + AppConstants.ACTIVATION_PATH);
                    if (activateFile.exists()) {
                        //remove previous update copies that are not completed
                        File f1 = new File(System.getProperty("user.home") + AppConstants.RUSH_UPDATE_PATH);
                        if (f1.exists()) {
                            f1.delete();
                        }
                        launch(args);
                    } else {
                        Runtime.getRuntime().exec(new String[] {"java", "-Dcom.sun.javafx.isEmbedded=true", "-Dcom.sun.javafx.virtualKeyboard=javafx", "-Dcom.sun.javafx.touch=true", "-jar", System.getProperty("user.home") + AppConstants.RUSH_JAR_PATH});
                        System.exit(0);
                    }
                }
            }
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    private static void copyRushToUserDirectory(boolean is64bit) throws IOException {
        File file = new File(System.getProperty("user.home") + AppConstants.RUSH_JAR_PATH);
        if (!file.exists()) {
            InputStream inStream;
            OutputStream outStream;
            File baseFile;
            if (is64bit) {
                baseFile = new File(AppConstants.PROGRAM_FILES_X86_PATH + AppConstants.RUSH_JAR_PATH);

            } else {
                baseFile = new File(AppConstants.PROGRAM_FILES_PATH + AppConstants.RUSH_JAR_PATH);
            }
            File targetFile = new File (System.getProperty("user.home") + AppConstants.RUSH_JAR_PATH);
            inStream = new FileInputStream(baseFile);
            outStream = new FileOutputStream(targetFile);
            byte[] buffer = new byte[5242880];
            int length;
            while ((length = inStream.read(buffer)) > 0){
                outStream.write(buffer, 0, length);
            }

            inStream.close();
            outStream.close();
        }
    }

    @Override
    public void start(Stage primaryStage) throws Exception {

        //Add shutdown hook in case User fucks his computer up and caused system reboot or something..
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                //remove lock file
                File file = new File(System.getProperty("user.home") + AppConstants.LOCK_PATH);
                if (file.exists()) {
                    file.delete();
                }
            }
        });

        Parent root = FXMLLoader.load(App.class.getResource("/app/fxml/update.fxml"));
        primaryStage.setScene(new Scene(root, 400,200));
        primaryStage.resizableProperty().setValue(false);
        primaryStage.setTitle(AppConstants.APP_TITLE);
        primaryStage.getIcons().add(new javafx.scene.image.Image(App.class.getResource("/app/images/r_logo.png").toExternalForm()));
        primaryStage.show();
    }

    @Override
    public void stop() throws Exception {
        super.stop();
        File file = new File(System.getProperty("user.home") + AppConstants.LOCK_PATH);
        if (file.exists()) {
            file.delete();
        }
    }
}
