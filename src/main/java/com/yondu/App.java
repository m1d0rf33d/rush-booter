package com.yondu;

import javafx.application.Application;
import javafx.concurrent.WorkerStateEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;
import javafx.stage.WindowEvent;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.json.simple.JSONObject;

import java.io.*;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Created by erwin on 11/17/2016.
 */
public class App extends Application{

    private static String javaExe = "\"c:\\Program Files (x86)\\Rush-POS-Sync\\jre1.8.0_121\\bin\\java.exe\"";
    private static boolean is64Bit;

    public static void main(String[] args) {
        try {
            if (System.getProperty("os.name").contains("Windows")) {
                is64Bit = (System.getenv("ProgramFiles(x86)") != null);
                //check if locked
                File lockFile = new File(System.getProperty("user.home") + AppConstants.LOCK_PATH);
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
                createBaseJarToUserDir(is64Bit);
                //transfer version txt
                createVersionFile(is64Bit);

                //If activated
                File activateFile = new File(System.getProperty("user.home") + AppConstants.ACTIVATION_PATH);
                if (activateFile.exists()) {
                    launch(args);
                } else {
                    //remove lock
                    lockFile.delete();
                    if (!is64Bit) {
                        javaExe = "\"c:\\Program Files\\Rush-POS-Sync\\jre1.8.0_121\\bin\\java.exe\"";
                    }
                    Runtime.getRuntime().exec(new String[] {javaExe, "-Dcom.sun.javafx.isEmbedded=true", "-Dcom.sun.javafx.virtualKeyboard=javafx", "-Dcom.sun.javafx.touch=true", "-jar", System.getProperty("user.home") + AppConstants.JAR_PATH});
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
    public void start(Stage primaryStage)  {
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

        try {
            //Let's get the party started
            String merchant = getActivatedMerchant();
            //Check for updates
            JSONObject jsonObj = this.checkForUpdate(merchant, getVersion());
            JSONObject dataJSON = (JSONObject) jsonObj.get("data");
            if (dataJSON.get("fileSize") != null) {
                Stage stage = new Stage();
                FXMLLoader  loader  = new FXMLLoader(App.class.getResource(AppConstants.NOTIFICATION_FXML));
                NotificationController notificationController = new NotificationController(merchant, dataJSON);
                loader.setController(notificationController);
                stage.setScene(new Scene(loader.load(), 400,110));
                stage.resizableProperty().setValue(Boolean.FALSE);
                stage.setTitle(AppConstants.APP_TITLE);
                stage.getIcons().add(new Image(App.class.getResource(AppConstants.R_LOGO).toExternalForm()));
                stage.show();
            } else {
                File lockFile = new File(System.getProperty("user.home") + AppConstants.LOCK_PATH);
                lockFile.delete();
                //launch app
                if (!is64Bit) {
                    javaExe = "\"c:\\Program Files\\Rush-POS-Sync\\jre1.8.0_121\\bin\\java.exe\"";
                }
                Runtime.getRuntime().exec(new String[] {javaExe, "-Dcom.sun.javafx.isEmbedded=true", "-Dcom.sun.javafx.virtualKeyboard=javafx", "-Dcom.sun.javafx.touch=true", "-jar", System.getProperty("user.home") + AppConstants.JAR_PATH});
                System.exit(0);
            }
        } catch (IOException e) {
            File lockFile = new File(System.getProperty("user.home") + AppConstants.LOCK_PATH);
            lockFile.delete();
            //launch app
            try {
                if (!is64Bit) {
                    javaExe = "\"c:\\Program Files\\Rush-POS-Sync\\jre1.8.0_121\\bin\\java.exe\"";
                }
                Runtime.getRuntime().exec(new String[] {javaExe, "-Dcom.sun.javafx.isEmbedded=true", "-Dcom.sun.javafx.virtualKeyboard=javafx", "-Dcom.sun.javafx.touch=true", "-jar", System.getProperty("user.home") + AppConstants.JAR_PATH});
            } catch (IOException e1) {
                e1.printStackTrace();
            }
            System.exit(0);
        }


    }


    private JSONObject checkForUpdate(String merchant, String version) throws IOException {
        ApiService apiService = new ApiService();
        return apiService.checkSoftwareUpdates(merchant, version);
    }
    public String getActivatedMerchant() {
        try {
            File activation = new File(System.getProperty("user.home") + "\\Rush-POS-Sync\\activation.txt");
            BufferedReader br = new BufferedReader(new FileReader(activation));
            String l = "";
            String merchant = null;
            while ((l = br.readLine()) != null) {
                String[] arr = l.split("=");
                merchant = arr[1];
            }
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
            File versionFile = new File(System.getProperty("user.home") + AppConstants.VERSION_PATH);
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

    @Override
    public void stop() throws Exception {
        super.stop();
        File file = new File(System.getProperty("user.home") + AppConstants.LOCK_PATH);
        if (file.exists()) {
            file.delete();
        }
    }
}
