package com.yondu.commons;

import com.yondu.services.ApiService;
import static com.yondu.commons.AppContants.*;
/**
 * Created by lynx on 2/23/17.
 */
public class AppContextHolder {


    private ApiService apiService;
    private String javaExePath;
    private String lockFilePath;
    private String activationPath;
    private String jarFilePath;
    private String versionFilePath;
    private String updateFilePath;
    private boolean isLinux;
    private String ocrFilePath;
    private String exeJarFilePath;

    public String getExeJarFilePath() {
        return exeJarFilePath;
    }

    public void setExeJarFilePath(String exeJarFilePath) {
        this.exeJarFilePath = exeJarFilePath;
    }

    private String installationDir;

    public String getOcrFilePath() {
        return ocrFilePath;
    }

    public void setOcrFilePath(String ocrFilePath) {
        this.ocrFilePath = ocrFilePath;
    }

    public String getInstallationDir() {
        return installationDir;

    }

    public void setInstallationDir(String installationDir) {
        this.installationDir = installationDir;
    }

    public ApiService getApiService() {
        return apiService;
    }

    public void setApiService(ApiService apiService) {
        this.apiService = apiService;
    }

    public String getJavaExePath() {
        return javaExePath;
    }

    public void setJavaExePath(String javaExePath) {
        this.javaExePath = javaExePath;
    }

    public String getLockFilePath() {
        return lockFilePath;
    }

    public void setLockFilePath(String lockFilePath) {
        this.lockFilePath = lockFilePath;
    }

    public String getActivationPath() {
        return activationPath;
    }

    public void setActivationPath(String activationPath) {
        this.activationPath = activationPath;
    }

    public String getJarFilePath() {
        return jarFilePath;
    }

    public void setJarFilePath(String jarFilePath) {
        this.jarFilePath = jarFilePath;
    }

    public String getVersionFilePath() {
        return versionFilePath;
    }

    public void setVersionFilePath(String versionFilePath) {
        this.versionFilePath = versionFilePath;
    }

    public String getUpdateFilePath() {
        return updateFilePath;
    }

    public void setUpdateFilePath(String updateFilePath) {
        this.updateFilePath = updateFilePath;
    }

    public boolean isLinux() {
        return isLinux;
    }

    public void setLinux(boolean linux) {
        isLinux = linux;
    }
}
