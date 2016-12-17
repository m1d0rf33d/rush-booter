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
import javafx.stage.WindowEvent;
import org.apache.commons.io.FileUtils;
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
    @FXML
    public Label installingLabel;

    private Properties prop = new Properties();
    private ApiService apiService = new ApiService();
    private String merchant;
    private JSONObject dataJSON;

    public UpdateController(String merchant, JSONObject dataJSON) {

        this.merchant = merchant;
        this.dataJSON = dataJSON;
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
        this.installingLabel.setVisible(false);
        this.installingLabel.setText("Installing update this might take a while...");
        this.rushLogoImage.setImage(new Image(App.class.getResource(AppConstants.RUSH_LOGO).toExternalForm()));
        this.downloadLabel.setText("Downloading update from Rush server...");

        String measure = "mb";
        Long totalBytes = (Long) dataJSON.get("fileSize");
        totalBytes = totalBytes / 1000000; //mb
        if (totalBytes == 0) {
            totalBytes =(Long) dataJSON.get("fileSize") / 1000; //kb
            measure = "kb";
        }

        MyService myService = new MyService(merchant, totalBytes, (String) dataJSON.get("version"));
        updateProgressBar.progressProperty().bind(myService.progressProperty());
        myService.start();
        myService.setOnFailed((WorkerStateEvent f) -> {
            Alert a = new Alert(Alert.AlertType.INFORMATION, "Unable to retrieve update due to network connection timeout.", ButtonType.OK);
            a.setTitle(AppConstants.APP_TITLE);
            a.initStyle(StageStyle.UTILITY);
            a.initOwner((givePointsButton.getScene().getWindow()));
            a.showAndWait();

            if (a.getResult() == ButtonType.OK) {
                this.exitApp();
            }
        });
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
        private String version;

        public MyService(String merchantKey, Long totalBytes, String version) {
            this.totalBytes = totalBytes;
            this.merchantKey = merchantKey;
            this.version = version;
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

                        verifyUpdateFile(version);

                    } catch (Exception e) {
                        e.printStackTrace();
                        throw new IOException();
                    }
                    return null;
                }
            };
        }
    }

    private void verifyUpdateFile(String version) {
        byte[] buffer = new byte[1024];

        try {

            UnzipUtil unzipUtil = new UnzipUtil();
            unzipUtil.unzip(System.getProperty("user.home") + AppConstants.UPDATE_ZIP, System.getProperty("user.home") + AppConstants.BASE_FOLDER);

            updateProgressBar.setVisible(false);
            givePointsButton.setVisible(false);
            downloadLabel.setVisible(false);
            rushLogoImage.setVisible(false);
            installingLabel.setVisible(true);

            Process p1 = Runtime.getRuntime().exec(new String[] {"jar", "uf", System.getProperty("user.home") + AppConstants.JAR_PATH, "-C",  System.getProperty("user.home") + AppConstants.BASE_FOLDER, "com/",
                     "-C",  System.getProperty("user.home") + AppConstants.BASE_FOLDER, "app/", "-C",  System.getProperty("user.home") + AppConstants.BASE_FOLDER, "lib/", "-C",  System.getProperty("user.home") + AppConstants.BASE_FOLDER, "api.properties"});
            while(p1.isAlive()) {
                Thread.sleep(500);
            }

            //clean up
            updateVersion(version);
           // deleteTempFiles();

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

    public void deleteTempFiles() {
        try {
            File dir = new File(System.getProperty("user.home") + AppConstants.BASE_FOLDER);
            GenericExtFilter filter = new GenericExtFilter(".tmp");
            String[] tempFiles = dir.list(filter);
            for (String file : tempFiles) {
                String temp = System.getProperty("user.home") + AppConstants.BASE_FOLDER + "\\" + file;
                File f = new File(temp);
                if (f.exists()) {
                    f.delete();
                }
            }
            File updateZip = new File(System.getProperty("user.home") + AppConstants.UPDATE_ZIP);
            if (updateZip.exists()) {
                updateZip.delete();
            }
            File comFile = new File(System.getProperty("user.home") + AppConstants.COM_FOLDER);
            if (comFile.exists()) {
                FileUtils.forceDelete(comFile);
            }
            File appFolder = new File(System.getProperty("user.home") + AppConstants.APP_FOLDER);
            if (appFolder.exists()) {
                FileUtils.forceDelete(appFolder);
            }
            File libFolder = new File(System.getProperty("user.home") + AppConstants.LIB_FOLDER);
            if (libFolder.exists()) {
                FileUtils.forceDelete(libFolder);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void updateVersion(String version) {
       try {
           File file = new File(System.getProperty("user.home") + AppConstants.VERSION_PATH);
           PrintWriter writer = new PrintWriter(file);
           writer.write("version=" + version);
           writer.flush();
           writer.close();
       } catch (FileNotFoundException e) {
           e.printStackTrace();
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

    // inner class, generic extension filter
    public class GenericExtFilter implements FilenameFilter {

        private String ext;

        public GenericExtFilter(String ext) {
            this.ext = ext;
        }

        public boolean accept(File dir, String name) {
            return (name.endsWith(ext));
        }
    }

}
