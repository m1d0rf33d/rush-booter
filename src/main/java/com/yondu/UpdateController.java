package com.yondu;

import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.concurrent.WorkerStateEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.stage.StageStyle;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.json.simple.JSONObject;

import java.io.*;
import java.net.URL;
import java.util.Enumeration;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

/**
 * Created by erwin on 11/17/2016.
 */
public class UpdateController implements Initializable {

    @FXML
    public ProgressBar updateProgressBar;
    @FXML
    public ImageView rushLogoImage;
    @FXML
    public Label downloadLabel;
    @FXML
    private Button givePointsButton;

    private Properties prop = new Properties();
    private ApiService apiService = new ApiService();

    public UpdateController() {
        try {
            InputStream inputStream = getClass().getClassLoader().getResourceAsStream("api.properties");
            if (inputStream != null) {
                prop.load(inputStream);
                inputStream.close();
            } else {
                throw new FileNotFoundException("property file api.properties not found in the classpath");
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {

        String version = this.getVersion();
        String merchant = this.getActivatedMerchant();
        //Check for update
        this.checkForUpdate(merchant, version);

        //Bind button events
        this.givePointsButton.addEventHandler(MouseEvent.MOUSE_CLICKED, (MouseEvent event) -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION, "Are you sure you want to cancel software update?", ButtonType.YES, ButtonType.NO);
            alert.setTitle(AppConstants.APP_TITLE);
            alert.initStyle(StageStyle.UTILITY);
            alert.showAndWait();

            if (alert.getResult() == ButtonType.NO) {
                alert.close();
            } else {
                alert.close();
                exitApp();
            }
        });

        this.rushLogoImage.setImage(new Image(App.class.getResource(AppConstants.RUSH_LOGO).toExternalForm()));
        this.downloadLabel.setText("Downloading update from Rush server..");
    }

    private void checkForUpdate(String merchant, String version) {
        try {
            JSONObject jsonObject = apiService.checkSoftwareUpdates(merchant, version);

            if (jsonObject.get("data") != null) {
                JSONObject dataContent = (JSONObject) jsonObject.get("data");
                Long totalBytes = (Long) dataContent.get("fileSize");
                Alert alert = new Alert(Alert.AlertType.INFORMATION, "There is a software update available with a total of " + (totalBytes / 1000000) + "mb. Would you like to download it now?", ButtonType.YES, ButtonType.NO);
                alert.setTitle(AppConstants.APP_TITLE);
                alert.initStyle(StageStyle.UTILITY);
                alert.showAndWait();

                if (alert.getResult() == ButtonType.NO) {
                    alert.close();
                    File lockFile = new File(System.getProperty("user.home") + AppConstants.LOCK_PATH);
                    lockFile.delete();
                    //launch app
                    Runtime.getRuntime().exec(new String[] {"java", "-Dcom.sun.javafx.isEmbedded=true", "-Dcom.sun.javafx.virtualKeyboard=javafx", "-Dcom.sun.javafx.touch=true", "-jar", System.getProperty("user.home") + AppConstants.JAR_PATH});
                    System.exit(0);
                }
                if (alert.getResult() == ButtonType.YES) {
                    alert.close();
                    MyService myService = new MyService(merchant,totalBytes);
                    updateProgressBar.progressProperty().bind(myService.progressProperty());
                    myService.start();
                    myService.setOnFailed((WorkerStateEvent f) -> {
                        Alert a = new Alert(Alert.AlertType.INFORMATION, "Unable to retrieve update due to network connection timeout.", ButtonType.OK);
                        a.setTitle(AppConstants.APP_TITLE);
                        a.initStyle(StageStyle.UTILITY);
                        a.showAndWait();

                        if (a.getResult() == ButtonType.OK) {
                            this.exitApp();
                        }
                    });
                }
            } else {
                this.exitApp();
            }

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
            this.exitApp();
        }
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


    private void exitApp() {
        try {
            File lockFile = new File(System.getProperty("user.home") + AppConstants.LOCK_PATH);
            lockFile.delete();
            Runtime.getRuntime().exec(new String[] {"java", "-Dcom.sun.javafx.isEmbedded=true", "-Dcom.sun.javafx.virtualKeyboard=javafx", "-Dcom.sun.javafx.touch=true", "-jar", System.getProperty("user.home") + AppConstants.JAR_PATH});
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.exit(0);
    }

    private class MyService extends Service<Void> {
        private String merchantKey;
        private Long totalBytes;

        public MyService(String merchantKey, Long totalBytes) {
            this.totalBytes = totalBytes;
            this.merchantKey = merchantKey;
        }

        @Override
        protected Task<Void> createTask() {
            return new Task<Void>() {
                @Override
                protected Void call() throws Exception {
                    try {
                        //Why am I using deprecated methods man.. :(
                        HttpResponse response;
                        final HttpParams httpParams = new BasicHttpParams();
                        HttpConnectionParams.setConnectionTimeout(httpParams, 20000);
                        HttpConnectionParams.setSoTimeout(httpParams, 20000);
                        CloseableHttpClient httpClient = new DefaultHttpClient(httpParams);

                        String url = prop.getProperty("base_url") + prop.getProperty("tomcat_port") + prop.getProperty("download_updates_api");
                        url = url.replace(":merchant", merchantKey);
                        HttpGet request = new HttpGet(url);
                        request.addHeader("content-type", "application/octet-stream");
                        request.addHeader("Authorization", "Bearer "  + apiService.getToken());
                        response = httpClient.execute(request);

                        InputStream inputStream = response.getEntity().getContent();
                        String location = System.getProperty("user.home") + AppConstants.UPDATE_ZIP;
                        FileOutputStream out = new FileOutputStream(location);
                        int len = 0;
                        byte[] buffer = new byte[2097152];
                        int length = inputStream.available();
                        int total = 0;
                        while((len = inputStream.read(buffer)) != -1) {
                            out.write(buffer, 0, len);
                            total += len;
                            updateProgress(total, totalBytes);
                        }
                        out.flush();
                        out.close();
                        inputStream.close();

                        verifyUpdateFile();

                    } catch (Exception e) {
                        e.printStackTrace();
                        throw new IOException();
                    }
                    return null;
                }
            };
        }
    }

    private void verifyUpdateFile() {
        byte[] buffer = new byte[1024];

        try {

            UnzipUtil unzipUtil = new UnzipUtil();
            unzipUtil.unzip(System.getProperty("user.home") + AppConstants.UPDATE_ZIP, System.getProperty("user.home") + AppConstants.BASE_FOLDER);
            //com, app, lib
            Process p = Runtime.getRuntime().exec(new String[] {"jar", "uf", System.getProperty("user.home") + AppConstants.JAR_PATH, "-C",  System.getProperty("user.home") + AppConstants.BASE_FOLDER, "com/"});
            Runtime.getRuntime().exec(new String[] {"jar", "uf", System.getProperty("user.home") + AppConstants.JAR_PATH, "-C",  System.getProperty("user.home") + AppConstants.BASE_FOLDER, "app/"});
            Runtime.getRuntime().exec(new String[] {"jar", "uf", System.getProperty("user.home") + AppConstants.JAR_PATH, "-C",  System.getProperty("user.home") + AppConstants.BASE_FOLDER, "lib/"});

            while(p.isAlive()) {
               Thread.sleep(2000);
            }
            Runtime.getRuntime().exec(new String[] {"java", "-Dcom.sun.javafx.isEmbedded=true", "-Dcom.sun.javafx.virtualKeyboard=javafx", "-Dcom.sun.javafx.touch=true", "-jar", System.getProperty("user.home") + "\\Rush-POS-Sync\\rush-pos-1.0-SNAPSHOT.jar"});
            System.exit(0);
           /* Thread.sleep(20000);
            File lockFile = new File(System.getProperty("user.home") + AppConstants.LOCK_PATH);
            lockFile.delete();
            Runtime.getRuntime().exec(new String[] {"java", "-Dcom.sun.javafx.isEmbedded=true", "-Dcom.sun.javafx.virtualKeyboard=javafx", "-Dcom.sun.javafx.touch=true", "-jar", System.getProperty("user.home") + AppConstants.JAR_PATH});
            System.exit(0);*/
            /*File oldJar = new File(System.getProperty("user.home") + "\\Rush-POS-Sync\\rush-pos-1.0-SNAPSHOT.jar");
            oldJar.delete();

            File newFile = new File(System.getProperty("user.home") + "\\Rush-POS-Sync\\rush-update.jar");
            File targetFile = new File (System.getProperty("user.home") + "\\Rush-POS-Sync\\rush-pos-1.0-SNAPSHOT.jar");
            InputStream inStream = new FileInputStream(newFile);
            OutputStream outStream = new FileOutputStream(targetFile);
            buffer = new byte[5024];
            int length;
            //copy the file content in bytes
            while ((length = inStream.read(buffer)) > 0){
                outStream.write(buffer, 0, length);
            }
            inStream.close();
            outStream.close();
            File lockFile = new File(System.getProperty("user.home") + "\\Rush-POS-Sync\\lock.txt");
            lockFile.delete();
            Runtime.getRuntime().exec(new String[] {"java", "-Dcom.sun.javafx.isEmbedded=true", "-Dcom.sun.javafx.virtualKeyboard=javafx", "-Dcom.sun.javafx.touch=true", "-jar", System.getProperty("user.home") + "\\Rush-POS-Sync\\rush-pos-1.0-SNAPSHOT.jar"});
            System.exit(0);*/
        } catch(Exception ex) {
            ex.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.INFORMATION, "File is corrupt", ButtonType.OK);
            alert.setTitle(AppConstants.APP_TITLE);
            alert.initStyle(StageStyle.UTILITY);
            alert.showAndWait();

            if (alert.getResult() == ButtonType.OK) {
                alert.close();
            }
        }
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

}
