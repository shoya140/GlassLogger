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
import android.view.WindowManager;

public class LoggerService extends Service implements SensorEventListener{

    public static boolean isLogging = false;
    
    private Thread irThread;
    private IRSensorLogger mIRSensorLogger;
    private LogFileWriter irLogFileWriter;
    
    private SensorManager sensorManager;
    
    private Sensor accSensor;
    private LogFileWriter accLogFileWriter;
    
    private Sensor rvSensor;
    private LogFileWriter rotationLogFileWriter;
    private LogFileWriter quaternionLogFileWriter;

    private Sensor gsSensor;
    private LogFileWriter gyroLogFileWriter;

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

        File logSessionDirectoryPath = new File(Environment.getExternalStorageDirectory() + "/GlassLogger/");
        
        try {
            logSessionDirectoryPath.mkdirs();
        } catch (Exception e) {// Catch exception if any
            Log.e(this.getClass().getSimpleName(), "Error: " + e.getMessage());
        }

        String logSessionFilePath = Environment.getExternalStorageDirectory() + "/GlassLogger/" + logSessionIdentifier;
        
        // accelerometer
        accLogFileWriter = new LogFileWriter(logSessionFilePath+"_acc.txt");
        accSensor = (Sensor)sensorManager.getSensorList(Sensor.TYPE_ACCELEROMETER).get(0);
        sensorManager.registerListener(this, accSensor, SensorManager.SENSOR_DELAY_FASTEST);
        
        // rotation vector
        rotationLogFileWriter = new LogFileWriter(logSessionFilePath+"_rotation.txt");
        quaternionLogFileWriter = new LogFileWriter(logSessionFilePath+"_quaternion.txt");
        rvSensor = (Sensor)sensorManager.getSensorList(Sensor.TYPE_ROTATION_VECTOR).get(0);
        sensorManager.registerListener(this, rvSensor, SensorManager.SENSOR_DELAY_FASTEST);
        
        // gyroscope
        gyroLogFileWriter = new LogFileWriter(logSessionFilePath+"_gyro.txt");
        gsSensor = (Sensor)sensorManager.getSensorList(Sensor.TYPE_GYROSCOPE).get(0);
        sensorManager.registerListener(this, gsSensor, SensorManager.SENSOR_DELAY_FASTEST);
        
        // IR sensor
        irLogFileWriter = new LogFileWriter(logSessionFilePath+"_ir.txt");
        mIRSensorLogger = new IRSensorLogger();        
        irThread = new Thread(){
            public void run(){
                while (isLogging) {
                    try {
                        Float logData = mIRSensorLogger.getIRSensorData();
                        if (logData > 0.0f) {
                            // DOCUMENT error code:
                            // -1.0: permission denied.
                            // -2.0: thread has just stopped.
                            irLogFileWriter.writeIRSensorData(System.currentTimeMillis(), logData);
                        }
                    } catch (Exception e) {
                        Log.v("LoggerService", "IRLogger has some error..");
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
        Long timestamp = System.currentTimeMillis();
        switch (event.sensor.getType()) {
        case Sensor.TYPE_ACCELEROMETER:
            accLogFileWriter.writeACCdata(
                    timestamp, 
                    event.values[0], 
                    event.values[1],
                    event.values[2]
                            );
            break;
            
        case Sensor.TYPE_ROTATION_VECTOR:
            rotationLogFileWriter.writeRotationVectorData(
                    timestamp, 
                    event.values[0], 
                    event.values[1],
                    event.values[2]
                            );
            float[] quaternion = new float[4];
            SensorManager.getQuaternionFromVector(quaternion, event.values);
            quaternionLogFileWriter.writeQuaternionData(timestamp, 
                    quaternion[0], 
                    quaternion[1], 
                    quaternion[2], 
                    quaternion[3]
                            );            
            break;
            
        case Sensor.TYPE_GYROSCOPE:
            gyroLogFileWriter.writeGyroscopeData(
                    timestamp, 
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
        rotationLogFileWriter.closeWriter();
        quaternionLogFileWriter.closeWriter();
        gyroLogFileWriter.closeWriter();
        irLogFileWriter.closeWriter();
        
        //Stop logging
        sensorManager.unregisterListener(this);
        mIRSensorLogger = null;
        isLogging = false;
        irThread.interrupt();        
    }
    
}
