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
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.*;
import java.net.URL;
import java.util.*;
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

    @Override
    public void initialize(URL location, ResourceBundle resources) {

        this.givePointsButton.addEventHandler(MouseEvent.MOUSE_CLICKED, (MouseEvent event) -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION, "Are you sure you want to cancel software update?", ButtonType.YES, ButtonType.NO);
            alert.showAndWait();

            if (alert.getResult() == ButtonType.NO) {
                alert.close();
            } else {
                alert.close();
                exitApp();
            }
        });

        this.rushLogoImage.setImage(new Image(App.class.getResource("/app/images/rush_logo.png").toExternalForm()));
        this.downloadLabel.setText("Downloading update from Rush server..");
        File file = new File(System.getProperty("user.home") + "\\Rush-POS-Sync\\rush-pos-1.0-SNAPSHOT.jar");
        long modificationDate = file.lastModified();

        try {
            File activation = new File(System.getProperty("user.home") + "\\Rush-POS-Sync\\activation.txt");
            BufferedReader br = new BufferedReader(new FileReader(activation));
            String l = "";
            String merchant = null;
            while ((l = br.readLine()) != null) {
                String[] arr = l.split("=");
                merchant = arr[1];
            }
            //Get oauth token




            final String merch = merchant;
            HttpResponse response = null;
            CloseableHttpClient httpClient = HttpClientBuilder.create().build();
            HttpGet request = new HttpGet("http://52.74.203.202:8080/rush-pos-sync/api/updates/" + merchant + "/" + modificationDate);
            request.addHeader("content-type", "application/octet-stream");
            request.addHeader("Authorization", "Bearer "  + getToken());
            response = httpClient.execute(request);
            // use httpClient (no need to close it explicitly)
            BufferedReader rd = new BufferedReader(
                    new InputStreamReader(response.getEntity().getContent()));

            StringBuffer result = new StringBuffer();
            String line = "";
            while ((line = rd.readLine()) != null) {
                result.append(line);
            }
            br.close();
            httpClient.close();
            Date date = new Date();
            Long m = date.getTime();
            JSONParser parser = new JSONParser();
            JSONObject jsonObject = (JSONObject) parser.parse(result.toString());
            if (jsonObject.get("data") != null) {
                Long totalBytes = (Long) jsonObject.get("data");
                Alert alert = new Alert(Alert.AlertType.INFORMATION, "There is a software update available with a total of " + (totalBytes / 1000000) + "mb. Would you like to download it now?", ButtonType.YES, ButtonType.NO);
                alert.showAndWait();

                if (alert.getResult() == ButtonType.NO) {
                    alert.close();
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
                        a.showAndWait();

                        if (a.getResult() == ButtonType.OK) {
                            try {
                                Runtime.getRuntime().exec(new String[] {"java", "-Dcom.sun.javafx.isEmbedded=true", "-Dcom.sun.javafx.virtualKeyboard=javafx", "-Dcom.sun.javafx.touch=true", "-jar", System.getProperty("user.home") + "\\Rush-POS-Sync\\rush-pos-1.0-SNAPSHOT.jar"});
                            } catch (IOException ee) {
                                ee.printStackTrace();
                            }
                            System.exit(0);
                        }
                    });
                }
            } else {
                Runtime.getRuntime().exec(new String[] {"java", "-Dcom.sun.javafx.isEmbedded=true", "-Dcom.sun.javafx.virtualKeyboard=javafx", "-Dcom.sun.javafx.touch=true", "-jar", System.getProperty("user.home") + "\\Rush-POS-Sync\\rush-pos-1.0-SNAPSHOT.jar"});
                System.exit(0);
            }

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
            try {
                Runtime.getRuntime().exec(new String[] {"java", "-Dcom.sun.javafx.isEmbedded=true", "-Dcom.sun.javafx.virtualKeyboard=javafx", "-Dcom.sun.javafx.touch=true", "-jar", System.getProperty("user.home") + "\\Rush-POS-Sync\\rush-pos-1.0-SNAPSHOT.jar"});
            } catch (IOException e1) {
                e1.printStackTrace();
            }
            System.exit(0);
        } catch (ParseException e) {
            e.printStackTrace();
        }


    }

    private void exitApp() {
        try {
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
                        HttpResponse response = null;
                        // set the connection timeout value to 30 seconds (30000 milliseconds)
                        final HttpParams httpParams = new BasicHttpParams();
                        HttpConnectionParams.setConnectionTimeout(httpParams, 5000);
                        HttpConnectionParams.setSoTimeout(httpParams, 5000);
                        CloseableHttpClient httpClient = new DefaultHttpClient(httpParams);
                        HttpGet request = new HttpGet("http://52.74.203.202:8080/rush-pos-sync/api/updates/download/" + merchantKey);
                        request.addHeader("content-type", "application/octet-stream");
                        request.addHeader("Authorization", "Bearer "  + getToken());
                        response = httpClient.execute(request);

                        InputStream inputStream = response.getEntity().getContent();
                        String location = System.getProperty("user.home") + "\\Rush-POS-Sync\\rush-update.jar";
                        FileOutputStream out = new FileOutputStream(location);
                        int len = 0;
                        byte[] buffer = new byte[5024];
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
                            buffer = new byte[5024];

                            //copy the file content in bytes
                            while ((length = inStream.read(buffer)) > 0){
                                outStream.write(buffer, 0, length);
                            }
                            inStream.close();
                            outStream.close();

                            Runtime.getRuntime().exec(new String[] {"java", "-Dcom.sun.javafx.isEmbedded=true", "-Dcom.sun.javafx.virtualKeyboard=javafx", "-Dcom.sun.javafx.touch=true", "-jar", System.getProperty("user.home") + "\\Rush-POS-Sync\\rush-pos-1.0-SNAPSHOT.jar"});
                            System.exit(0);


                        } catch(Exception ex) {
                            ex.printStackTrace();
                            Alert alert = new Alert(Alert.AlertType.INFORMATION, "File is corrupt", ButtonType.OK);
                            alert.showAndWait();

                            if (alert.getResult() == ButtonType.OK) {
                                alert.close();
                            }
                        }

                    } catch (Exception e) {
                        e.printStackTrace();
                        throw new IOException();
                    }
                    return null;
                }
            };
        }
    }

    public String getToken() throws IOException, ParseException {
        CloseableHttpClient httpClient = HttpClientBuilder.create().build();
        HttpPost httpPost = new HttpPost("http://52.74.203.202:8080/rush-pos-sync/oauth/token?grant_type=password&username=admin&password=admin&client_id=clientIdPassword");
        httpPost.addHeader("Content-Type", "application/json");
        httpPost.addHeader("Authorization", "Basic Y2xpZW50SWRQYXNzd29yZDpzZWNyZXQ=");
        HttpResponse response = httpClient.execute(httpPost);
        BufferedReader rd = new BufferedReader(
                new InputStreamReader(response.getEntity().getContent()));

        StringBuffer result = new StringBuffer();
        String line = "";
        while ((line = rd.readLine()) != null) {
            result.append(line);
        }
        JSONParser parser = new JSONParser();
        JSONObject jsonObject = (JSONObject) parser.parse(result.toString());
        return (String) jsonObject.get("access_token");
    }
}
