package com.mrk1869.glasslogger;

import java.io.File;
import java.util.GregorianCalendar;

import android.app.Service;
import android.content.Intent;
import android.os.Environment;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

public class LoggerService extends Service{

    public static boolean isLogging = false;
    private IRSensorLogger mIRSensorLogger;
    private Thread mThread;
    private LogFileWriter mLogFileWriter;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        isLogging = true;
        foregroundProcessing();
        return START_STICKY;
    }

    private void foregroundProcessing() {
        GregorianCalendar now = new GregorianCalendar();

        String logSessionIdentifier = now.get(GregorianCalendar.YEAR) + "-"
                + (now.get(GregorianCalendar.MONTH) + 1) + "-"
                + now.get(GregorianCalendar.DAY_OF_MONTH) + "_"
                + now.get(GregorianCalendar.HOUR_OF_DAY) + "-"
                + now.get(GregorianCalendar.MINUTE) + "-"
                + now.get(GregorianCalendar.SECOND) + "-"
                + now.get(GregorianCalendar.MILLISECOND);

        File logSessionDirectoryPath = new File(Environment.getExternalStorageDirectory()
                + "/GlassLogger/" + logSessionIdentifier);

        String logSessionFilePath = logSessionDirectoryPath.getAbsolutePath();
        mLogFileWriter = new LogFileWriter(logSessionFilePath+"_ir.txt");
        
        mIRSensorLogger = new IRSensorLogger();
        mThread = new Thread(){
            @Override
            public void run(){
                while (isLogging) {
                    try {
                        String logData = mIRSensorLogger.getIRSensorData();
                        mLogFileWriter.writeIRSensorData(System.currentTimeMillis(), logData);
                    } catch (Exception e) {
                        Log.v("IRSensorLogger", "stopped");
                    }
                }
            }
        };
        mThread.start();
    }

    @Override
    public void onDestroy() {
        //Stop logging
        mLogFileWriter.closeWriter();
        mIRSensorLogger = null;
        isLogging = false;
        mThread.interrupt();
    }

}
