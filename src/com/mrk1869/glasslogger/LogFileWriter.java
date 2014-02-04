package com.mrk1869.glasslogger;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

import android.util.Log;

public class LogFileWriter {

    private BufferedWriter mBufferedWriter = null;
    private File mFile;
    private FileWriter mFileWriter = null;
    private final int BUFFER_SIZE = 100;
    private String fileName;
    
    public LogFileWriter(String filename) {
        this.mFile = new File(filename);
        fileName = filename;
        if (mFile.exists()){
            mFile.delete();
        }
        try {
            mFile.createNewFile();
            mFileWriter = new FileWriter(mFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
        mBufferedWriter = new BufferedWriter(mFileWriter, BUFFER_SIZE);
    }
    
    public void writeACCdata(Long timestamp, float xVal, float yVal, float zVal) {
        String logString = timestamp + "\t" + xVal + "\t" + yVal + "\t" + zVal + "\n";
        this.writeString(logString);
    }

    public void writeRotationVectorData(Long timestamp, float xVal, float yVal, float zVal) {
        String logString = timestamp + "\t" + xVal + "\t" + yVal + "\t" + zVal + "\n";
        this.writeString(logString);
    }
    
    public void writeQuaternionData(Long timestamp, float wVal, float xVal, float yVal, float zVal) {
        String logString = timestamp + "\t" + wVal +"\t" + xVal + "\t" + yVal + "\t" + zVal + "\n";
        this.writeString(logString);
    }
    
    public void writeGyroscopeData(Long timestamp, float xVal, float yVal, float zVal) {
        String logString = timestamp + "\t" + xVal + "\t" + yVal + "\t" + zVal + "\n";
        this.writeString(logString);
    }
    
    public void writeMagneticSensorData(Long timestamp, float xVal, float yVal, float zVal) {
        String logString = timestamp + "\t" + xVal + "\t" + yVal + "\t" + zVal + "\n";
        this.writeString(logString);
    }
    
    public void writeBluetoothData(Long timestamp, ArrayList<String> btDevices) {
        String logString = timestamp.toString();
        for (String device : btDevices) {
            logString = logString + "\t" + device;
        }
        logString = logString + "\n";
        this.writeString(logString);
    }
    
    public void writeLightSensorData(Long timestamp, float value){
        String logString = timestamp + "\t" + value + "\n";
        this.writeString(logString);
    }
    
    public void writeIRSensorData(Long timestamp, float value){
        String logString = timestamp + "\t" + value + "\n";
        this.writeString(logString);
    }

    public void writeString(String logdata) {
        if (mBufferedWriter != null) {
            try {
                mBufferedWriter.write(logdata);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void closeWriter() {
        Log.v("Log file writer", "Finished writing to " + fileName);
        try {
            mBufferedWriter.flush();
            mBufferedWriter.close();
            mBufferedWriter = null;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
