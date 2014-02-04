package com.mrk1869.glasslogger;

import java.io.File;
import java.io.FileOutputStream;
import java.util.GregorianCalendar;

import android.R.bool;
import android.R.integer;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PixelFormat;
import android.graphics.Bitmap.CompressFormat;
import android.hardware.Camera;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Environment;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.WindowManager;

public class LoggerService extends Service implements SensorEventListener {

    public static boolean isLogging = false;
    private String logSessionDirectoryPath;

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

    private Thread cameraThread;
    private Camera mCamera;
    private CameraSurfaceView mCameraSurfaceView;

    private SharedPreferences sharedPreferences;
    private WindowManager windowManager;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        sensorManager = (SensorManager) getApplicationContext()
                .getSystemService(SENSOR_SERVICE);
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

        logSessionDirectoryPath = Environment.getExternalStorageDirectory()
                + "/GlassLogger/" + logSessionIdentifier + "/";

        File logSessionDirectory = new File(logSessionDirectoryPath);

        try {
            logSessionDirectory.mkdirs();
        } catch (Exception e) {
            Log.e(this.getClass().getSimpleName(), "Error: " + e.getMessage());
        }

        // accelerometer
        if (sharedPreferences.getBoolean("preference_accelerometer", true)) {
            accLogFileWriter = new LogFileWriter(logSessionDirectoryPath
                    + "acc.txt");
            accSensor = (Sensor) sensorManager.getSensorList(
                    Sensor.TYPE_ACCELEROMETER).get(0);
            sensorManager.registerListener(this, accSensor,
                    SensorManager.SENSOR_DELAY_FASTEST);
        }

        // rotation vector
        if (sharedPreferences.getBoolean("preference_rotation", true)) {
            rotationLogFileWriter = new LogFileWriter(logSessionDirectoryPath
                    + "rotation.txt");
            quaternionLogFileWriter = new LogFileWriter(logSessionDirectoryPath
                    + "quaternion.txt");
            rvSensor = (Sensor) sensorManager.getSensorList(
                    Sensor.TYPE_ROTATION_VECTOR).get(0);
            sensorManager.registerListener(this, rvSensor,
                    SensorManager.SENSOR_DELAY_FASTEST);
        }

        // gyroscope
        if (sharedPreferences.getBoolean("preference_gyroscope", true)) {
            gyroLogFileWriter = new LogFileWriter(logSessionDirectoryPath
                    + "gyro.txt");
            gsSensor = (Sensor) sensorManager.getSensorList(
                    Sensor.TYPE_GYROSCOPE).get(0);
            sensorManager.registerListener(this, gsSensor,
                    SensorManager.SENSOR_DELAY_FASTEST);
        }

        // light sensor
        if (sharedPreferences.getBoolean("preference_light_sensor", true)) {
            lightSensorLogFileWriter = new LogFileWriter(
                    logSessionDirectoryPath + "light.txt");
            liSensor = (Sensor) sensorManager.getSensorList(Sensor.TYPE_LIGHT)
                    .get(0);
            sensorManager.registerListener(this, liSensor,
                    SensorManager.SENSOR_DELAY_FASTEST);
        }

        // proximity sensor
        if (sharedPreferences.getBoolean("preference_proximity_sensor", true)) {
            irLogFileWriter = new LogFileWriter(logSessionDirectoryPath
                    + "proximity.txt");
            mIRSensorLogger = new IRSensorLogger();
            irThread = new Thread() {
                public void run() {
                    while (isLogging) {
                        try {
                            Float logData = mIRSensorLogger.getIRSensorData();
                            if (logData > 0.0f) {
                                // DOCUMENT error code:
                                // -1.0: permission denied.
                                // -2.0: thread has just stopped.
                                irLogFileWriter.writeIRSensorData(
                                        System.currentTimeMillis(), logData);
                            }
                        } catch (Exception e) {
                            Log.v("LoggerService", "IRLogger has some error..");
                        }
                    }
                }
            };
            irThread.start();
        }

        // camera
        if (sharedPreferences.getBoolean("preference_camera", true)) {
            mCamera = Camera.open();
            WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                    3, 2, WindowManager.LayoutParams.TYPE_TOAST,
                    WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
                    PixelFormat.TRANSLUCENT);
            windowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
            mCameraSurfaceView = new CameraSurfaceView(this, mCamera);
            windowManager.addView(mCameraSurfaceView, params);
            mCamera.startPreview();
            cameraThread = new Thread() {
                public void run() {
                    while (isLogging) {
                        try {
                            Thread.sleep(60000);
                            mCamera.takePicture(null, null, pictureCallback);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
            };
            cameraThread.start();
        }
    }

    private Camera.PictureCallback pictureCallback = new Camera.PictureCallback() {
        public void onPictureTaken(byte[] data, Camera camera) {
            if (data == null || mCamera == null) {
                return;
            }
            
            String imageDirectoryPath = logSessionDirectoryPath + "camera/";
            File imageDirectory = new File(imageDirectoryPath);
            if (!imageDirectory.exists()) {
                try {
                    imageDirectory.mkdirs();
                } catch (Exception e) {
                    Log.e(this.getClass().getSimpleName(),
                        "Error: " + e.getMessage());
                }
            }

            String imagePath = imageDirectoryPath + System.currentTimeMillis()
                    + ".jpg";
            FileOutputStream fileOutputStream;
            try {
                fileOutputStream = new FileOutputStream(imagePath, true);
                fileOutputStream.write(data);
                fileOutputStream.close();
            } catch (Exception e) {
            }
            fileOutputStream = null;
        }
    };

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        Long timestamp = System.currentTimeMillis();
        switch (event.sensor.getType()) {
        case Sensor.TYPE_ACCELEROMETER:
            accLogFileWriter.writeACCdata(timestamp, event.values[0],
                    event.values[1], event.values[2]);
            break;

        case Sensor.TYPE_ROTATION_VECTOR:
            rotationLogFileWriter.writeRotationVectorData(timestamp,
                    event.values[0], event.values[1], event.values[2]);
            float[] quaternion = new float[4];
            SensorManager.getQuaternionFromVector(quaternion, event.values);
            quaternionLogFileWriter.writeQuaternionData(timestamp,
                    quaternion[0], quaternion[1], quaternion[2], quaternion[3]);
            break;

        case Sensor.TYPE_GYROSCOPE:
            gyroLogFileWriter.writeGyroscopeData(timestamp, event.values[0],
                    event.values[1], event.values[2]);
            break;

        case Sensor.TYPE_LIGHT:
            lightSensorLogFileWriter.writeLightSensorData(timestamp,
                    event.values[0]);
            break;

        default:
            break;
        }
    }

    @Override
    public void onDestroy() {

        // Stop logging
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

        if (mCamera != null) {
            cameraThread.interrupt();
            mCamera.release();
            mCamera = null;
            windowManager.removeView(mCameraSurfaceView);
        }
    }

}
