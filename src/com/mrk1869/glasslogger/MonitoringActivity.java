package com.mrk1869.glasslogger;

import java.util.ArrayList;

import android.R.integer;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

public class MonitoringActivity extends Activity{

    private IRSensorLogger irSensorLogger;
    private Thread mThread;
    private GraphView mGraphView;
    private boolean isRunning;
    
    private SensorManager sensorManager;
    private Sensor accSensor;
    private Float irValue;
    private int blinkCount;
    private TextView blinkCountTextView;
    private SoundPool mSoundPool;
    private int mSoundID;
    private boolean preferences_make_a_sound;
    private Float irTHRESTOLD;

    private class GraphView extends View implements SensorEventListener{

        private Bitmap  mBitmap;
        private Paint   mPaint = new Paint();
        private Canvas  mCanvas = new Canvas();
        private float   mLastValue;
        private int     mColors[] = new int[3*2];
        private float   mLastX;
        private float   mScale;
        private float   mYOffset;
        private float   mMaxX;
        private float   mSpeed = 0.5f;
        private float   mWidth;
        private float   mHeight;
        private float   defaultValue;

        public GraphView(Context context) {
            super(context);
            mColors[0] = Color.argb(192, 255, 64, 64);
            mColors[1] = Color.argb(192, 64, 128, 64);
            mColors[2] = Color.argb(192, 64, 64, 255);
            mColors[3] = Color.argb(192, 64, 255, 255);
            mColors[4] = Color.argb(192, 128, 64, 128);
            mColors[5] = Color.argb(192, 255, 255, 64);

            mPaint.setFlags(Paint.ANTI_ALIAS_FLAG);
            mPaint.setStrokeWidth(3.0f);
        }

        @Override
        protected void onSizeChanged(int w, int h, int oldw, int oldh) {
            mBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.RGB_565);
            mCanvas.setBitmap(mBitmap);
            mCanvas.drawColor(0xFF000000);
            mYOffset = h * 0.5f;
            defaultValue = irValue;
            mScale = h * 0.5f * (1.0f/100.0f);
            mWidth = w;
            mHeight = h;
            if (mWidth < mHeight) {
                mMaxX = w;
            } else {
                mMaxX = w-50;
            }
            mLastX = mMaxX;
            super.onSizeChanged(w, h, oldw, oldh);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            synchronized (this) {
                if (mBitmap != null) {
                    final Paint paint = mPaint;
                    if (mLastX >= mMaxX) {
                        mLastX = 0;
                        defaultValue = irValue;
                        Toast toast = Toast.makeText(getBaseContext(), String.valueOf(irValue), Toast.LENGTH_LONG);
                        toast.setGravity(Gravity.RIGHT, 0, 0);
                        toast.show();
                        final Canvas cavas = mCanvas;
                        paint.setColor(0xFFAAAAAA);
                        cavas.drawColor(0xFF000000);
                    }
                    canvas.drawBitmap(mBitmap, 0, 0, null);
                }
            }
        }

        public void updateView(float value) {
            synchronized (this) {
                if (mBitmap != null) {
                    blinkCountTextView.setText(String.valueOf(blinkCount));
                    final Canvas canvas = mCanvas;
                    final Paint paint = mPaint;
                    float newX = mLastX + mSpeed;
                    final float v = mYOffset + (value - defaultValue) * mScale;
                    paint.setColor(mColors[0]);
                    canvas.drawLine(mLastX, mLastValue, newX, v, paint);
                    mLastValue = v;
                    mLastX += mSpeed;
                    invalidate();                    
                }
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
            // TODO Auto-generated method stub
        }

        @Override
        public void onSensorChanged(SensorEvent event) {
            // TODO Auto-generated method stub
            updateView(irValue);
        }

    }

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        
        irSensorLogger = new IRSensorLogger();
        irValue = 0.0f;
        sensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);
        accSensor = (Sensor)sensorManager.getSensorList(Sensor.TYPE_ACCELEROMETER).get(0);
        
        mGraphView = new GraphView(this);
        setContentView(mGraphView);
        
        View view = this.getLayoutInflater().inflate(R.layout.activity_monitoring, null);
        addContentView(view,new LayoutParams(LayoutParams.FILL_PARENT,LayoutParams.FILL_PARENT));
        blinkCount = 0;
        blinkCountTextView = (TextView)findViewById(R.id.countLabel);
        blinkCountTextView.setText(String.valueOf(blinkCount));
        
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        preferences_make_a_sound = sharedPreferences.getBoolean("sound", true);
        irTHRESTOLD = sharedPreferences.getFloat("threshold", 4.0f);
        
        TextView thresholdTextView = (TextView)findViewById(R.id.thresholdLabel);
        thresholdTextView.setText("threshold: " + String.valueOf(irTHRESTOLD));
    }

    @Override
    protected void onResume() {
        super.onResume();
        sensorManager.registerListener(mGraphView, accSensor, SensorManager.SENSOR_DELAY_FASTEST);
        
        // SE
        mSoundPool = new SoundPool(1, AudioManager.STREAM_MUSIC, 0);
        mSoundID = mSoundPool.load(getApplicationContext(), R.raw.chime, 0);
        
        mThread = new Thread(){
            @Override
            public void run(){
                
                // peak detection
                ArrayList<Float> irValues = new ArrayList<Float>();
//                int FRAME = 3; // frame for peak detection |<--FRAME-->*peak*<--FRAME-->|
//                int RANGE = 2; // RANGE = 2 #|<--RANGE-->*FRAME*<--RANGE-->|
                Long lastBlinkTimestamp = System.currentTimeMillis();
                while (isRunning) {
                    try {
                        Float logData = irSensorLogger.getIRSensorData();
                        // DOCUMENT error code:
                        // -1.0: permission denied.
                        // -2.0: thread has just stopped.
                        if (logData > 0.0f){
                            irValue = logData;
                            
                            // peak detection
                            irValues.add(irValue);
                            if (irValues.size() < 10) {
                                continue;
                            }
                            irValues.remove(0);
                            
                            Float left = (irValues.get(0)+irValues.get(1)+irValues.get(2)+irValues.get(3))/4.0f;
                            Float right = (irValues.get(5)+irValues.get(6)+irValues.get(7)+irValues.get(8))/4.0f;
                            Float peak = irValues.get(4);
                            
                            if (left < peak && peak < right){
                                continue;
                            }
                            if (left > peak && peak > left){
                                continue;
                            }
                            
                            Float peak_to_left = (float) Math.pow((peak-left)*(peak-left), 0.5);
                            Float peak_to_right = (float) Math.pow((peak-right)*(peak-right), 0.5);
                            Float left_to_right = (float) Math.pow((right-left)*(right-left), 0.5);
                            
                            if (peak_to_left < left_to_right || peak_to_right < left_to_right) {
                                continue;
                            }
                            
                            Float diff = (float) Math.pow(Math.pow(peak - (left+right)/2.0f, 2), 0.5);
                            if (diff > irTHRESTOLD){
                                Long blinkTimestamp = System.currentTimeMillis();
                                if (blinkTimestamp > lastBlinkTimestamp + 500){
                                    blinkCount += 1;
                                    if (preferences_make_a_sound){
                                        mSoundPool.play(mSoundID, 1.0f, 1.0f, 0, 0, 2.0f);
                                    }
                                }
                                lastBlinkTimestamp = blinkTimestamp;
                            }
                        }
                    } catch (Exception e) {
                        Log.v("Monitoring Activity", "IRSensorLogger has some errors..");
                    }
                }
            }
        };
        mThread.start();
        isRunning = true;
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    @Override
    protected void onPause() {
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        sensorManager.unregisterListener(mGraphView);
        isRunning = false;
        mThread.interrupt();
        mSoundPool.release();
        super.onPause();
    }
}
