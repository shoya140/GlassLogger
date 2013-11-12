package com.mrk1869.glasslogger;

import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import android.util.Log;

public class LogFileWriter {

    private BufferedWriter mBufferedWriter = null;
    private BufferedOutputStream mBufferedOutputStream = null;
    private File mFile;
    private FileWriter mFileWriter = null;
    private final int BUFFER_SIZE = 100;
    
    public LogFileWriter(String filename) {
        this.mFile = new File(filename);
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
    
    public void writeGyroscopeData(Long timestamp, float xVal, float yVal, float zVal) {
        String logString = timestamp + "\t" + xVal + "\t" + yVal + "\t" + zVal + "\n";
        this.writeString(logString);
    }
    
    public void writeIRSensorData(Long timestamp, String string){
        String logString = timestamp + "\t" + string;
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

    public void writeBuffer(byte[] buffer) throws IOException {
        if (mBufferedOutputStream != null) {
            mBufferedOutputStream.write(buffer);
            mBufferedOutputStream.flush();
        } else {
            throw new IOException();
        }
    }

    public void writeBuffer(short[] buffer, int start, int num) throws IOException {
        if (start < 0 || num < 0) {
            Log.v(">>>>>>>>>>>>>", "LogfileWriter Error: start or num == 0!");
            return;
        }
        if (start + num > buffer.length) {
            Log.v(">>>>>>>>>>>>>",
                    "LogfileWriter Error: Call to writeBuffer with invalid parameters! "
                            + buffer.length + " " + start + " " + num);
            return;
        }
        for (int i = 0; i < num; i++) {

            mBufferedOutputStream.write(((buffer[start + i]) >> 8) & 0xFF);
            mBufferedOutputStream.write((buffer[start + i]) & 0xFF);
        }
        mBufferedOutputStream.flush();
    }

    public void closeWriter() {
        Log.v(">>>>>>>>>>>>>", "logfilewriter-stop");
        try {
            mBufferedWriter.flush();
            mBufferedWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
