package com.yondu;

import com.yondu.commons.AppContants;
import com.yondu.commons.AppContextHolder;
import com.yondu.services.ApiService;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.File;
import java.io.IOException;

import static com.yondu.commons.AppContants.*;

/**
 * Created by erwin on 11/17/2016.
 */
public class App extends Application{

    public static final AppContextHolder appContextHolder = new AppContextHolder();
    private static boolean is64Bit;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage)  {

        AppContants.RUSH_HOME = System.getenv("RUSH_HOME").replace(";", "");

        setFilePaths();
        maximizeRunningApp();
        launchSplashScreen(primaryStage);


        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                File file = new File(AppContants.RUSH_HOME + WINDOWS_DIVIDER +  LOCK_FILE);
                if (file.exists()) {
                    file.delete();
                }
            }
        });

    }
    private void launchSplashScreen(Stage primaryStage){
        try {


            //Let's get the party started
            FXMLLoader fxmlLoader = new FXMLLoader(App.class.getResource(SPLASH_FXML));
            Parent root = fxmlLoader.load();
            primaryStage.setScene(new Scene(root, 500,300));
            primaryStage.resizableProperty().setValue(false);
            primaryStage.initStyle(StageStyle.UNDECORATED);
            primaryStage.setTitle(APP_TITLE);
            primaryStage.getIcons().add(new Image(App.class.getResource(R_LOGO).toExternalForm()));
            primaryStage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void stop() throws Exception {
        super.stop();
        File file = new File(AppContants.RUSH_HOME + WINDOWS_DIVIDER + RUSH_FOLDER + WINDOWS_DIVIDER + LOCK_FILE);
        if (file.exists()) {
            file.delete();
        }
    }

    private static void setFilePaths() {
        appContextHolder.setApiService(new ApiService());

        if (System.getProperty("os.name").contains("Windows")) {
            String programFiles;
            is64Bit = (System.getenv("ProgramFiles(x86)") != null);
            if (is64Bit) {
                programFiles = PROGRAM_FILES_32;

            } else {
                programFiles = PROGRAM_FILES;
            }
            appContextHolder.setExeJarFilePath(programFiles + "\\" + RUSH_FOLDER + "\\" + JRE_FOLDER + "\\" + "jar.exe");
            appContextHolder.setInstallationDir(programFiles + "\\Rush-POS-Sync");
            appContextHolder.setLinux(false);
            appContextHolder.setLockFilePath(RUSH_HOME + "\\" + LOCK_FILE);
            appContextHolder.setActivationPath(RUSH_HOME + "\\" + ACTIVATION_FILE);
            appContextHolder.setJarFilePath(RUSH_HOME + "\\" + JAR_FILE);
            appContextHolder.setVersionFilePath(RUSH_HOME + "\\" + VERSION_FILE);
            appContextHolder.setUpdateFilePath(RUSH_HOME + "\\" + UPDATE_ZIP);
            appContextHolder.setJavaExePath(programFiles + "\\" + RUSH_FOLDER + "\\" + JRE_FOLDER + "\\" + JAVA_EXE);
            appContextHolder.setOcrFilePath(RUSH_HOME + "\\" + OCR_FILE);

        } else {
            appContextHolder.setLinux(true);
            appContextHolder.setLockFilePath(RUSH_HOME + "//" + LOCK_FILE);
            appContextHolder.setActivationPath(RUSH_HOME + "//" + ACTIVATION_FILE);
            appContextHolder.setJarFilePath(RUSH_HOME + "//" + JAR_FILE);
            appContextHolder.setVersionFilePath(RUSH_HOME + "//" + VERSION_FILE);
            appContextHolder.setUpdateFilePath(RUSH_HOME + "//" + UPDATE_ZIP);
            appContextHolder.setJavaExePath("/usr/lib/jvm/java-8-oracle/bin/java");
            appContextHolder.setInstallationDir("/home/lynx/Yondu/programfiles");
            appContextHolder.setOcrFilePath(RUSH_HOME + "//" + OCR_FILE);
        }
    }



    private static void maximizeRunningApp() {
        File lockFile = new File(appContextHolder.getLockFilePath());
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


}
