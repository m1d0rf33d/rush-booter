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

/**
 * Created by erwin on 11/17/2016.
 */
public class App extends Application{

    public static void main(String[] args) {
        try {
            boolean is64bit = false;
            if (System.getProperty("os.name").contains("Windows")) {
                is64bit = (System.getenv("ProgramFiles(x86)") != null);
                //check if locked
                File lockFile = new File(System.getProperty("user.home") + AppConstants.LOCK_PATH);
                if (lockFile.exists()) {
                    try {
                        if (is64bit) {
                            Runtime.getRuntime().exec("cmd /c start  C:\\\"Program Files (x86)\"\\Rush-POS-Sync\\max.vbs");
                        } else {
                            Runtime.getRuntime().exec("cmd /c start C:\\\"Program Files\"\\Rush-POS-Sync\\max.vbs");
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    System.exit(0);
                }

                //Create initial project setup
                File dir = new File(System.getProperty("user.home") + AppConstants.BASE_FOLDER);
                if (!dir.exists()) {
                    dir.mkdir();
                    Path path = FileSystems.getDefault().getPath(dir.getAbsolutePath());
                    Files.setAttribute(path, "dos:hidden", true);
                }

                //lock it
                lockFile.createNewFile();
                //transfer base jar to User directory
                createBaseJarToUserDir(is64bit);
                //transfer version txt
                createVersionFile(is64bit);

                //If activated
                File activateFile = new File(System.getProperty("user.home") + AppConstants.ACTIVATION_PATH);
                if (activateFile.exists()) {
                    launch(args);
                } else {
                    //remove lock
                    lockFile.delete();
                    Runtime.getRuntime().exec(new String[] {"java", "-Dcom.sun.javafx.isEmbedded=true", "-Dcom.sun.javafx.virtualKeyboard=javafx", "-Dcom.sun.javafx.touch=true", "-jar", System.getProperty("user.home") + AppConstants.JAR_PATH});
                    System.exit(0);
                }
            }
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    private static void createBaseJarToUserDir(boolean is64bit) {
        try {
            File file = new File(System.getProperty("user.home") + AppConstants.JAR_PATH);
            if (!file.exists()) {
                InputStream inStream;
                OutputStream outStream;
                File baseFile;
                if (is64bit) {
                    baseFile = new File("C:\\Program Files (x86)" + AppConstants.JAR_PATH);

                } else {
                    baseFile = new File("C:\\Program Files" + AppConstants.JAR_PATH);
                }
                File targetFile = new File (System.getProperty("user.home") + AppConstants.JAR_PATH);
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
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }



    private static void createVersionFile(boolean is64bit) {
        try {
            File file = new File(System.getProperty("user.home") + AppConstants.VERSION_PATH);
            if (!file.exists()) {
                InputStream inStream;
                OutputStream outStream;
                File baseFile;
                if (is64bit) {
                    baseFile = new File("C:\\Program Files (x86)" + AppConstants.VERSION_PATH);

                } else {
                    baseFile = new File("C:\\Program Files" + AppConstants.VERSION_PATH);
                }
                File targetFile = new File (System.getProperty("user.home") + AppConstants.VERSION_PATH);
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
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        //lock file
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                File file = new File(System.getProperty("user.home") + AppConstants.LOCK_PATH);
                if (file.exists()) {
                    file.delete();
                }
            }
        });

        //Let's get the party started
        Parent root = FXMLLoader.load(App.class.getResource(AppConstants.UPDATE_FXML));
        primaryStage.setScene(new Scene(root, 400,200));
        primaryStage.resizableProperty().setValue(false);
        primaryStage.setTitle(AppConstants.APP_TITLE);
        primaryStage.getIcons().add(new javafx.scene.image.Image(App.class.getResource(AppConstants.R_LOGO).toExternalForm()));
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
