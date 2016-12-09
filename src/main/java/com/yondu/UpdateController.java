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
import java.util.ResourceBundle;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

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

    private ApiService apiService = new ApiService();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
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

        File file = new File(System.getProperty("user.home") + "\\Rush-POS-Sync\\rush-pos-1.0-SNAPSHOT.jar");
        long modificationDate = file.lastModified();

        try {
            //Read activated merchant
            String merchant = this.getActivatedMerchant();

            //Check for software updates
            JSONObject jsonObject = apiService.checkSoftwareUpdates(merchant, modificationDate);

            if (jsonObject.get("data") != null) {
                Long totalBytes = (Long) jsonObject.get("data");
                Alert alert = new Alert(Alert.AlertType.INFORMATION, "There is a software update available with a total of " + (totalBytes / 1000000) + "mb. Would you like to download it now?", ButtonType.YES, ButtonType.NO);
                alert.setTitle(AppConstants.APP_TITLE);
                alert.initStyle(StageStyle.UTILITY);
                alert.showAndWait();

                if (alert.getResult() == ButtonType.NO) {
                    alert.close();
                    File lockFile = new File(System.getProperty("user.home") + "\\Rush-POS-Sync\\lock.txt");
                    lockFile.delete();
                    //launch app
                    Runtime.getRuntime().exec(new String[] {"java", "-Dcom.sun.javafx.isEmbedded=true", "-Dcom.sun.javafx.virtualKeyboard=javafx", "-Dcom.sun.javafx.touch=true", "-jar", System.getProperty("user.home") + "\\Rush-POS-Sync\\rush-pos-1.0-SNAPSHOT.jar"});
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

    private void exitApp() {
        try {
            File lockFile = new File(System.getProperty("user.home") + "\\Rush-POS-Sync\\lock.txt");
            lockFile.delete();
            Runtime.getRuntime().exec(new String[] {"java", "-Dcom.sun.javafx.isEmbedded=true", "-Dcom.sun.javafx.virtualKeyboard=javafx", "-Dcom.sun.javafx.touch=true", "-jar", System.getProperty("user.home") + "\\Rush-POS-Sync\\rush-pos-1.0-SNAPSHOT.jar"});
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
                        HttpGet request = new HttpGet("http://52.74.203.202:8080/rush-pos-sync/api/updates/download/" + merchantKey);
                        request.addHeader("content-type", "application/octet-stream");
                        request.addHeader("Authorization", "Bearer "  + apiService.getToken());
                        response = httpClient.execute(request);

                        InputStream inputStream = response.getEntity().getContent();
                        String location = System.getProperty("user.home") + "\\Rush-POS-Sync\\rush-update.jar";
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
        try {
            ZipFile file = new ZipFile(new File(System.getProperty("user.home") + "\\Rush-POS-Sync\\rush-update.jar"));
            Enumeration<? extends ZipEntry> e = file.entries();
            while(e.hasMoreElements()) {
                ZipEntry entry = e.nextElement();
            }

            File oldJar = new File(System.getProperty("user.home") + "\\Rush-POS-Sync\\rush-pos-1.0-SNAPSHOT.jar");
            oldJar.delete();

            File newFile = new File(System.getProperty("user.home") + "\\Rush-POS-Sync\\rush-update.jar");
            File targetFile = new File (System.getProperty("user.home") + "\\Rush-POS-Sync\\rush-pos-1.0-SNAPSHOT.jar");
            InputStream inStream = new FileInputStream(newFile);
            OutputStream outStream = new FileOutputStream(targetFile);
            byte[] buffer = new byte[5024];
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
            System.exit(0);


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
