package com.mrk1869.glasslogger;

import java.io.File;
import java.util.GregorianCalendar;

import android.app.Service;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Environment;
import android.os.IBinder;
import android.util.Log;

public class LoggerService extends Service implements SensorEventListener{

    public static boolean isLogging = false;
    
    private Thread irThread;
    private IRSensorLogger mIRSensorLogger;
    private LogFileWriter irLogFileWriter;
    
    private SensorManager sensorManager;
    
    private Sensor accSensor;
    private LogFileWriter accLogFileWriter;
    
    private Sensor rvSensor;
    private LogFileWriter rvLogFileWriter;

    private Sensor gsSensor;
    private LogFileWriter gsLogFileWriter;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
    
    @Override
    public void onCreate() {
        super.onCreate();
        sensorManager = (SensorManager)getApplicationContext().getSystemService(SENSOR_SERVICE);
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
        
        // accelerometer
        accLogFileWriter = new LogFileWriter(logSessionFilePath+"_acc.txt");
        accSensor = (Sensor)sensorManager.getSensorList(Sensor.TYPE_ACCELEROMETER).get(0);
        sensorManager.registerListener(this, accSensor, SensorManager.SENSOR_DELAY_FASTEST);
        
        // rotation vector
        rvLogFileWriter = new LogFileWriter(logSessionFilePath+"_rv.txt");
        rvSensor = (Sensor)sensorManager.getSensorList(Sensor.TYPE_ROTATION_VECTOR).get(0);
        sensorManager.registerListener(this, rvSensor, SensorManager.SENSOR_DELAY_FASTEST);
        
        // gyroscope
        gsLogFileWriter = new LogFileWriter(logSessionFilePath+"_gs.txt");
        gsSensor = (Sensor)sensorManager.getSensorList(Sensor.TYPE_GYROSCOPE).get(0);
        sensorManager.registerListener(this, gsSensor, SensorManager.SENSOR_DELAY_FASTEST);
        
        // IR sensor
        irLogFileWriter = new LogFileWriter(logSessionFilePath+"_ir.txt");
        
        mIRSensorLogger = new IRSensorLogger();
        irThread = new Thread(){
            @Override
            public void run(){
                while (isLogging) {
                    try {
                        String logData = mIRSensorLogger.getIRSensorData();
                        irLogFileWriter.writeIRSensorData(System.currentTimeMillis(), Float.valueOf(logData));
                    } catch (Exception e) {
                        Log.v("IRSensorLogger", "stopped");
                    }
                }
            }
        };
        irThread.start();
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        
    }
    @Override
    public void onSensorChanged(SensorEvent event) {
        switch (event.sensor.getType()) {
        case Sensor.TYPE_ACCELEROMETER:
            accLogFileWriter.writeACCdata(
                    System.currentTimeMillis(), 
                    event.values[0], 
                    event.values[1],
                    event.values[2]
                            );
            break;
            
        case Sensor.TYPE_ROTATION_VECTOR:
            rvLogFileWriter.writeACCdata(
                    System.currentTimeMillis(), 
                    event.values[0], 
                    event.values[1],
                    event.values[2]
                            );
            break;
            
        case Sensor.TYPE_GYROSCOPE:
            gsLogFileWriter.writeACCdata(
                    System.currentTimeMillis(), 
                    event.values[0], 
                    event.values[1],
                    event.values[2]
                            );            
            break;

        default:
            break;
        }
        
    }

    @Override
    public void onDestroy() {
        // save
        accLogFileWriter.closeWriter();
        rvLogFileWriter.closeWriter();
        gsLogFileWriter.closeWriter();
        irLogFileWriter.closeWriter();
        
        //Stop logging
        sensorManager.unregisterListener(this);
        mIRSensorLogger = null;
        isLogging = false;
        irThread.interrupt();        
    }
    
}
