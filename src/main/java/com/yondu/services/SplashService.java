package com.yondu.services;

import java.io.*;

/**
 * Created by lynx on 2/24/17.
 */
public class SplashService {

    public void transferFile(File fileFrom, File fileTo) {
        try {
            InputStream inStream;
            OutputStream outStream;
            inStream = new FileInputStream(fileFrom);
            outStream = new FileOutputStream(fileTo);
            byte[] buffer = new byte[5242880];

            int length;
            while ((length = inStream.read(buffer)) > 0){
                outStream.write(buffer, 0, length);
            }

            inStream.close();
            outStream.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
