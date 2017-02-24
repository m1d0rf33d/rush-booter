package com.yondu.controller;

import com.yondu.App;
import com.yondu.commons.UnzipUtil;
import com.yondu.model.ApiResponse;
import com.yondu.services.SplashService;
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
import org.apache.commons.io.FileUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;

import java.io.*;
import java.net.URL;
import java.util.Properties;
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
    private String merchantKey;
    private Properties properties = new Properties();
    private SplashService splashService = new SplashService();

    public SplashController() {
        try {
            InputStream inputStream = getClass().getClassLoader().getResourceAsStream("api.properties");
            if (inputStream != null) {
                properties.load(inputStream);
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

        this.logoImageView.setImage(new Image(App.class.getResource(RUSH_LOGO).toExternalForm()));

        Task phaseOneWorker = PhaseOneWorker();
        progressBar.progressProperty().unbind();
        progressBar.progressProperty().bind(phaseOneWorker.progressProperty());
        phaseOneWorker.messageProperty().addListener(new ChangeListener<String>() {
            public void changed(ObservableValue<? extends String> observable,
                                String oldValue, String newValue) {
               taskLabel.setText(newValue);
            }
        });
        phaseOneWorker.setOnSucceeded(new EventHandler<WorkerStateEvent>() {
            @Override
            public void handle(WorkerStateEvent event) {
                goToSecondPhase();
            }
        });
        new Thread(phaseOneWorker).start();
    }

    public Task PhaseOneWorker() {
        return new Task() {
            @Override
            protected Object call() throws Exception {
                updateProgress(1, 3);
                updateMessage("Preparing application configuration");

                if (!App.appContextHolder.isLinux()) {
                    String divider = App.appContextHolder.isLinux() ? "//" : "\\";
                    File rushHome = new File(System.getenv("RUSH_HOME"));
                    if (!rushHome.exists()) {
                        rushHome.mkdir();
                    }
                    File versionFile = new File(App.appContextHolder.getVersionFilePath());
                    if (!versionFile.exists()) {

                        File fileFrom = new File(App.appContextHolder.getInstallationDir() + divider + VERSION_FILE);
                        splashService.transferFile(fileFrom, versionFile);
                    }

                    File ocrPropertiesFile = new File(App.appContextHolder.getOcrFilePath());
                    if (!ocrPropertiesFile.exists()) {
                        File fileFrom = new File(App.appContextHolder.getInstallationDir() + divider + OCR_FILE);
                        splashService.transferFile(fileFrom, ocrPropertiesFile);
                    }

                    File offlineTxt = new File(rushHome + divider + "offline.txt");
                    if (!offlineTxt.exists()) {
                        offlineTxt.createNewFile();
                    }

                    File rushJar = new File(App.appContextHolder.getJarFilePath());
                    if (!rushJar.exists()) {
                        File fileFrom = new File(App.appContextHolder.getInstallationDir() + divider + JAR_FILE);
                        splashService.transferFile(fileFrom, rushJar);
                    }
                }

                Thread.sleep(1000);
                return true;
            }
        };
    }

    public void goToSecondPhase() {
        Task phaseTwoWorker = PhaseTwoWorker();
        progressBar.progressProperty().unbind();
        progressBar.progressProperty().bind(phaseTwoWorker.progressProperty());
        phaseTwoWorker.messageProperty().addListener(new ChangeListener<String>() {
            public void changed(ObservableValue<? extends String> observable,
                                String oldValue, String newValue) {
                taskLabel.setText(newValue);
            }
        });
        phaseTwoWorker.setOnSucceeded(new EventHandler<WorkerStateEvent>() {

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
                    Alert alert = new Alert(Alert.AlertType.INFORMATION, "", ButtonType.YES, ButtonType.NO);
                    alert.setTitle(APP_TITLE);
                    alert.initStyle(StageStyle.UTILITY);
                    alert.initOwner(taskLabel.getScene().getWindow());
                    alert.setHeaderText("SOFTWARE UPDATE");
                    alert.getDialogPane().setPadding(new javafx.geometry.Insets(10,10,10,10));
                    alert.getDialogPane().setContent(text);
                    alert.getDialogPane().setPrefWidth(400);
                    alert.showAndWait();

                    if (alert.getResult() == ButtonType.NO) {
                        launchRushPOS();
                    } else {
                        startDownloadWorker();
                    }
                } else {
                    launchRushPOS();
                }
            }
        });
        new Thread(phaseTwoWorker).start();
    }



    public Task PhaseTwoWorker() {
        return new Task() {
            @Override
            protected Object call() throws Exception {
                updateProgress(2, 3);
                updateMessage("Checking for software updates..");
                merchantKey = getActivatedMerchant();
                String version = getVersion();
                ApiResponse apiResponse = App.appContextHolder.getApiService().checkSoftwareUpdates(merchantKey, version);
                if (apiResponse.isSuccess()) {
                    JSONObject payload = apiResponse.getPayload();
                    JSONObject data = (JSONObject) payload.get("data");
                    if (data.get("fileSize") != null) {
                        fileSize = (Long) data.get("fileSize");
                    }
                } else {
                    updateProgress(3, 3);
                    updateMessage("Unable to fetch updates. Launching application..");
                    Thread.sleep(1000);
                }
                return true;
            }
        };
    }

    private void startDownloadWorker() {
        Task downloadWorker = DownloadWorker(merchantKey);
        progressBar.progressProperty().unbind();
        progressBar.progressProperty().bind(downloadWorker.progressProperty());
        downloadWorker.messageProperty().addListener(new ChangeListener<String>() {
            public void changed(ObservableValue<? extends String> observable,
                                String oldValue, String newValue) {
                taskLabel.setText(newValue);
            }
        });
        downloadWorker.setOnSucceeded(new EventHandler<WorkerStateEvent>() {
            @Override
            public void handle(WorkerStateEvent event) {
                startInstallationWorker();
            }
        });
        new Thread(downloadWorker).start();
    }
    private void startInstallationWorker() {
        Task installWorker = InstallationWorker();
        progressBar.progressProperty().unbind();
        progressBar.progressProperty().bind(installWorker.progressProperty());
        installWorker.messageProperty().addListener(new ChangeListener<String>() {
            public void changed(ObservableValue<? extends String> observable,
                                String oldValue, String newValue) {
                taskLabel.setText(newValue);
            }
        });
        installWorker.setOnSucceeded(new EventHandler<WorkerStateEvent>() {
            @Override
            public void handle(WorkerStateEvent event) {
                launchRushPOS();
            }
        });
        new Thread(installWorker).start();
    }

    public Task InstallationWorker() {
        return new Task() {
            @Override
            protected Object call() throws Exception {
                updateProgress(0,3);
                updateMessage("Installing update..");
                byte[] buffer = new byte[1024];

                try {
                    UnzipUtil unzipUtil = new UnzipUtil();
                    unzipUtil.unzip(App.appContextHolder.getUpdateFilePath(), System.getenv("RUSH_HOME"));
                    String rushHome = System.getenv("RUSH_HOME");
                    Process p1 = Runtime.getRuntime().exec(new String[] {App.appContextHolder.getJavaExePath(), "uf", App.appContextHolder.getJarFilePath(), "-C",  rushHome, "com/",
                            "-C", rushHome, "app/", "-C",  rushHome, "lib/", "-C",  rushHome, "api.properties"});
                    updateProgress(1,3);
                    while(p1.isAlive()) {
                        Thread.sleep(500);
                    }

                    //clean up
                    //updateVersion(version);
                    deleteTempFiles();
                    updateProgress(3,3);
                    updateMessage("Installation complete. Launching application..");
                    Thread.sleep(1000);
                } catch(Exception ex) {
                    ex.printStackTrace();
                    Alert alert = new Alert(Alert.AlertType.INFORMATION, "File is corrupt", ButtonType.OK);
                    alert.setTitle(APP_TITLE);
                    alert.initStyle(StageStyle.UTILITY);
                    alert.showAndWait();

                    if (alert.getResult() == ButtonType.OK) {
                        alert.close();
                    }
                }
                return true;
            }
        };
    }

    public void deleteTempFiles() {
        try {
            File rushHome = new File(System.getenv("RUSH_HOME"));
            GenericExtFilter filter = new GenericExtFilter(".tmp");
            String[] tempFiles = rushHome.list(filter);
            String divider = App.appContextHolder.isLinux() ? "//" : "\\";
            for (String file : tempFiles) {
                String temp = System.getenv("RUSH_HOME")+ divider + file;
                File f = new File(temp);
                if (f.exists()) {
                    f.delete();
                }
            }
            File updateZip = new File(rushHome + "//update.zip");
            if (updateZip.exists()) {
                updateZip.delete();
            }
            File comFile = new File(rushHome+ "//com");
            if (comFile.exists()) {
                FileUtils.forceDelete(comFile);
            }
            File appFolder = new File(rushHome + "//app");
            if (appFolder.exists()) {
                FileUtils.forceDelete(appFolder);
            }
            File libFolder = new File(rushHome + "//lib");
            if (libFolder.exists()) {
                FileUtils.forceDelete(libFolder);
            }
            File apiProperties = new File(rushHome + "//api.properties");
            if (apiProperties.exists()) {
                apiProperties.delete();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Task DownloadWorker(String merchantKey) {
        return new Task() {
            @Override
            protected Object call() throws Exception {
                try {
                    updateMessage("Downloading..");
                    updateProgress(0, fileSize);
                    HttpResponse response;
                    final HttpParams httpParams = new BasicHttpParams();
                    HttpConnectionParams.setConnectionTimeout(httpParams, 20000);
                    HttpConnectionParams.setSoTimeout(httpParams, 20000);
                    CloseableHttpClient httpClient = new DefaultHttpClient(httpParams);

                    String url = properties.get("base_url") + properties.getProperty("tomcat_port") + properties.getProperty("download_updates_api");
                    url = url.replace(":merchant", merchantKey);
                    HttpGet request = new HttpGet(url);
                    request.addHeader("content-type", "application/octet-stream");
                    request.addHeader("Authorization", "Bearer "  + App.appContextHolder.getApiService().getToken());
                    response = httpClient.execute(request);

                    InputStream inputStream = response.getEntity().getContent();
                    FileOutputStream out = new FileOutputStream(App.appContextHolder.getUpdateFilePath());
                    int len = 0;
                    byte[] buffer = new byte[2097152];
                    int total = 0;
                    while((len = inputStream.read(buffer)) != -1) {
                        out.write(buffer, 0, len);
                        total += len;
                        updateProgress(total, fileSize);
                    }
                    out.flush();
                    out.close();
                } catch (ParseException e) {
                    e.printStackTrace();
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (ClientProtocolException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                updateMessage("Download complete.");
                Thread.sleep(1000);
                return true;
            }
        };
    }


    public String getActivatedMerchant() {
        try {
            File activation = new File(App.appContextHolder.getActivationPath());
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
        String filePath;
        try {
            File versionFile = new File(App.appContextHolder.getVersionFilePath());
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

    public void launchRushPOS() {
        try {
            if (App.appContextHolder.isLinux()) {
                Runtime.getRuntime().exec("/usr/lib/jvm/java-8-oracle/bin/java -jar /home/lynx/Yondu/Rush-POS-Sync/rush-pos-1.0-SNAPSHOT.jar");
            } else {
                Runtime.getRuntime().exec(new String[] {App.appContextHolder.getJavaExePath(), "-Dcom.sun.javafx.isEmbedded=true", "-Dcom.sun.javafx.virtualKeyboard=javafx",  App.appContextHolder.getJarFilePath()});
            }
            System.exit(0);
        } catch (IOException e) {
            e.printStackTrace();
        }
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
