package com.yondu.commons;

import com.yondu.services.ApiService;
import static com.yondu.commons.AppContants.*;
/**
 * Created by lynx on 2/23/17.
 */
public class AppContextHolder {


    public static final ApiService apiService = new ApiService();
    public static String JAVA_EXE = "./usr/lib/jvm/java-8-oracle/bin/java.exe";
}
