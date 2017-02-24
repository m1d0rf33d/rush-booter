package com.yondu;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.*;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;

import static com.yondu.commons.AppContants.*;

/**
 * Created by erwin on 11/17/2016.
 */
public class App extends Application{

    public static String javaExePath;
    public static String lockFilePath = System.getenv("RUSH_HOME") + WINDOWS_DIVIDER + LOCK_FILE;
    public static String activationPath = System.getenv("RUSH_HOME") + WINDOWS_DIVIDER + ACTIVATION_FILE;
    public static String jarFilePath = System.getenv("RUSH_HOME") + WINDOWS_DIVIDER + JAR_FILE;
    public static String versionFilePath = System.getenv("RUSH_HOME") + WINDOWS_DIVIDER + LOCK_FILE;

    private static boolean is64Bit;


    public static void main(String[] args) {
        try {
            if (System.getProperty("os.name").contains("Windows")) {
                is64Bit = (System.getenv("ProgramFiles(x86)") != null);
                if (is64Bit) {
                    javaExePath = PROGRAM_FILES_32 + WINDOWS_DIVIDER + RUSH_FOLDER + JAVA_EXE;
                } else {
                    javaExePath = PROGRAM_FILES + WINDOWS_DIVIDER + RUSH_FOLDER + JAVA_EXE;
                }


                maximizeRunningApp();

                File lockFile = new File(lockFilePath);
                lockFile.createNewFile();

                File dir = new File(System.getenv("RUSH_HOME"));
                if (!dir.exists()) {
                    dir.mkdir();
                    Path path = FileSystems.getDefault().getPath(dir.getAbsolutePath());
                    Files.setAttribute(path, "dos:hidden", true);
                }

                createBaseJarToUserDir();
                createVersionFile();

                File activateFile = new File(activationPath);
                if (activateFile.exists()) {
                    launch(args);
                } else {
                    lockFile.delete();
                    Runtime.getRuntime().exec(new String[] {javaExePath, "-Dcom.sun.javafx.isEmbedded=true", "-Dcom.sun.javafx.virtualKeyboard=javafx", "-jar", jarFilePath});
                    System.exit(0);
                }
            } else {
                //This is just for me I hate windows

                launch(args);
            }
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }

    }

    private static void createBaseJarToUserDir() {
        try {
            File file = new File(jarFilePath);
            if (!file.exists()) {
                InputStream inStream;
                OutputStream outStream;
                File baseFile;
                if (is64Bit) {
                    baseFile = new File(PROGRAM_FILES_32 + WINDOWS_DIVIDER + RUSH_FOLDER + WINDOWS_DIVIDER + JAR_FILE);

                } else {
                    baseFile = new File(PROGRAM_FILES + WINDOWS_DIVIDER + RUSH_FOLDER + WINDOWS_DIVIDER + JAR_FILE);
                }
                File targetFile = new File (jarFilePath);
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

    private static void maximizeRunningApp() {
        File lockFile = new File(lockFilePath);
        if (lockFile.exists()) {
            try {
                if (is64Bit) {
                    Runtime.getRuntime().exec("cmd /c start  C:\\\"Program Files (x86)\"\\Rush-POS-Sync\\max.vbs");
                } else {
                    Runtime.getRuntime().exec("cmd /c start C:\\\"Program Files\"\\Rush-POS-Sync\\max.vbs");
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            System.exit(0);
        }
    }



    private static void createVersionFile() {
        try {
            File file = new File(versionFilePath);
            if (!file.exists()) {
                InputStream inStream;
                OutputStream outStream;
                File baseFile;
                if (is64Bit) {
                    baseFile = new File(PROGRAM_FILES_32 + WINDOWS_DIVIDER + RUSH_FOLDER + WINDOWS_DIVIDER + VERSION_FILE);
                } else {
                    baseFile = new File(PROGRAM_FILES + WINDOWS_DIVIDER + RUSH_FOLDER + WINDOWS_DIVIDER + VERSION_FILE);
                }
                File targetFile = new File (versionFilePath);
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
    public void start(Stage primaryStage)  {

        try {
            //Let's get the party started
            FXMLLoader fxmlLoader = new FXMLLoader(App.class.getResource(SPLASH_FXML));
            Parent root = fxmlLoader.load();
            primaryStage.setScene(new Scene(root, 500,300));
            primaryStage.resizableProperty().setValue(false);
            primaryStage.initStyle(StageStyle.UNDECORATED);
            primaryStage.getIcons().add(new Image(App.class.getResource(R_LOGO).toExternalForm()));
            primaryStage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }

       /* //Let's get the party started
        String merchant = getActivatedMerchant();
        String version = getVersion();

        ApiResponse apiResponse = AppContextHolder.apiService.checkSoftwareUpdates(merchant, version);
        if (apiResponse.isSuccess()) {
            JSONObject payload = apiResponse.getPayload();
            if (payload.get("fileSize") != null) {
                try {
                    Stage stage = new Stage();
                    FXMLLoader  loader  = new FXMLLoader(App.class.getResource(NOTIFICATION_FXML));
                    NotificationController notificationController = new NotificationController(merchant, dataJSON);
                    loader.setController(notificationController);
                    stage.setScene(new Scene(loader.load(), 400,110));
                    stage.resizableProperty().setValue(Boolean.FALSE);
                    stage.setTitle(APP_TITLE);
                    stage.getIcons().add(new Image(App.class.getResource(R_LOGO).toExternalForm()));
                    stage.show();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                File lockFile = new File(System.getProperty("user.home") + WINDOWS_DIVIDER + RUSH_FOLDER + WINDOWS_DIVIDER + LOCK_FILE);
                lockFile.delete();
                Runtime.getRuntime().exec(new String[] {javaExe, "-Dcom.sun.javafx.isEmbedded=true", "-Dcom.sun.javafx.virtualKeyboard=javafx", "-Dcom.sun.javafx.touch=true", "-jar", System.getProperty("user.home") + AppConstants.JAR_PATH});
                System.exit(0);
            }
        }*/

        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                File file = new File(System.getProperty("user.home") + WINDOWS_DIVIDER + RUSH_FOLDER + WINDOWS_DIVIDER + LOCK_FILE);
                if (file.exists()) {
                    file.delete();
                }
            }
        });

    }


    @Override
    public void stop() throws Exception {
        super.stop();
        File file = new File(System.getProperty("user.home") + WINDOWS_DIVIDER + RUSH_FOLDER + WINDOWS_DIVIDER + LOCK_FILE);
        if (file.exists()) {
            file.delete();
        }
    }
}
