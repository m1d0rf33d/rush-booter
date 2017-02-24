package com.yondu.services;

import com.yondu.model.ApiResponse;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.client.DefaultHttpRequestRetryHandler;
import org.apache.http.impl.client.HttpClientBuilder;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.*;
import java.util.Properties;

/**
 * Created by erwin on 12/9/2016.
 */
public class ApiService {

    private Properties properties = new Properties();

    public ApiService() {
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

    public ApiResponse checkSoftwareUpdates(String merchant, String version)  {

        ApiResponse apiResponse = new ApiResponse();
        apiResponse.setSuccess(false);

        try {
            DefaultHttpClient httpClient = new DefaultHttpClient();
            DefaultHttpRequestRetryHandler retryHandler = new DefaultHttpRequestRetryHandler(20, true);
            httpClient.setHttpRequestRetryHandler(retryHandler);

            String url = properties.getProperty("base_url") + properties.getProperty("get_updates_api");
            url = url.replace(":merchant", merchant);
            url = url.replace(":version", version);
            HttpGet request = new HttpGet(url);
            request.addHeader("content-type", "application/octet-stream");
            request.addHeader("Authorization", "Bearer "  + getToken());
            HttpResponse response = httpClient.execute(request);
            BufferedReader rd = new BufferedReader(
                    new InputStreamReader(response.getEntity().getContent()));

            StringBuffer result = new StringBuffer();
            String line = "";
            while ((line = rd.readLine()) != null) {
                result.append(line);
            }

            JSONParser parser = new JSONParser();
            JSONObject payload = (JSONObject) parser.parse(request.toString());

            apiResponse.setSuccess(true);
            apiResponse.setPayload(payload);

            rd.close();
            httpClient.close();
        } catch (ParseException e) {
            e.printStackTrace();
        } catch (ClientProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public String getToken() throws IOException, ParseException {
        DefaultHttpClient httpClient = new DefaultHttpClient();
        DefaultHttpRequestRetryHandler retryHandler = new DefaultHttpRequestRetryHandler(20, true);
        httpClient.setHttpRequestRetryHandler(retryHandler);

        String url = properties.getProperty("base_url") + properties.getProperty("get_token_api");
        url = url.replace(":username", properties.getProperty("oauth_username"));
        url = url.replace(":password", properties.getProperty("oauth_password"));
        url = url.replace(":clientIdPassword", properties.getProperty("oauth_client_id_password"));

        HttpPost httpPost = new HttpPost(url);
        httpPost.addHeader("Content-Type", "application/json");
        httpPost.addHeader("Authorization", "Basic " + properties.getProperty("oauth_bearer"));
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
        rd.close();
        httpClient.close();
        return (String) jsonObject.get("access_token");
    }

}
