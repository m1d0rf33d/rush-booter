package com.yondu.model;

import org.json.simple.JSONObject;

/**
 * Created by lynx on 2/23/17.
 */
public class ApiResponse {

    private boolean isSuccess;
    private JSONObject payload;

    public boolean isSuccess() {
        return isSuccess;
    }

    public void setSuccess(boolean success) {
        isSuccess = success;
    }

    public JSONObject getPayload() {
        return payload;
    }

    public void setPayload(JSONObject payload) {
        this.payload = payload;
    }
}
