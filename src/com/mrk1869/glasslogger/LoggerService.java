package com.mrk1869.glasslogger;

import java.io.File;
import java.util.GregorianCalendar;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Environment;
import android.os.IBinder;
import android.preference.PreferenceManager;
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
    
    private Sensor liSensor;
    private LogFileWriter lightSensorLogFileWriter;
    
    private SharedPreferences sharedPreferences;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
    
    @Override
    public void onCreate() {
        super.onCreate();
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
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
        
        String logSessionDirectoryPath = Environment.getExternalStorageDirectory() + "/GlassLogger/" + logSessionIdentifier + "/";

        File logSessionDirectory = new File(logSessionDirectoryPath);
        
        try {
            logSessionDirectory.mkdirs();
        } catch (Exception e) {// Catch exception if any
            Log.e(this.getClass().getSimpleName(), "Error: " + e.getMessage());
        }
        
        // accelerometer
        if (sharedPreferences.getBoolean("preference_accelerometer", false)) {
            accLogFileWriter = new LogFileWriter(logSessionDirectoryPath+"acc.txt");
            accSensor = (Sensor)sensorManager.getSensorList(Sensor.TYPE_ACCELEROMETER).get(0);
            sensorManager.registerListener(this, accSensor, SensorManager.SENSOR_DELAY_FASTEST);
        }
        
        // rotation vector
        if (sharedPreferences.getBoolean("preference_rotation", false)) {
            rotationLogFileWriter = new LogFileWriter(logSessionDirectoryPath+"rotation.txt");
            quaternionLogFileWriter = new LogFileWriter(logSessionDirectoryPath+"quaternion.txt");
            rvSensor = (Sensor)sensorManager.getSensorList(Sensor.TYPE_ROTATION_VECTOR).get(0);
            sensorManager.registerListener(this, rvSensor, SensorManager.SENSOR_DELAY_FASTEST);            
        }
        
        // gyroscope
        if (sharedPreferences.getBoolean("preference_gyroscope", false)) {
        gyroLogFileWriter = new LogFileWriter(logSessionDirectoryPath+"gyro.txt");
        gsSensor = (Sensor)sensorManager.getSensorList(Sensor.TYPE_GYROSCOPE).get(0);
        sensorManager.registerListener(this, gsSensor, SensorManager.SENSOR_DELAY_FASTEST);
        }
        
        // light sensor
        if (sharedPreferences.getBoolean("preference_light_sensor", false)) {
        lightSensorLogFileWriter = new LogFileWriter(logSessionDirectoryPath+"light.txt");
        liSensor = (Sensor)sensorManager.getSensorList(Sensor.TYPE_LIGHT).get(0);
        sensorManager.registerListener(this, liSensor, SensorManager.SENSOR_DELAY_FASTEST);
        }
        
        // proximity sensor
        if (sharedPreferences.getBoolean("preference_proximity_sensor", false)) {
        irLogFileWriter = new LogFileWriter(logSessionDirectoryPath+"proximity.txt");
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
            
        case Sensor.TYPE_LIGHT:
            lightSensorLogFileWriter.writeLightSensorData(
                    timestamp, 
                    event.values[0]
                            );            
            break;

        default:
            break;
        }
    }

    @Override
    public void onDestroy() {
        
        //Stop logging        
        sensorManager.unregisterListener(this);
        isLogging = false;
        
        // save
        if (accSensor != null) {
            accLogFileWriter.closeWriter();
            accSensor = null;
        }
        if (rvSensor != null) {
            rotationLogFileWriter.closeWriter();            
            quaternionLogFileWriter.closeWriter();
            rvSensor = null;
        }
        if (gsSensor != null) {
            gyroLogFileWriter.closeWriter();            
            gsSensor = null;
        }
        if (liSensor != null) {
            lightSensorLogFileWriter.closeWriter();            
            liSensor = null;
        }
        if (mIRSensorLogger != null) {
            irLogFileWriter.closeWriter();
            irThread.interrupt();
            mIRSensorLogger = null;
        }
    }
    
}
