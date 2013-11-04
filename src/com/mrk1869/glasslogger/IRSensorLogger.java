package com.mrk1869.glasslogger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class IRSensorLogger{

    public String getIRSensorData() {
        String data = new String();
        try {
            Process process = Runtime.getRuntime().exec("cat /sys/bus/i2c/devices/4-0035/proxraw");
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            int read;
            char[] buffer = new char[256];
            StringBuffer output = new StringBuffer();
            while ((read = reader.read(buffer)) > 0) {
                output.append(buffer, 0, read);
            }
            reader.close();
            process.waitFor();
            data = output.toString();
        }catch (IOException e){
            throw new RuntimeException(e);
        }catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        return data;
    }

}