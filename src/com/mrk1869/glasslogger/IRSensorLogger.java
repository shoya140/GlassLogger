package com.mrk1869.glasslogger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import android.util.Log;

public class IRSensorLogger{

    public float getIRSensorData() {
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
            return Float.valueOf(output.toString());
        }catch (IOException e){
            // permission error
            Log.v("IRSensor", "Permission error!");
            return -1.0f;
        }catch (InterruptedException e) {
            // finished sign
            return -2.0f;
        }
    }
    
    public boolean isInstallationFinished () {
        Float res = this.getIRSensorData();
        if (res == -1.0f){
            return false;
        }
        return true;
    }

}