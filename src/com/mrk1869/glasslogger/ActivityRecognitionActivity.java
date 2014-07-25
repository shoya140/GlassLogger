package com.mrk1869.glasslogger;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import android.app.Activity;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.WindowManager;
import android.widget.TextView;

public class ActivityRecognitionActivity extends Activity implements
		SensorEventListener {

	private final int WINDOW_SIZE = 10000;
	private final int UPDATE_INTERVAL = 2000;

	private IRSensorLogger irSensorLogger;
	private Thread mThread;
	private boolean isRunning;

	private SensorManager sensorManager;
	private Sensor accSensor;

	private int activityStatus; // 1 default 2 reading 3 talking
	private float irValue;
	private float xAccValue;
	private float yAccValue;
	private float zAccValue;
	private float irTHRESTOLD;
	private TextView activityTextView;
	private TextView accTextView;
	private TextView blinkTextView;
	private SoundPool mSoundPool;
	private int mSoundID;
	private int mSoundIDTalking;
	private int mSoundIDReading;
	private boolean preferences_make_a_sound;

	ArrayList<Float> mXAccValues = new ArrayList<Float>();
	ArrayList<Float> mYAccValues = new ArrayList<Float>();
	ArrayList<Float> mZAccValues = new ArrayList<Float>();
	ArrayList<Long> mAccTimeStamps = new ArrayList<Long>();
	ArrayList<Long> mBlinkTimeStamps = new ArrayList<Long>();

	private Timer mTimer;
	private Handler mHandler;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_recognition);
		activityTextView = (TextView) findViewById(R.id.activityLabel);
		accTextView = (TextView) findViewById(R.id.accLabel);
		blinkTextView = (TextView) findViewById(R.id.blinkLabel);

		activityStatus = 1; // nothing

		irSensorLogger = new IRSensorLogger();
		irValue = 0.0f;
		sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
		accSensor = (Sensor) sensorManager.getSensorList(
				Sensor.TYPE_ACCELEROMETER).get(0);

		SharedPreferences sharedPreferences = PreferenceManager
				.getDefaultSharedPreferences(this);
		irTHRESTOLD = sharedPreferences.getFloat("threshold", 4.0f);
		preferences_make_a_sound = sharedPreferences.getBoolean("sound", true);

	}

	@Override
	protected void onResume() {
		super.onResume();

		sensorManager.registerListener(this, accSensor,
				SensorManager.SENSOR_DELAY_FASTEST);

		// // SE
		mSoundPool = new SoundPool(1, AudioManager.STREAM_MUSIC, 0);
		mSoundID = mSoundPool.load(getApplicationContext(), R.raw.chime, 0);
		mSoundIDReading = mSoundPool.load(getApplicationContext(),
				R.raw.reading, 0);
		mSoundIDTalking = mSoundPool.load(getApplicationContext(),
				R.raw.talking, 0);

		mThread = new Thread() {
			@Override
			public void run() {

				// peak detection
				ArrayList<Float> irValues = new ArrayList<Float>();
				Long lastBlinkTimestamp = System.currentTimeMillis();
				while (isRunning) {
					try {
						Float logData = irSensorLogger.getIRSensorData();
						// DOCUMENT error code:
						// -1.0: permission denied.
						// -2.0: thread has just stopped.
						if (logData > 0.0f) {
							irValue = logData;

							// peak detection
							irValues.add(irValue);
							mXAccValues.add(xAccValue);
							mYAccValues.add(yAccValue);
							mZAccValues.add(zAccValue);
							mAccTimeStamps.add(System.currentTimeMillis());
							if (irValues.size() < 8) {
								continue;
							}
							irValues.remove(0);

							// Removing over flowed data
							if (mAccTimeStamps.size() > WINDOW_SIZE * 30 / 1000) {
								mXAccValues.remove(0);
								mYAccValues.remove(0);
								mZAccValues.remove(0);
								mAccTimeStamps.remove(0);
							}

							if (mBlinkTimeStamps.size() > WINDOW_SIZE / 1000) {
								mBlinkTimeStamps.remove(0);
							}

							Float left = (irValues.get(0) + irValues.get(1) + irValues
									.get(2)) / 3.0f;
							Float right = (irValues.get(4) + irValues.get(5) + irValues
									.get(6)) / 3.0f;
							Float peak = irValues.get(3);

							if (left < peak && peak < right) {
								continue;
							}
							if (left > peak && peak > right) {
								continue;
							}

							Float peak_to_left = (float) Math.pow((peak - left)
									* (peak - left), 0.5);
							Float peak_to_right = (float) Math.pow(
									(peak - right) * (peak - right), 0.5);
							Float left_to_right = (float) Math.pow(
									(right - left) * (right - left), 0.5);

							if (peak_to_left < left_to_right
									|| peak_to_right < left_to_right) {
								continue;
							}

							Float diff = (float) Math.pow(
									Math.pow(peak - (left + right) / 2.0f, 2),
									0.5);
							if (diff > irTHRESTOLD) {
								Long blinkTimestamp = System
										.currentTimeMillis();
								if (blinkTimestamp > lastBlinkTimestamp + 500) {
									if (preferences_make_a_sound) {
										mBlinkTimeStamps.add(blinkTimestamp);
										// mSoundPool.play(mSoundID, 1.0f, 1.0f,
										// 0, 0, 2.0f);
									}
								}
								lastBlinkTimestamp = blinkTimestamp;
							}
						}
					} catch (Exception e) {
						Log.v("Monitoring Activity",
								"IRSensorLogger has some errors..");
					}
				}
			}
		};
		mThread.start();
		isRunning = true;
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

		mHandler = new Handler();
		mTimer = new Timer();
		mTimer.schedule(new TimerTask() {
			@Override
			public void run() {
				// mHandlerを通じてUI Threadへ処理をキューイング
				mHandler.post(new Runnable() {
					public void run() {
						Long now = System.currentTimeMillis();

						ArrayList<Float> xAccValues = new ArrayList<Float>(
								mXAccValues);
						ArrayList<Float> yAccValues = new ArrayList<Float>(
								mYAccValues);
						ArrayList<Float> zAccValues = new ArrayList<Float>(
								mZAccValues);
						ArrayList<Long> accTimeStamps = new ArrayList<Long>(
								mAccTimeStamps);
						ArrayList<Long> blinkTimeStamps = new ArrayList<Long>(
								mBlinkTimeStamps);

						while (now - accTimeStamps.get(0) > WINDOW_SIZE) {
							accTimeStamps.remove(0);
							xAccValues.remove(0);
							yAccValues.remove(0);
							zAccValues.remove(0);
						}
						while (blinkTimeStamps.size() > 0
								&& now - blinkTimeStamps.get(0) > WINDOW_SIZE) {
							blinkTimeStamps.remove(0);
						}

						float xAccAve = 0;
						float yAccAve = 0;
						float zAccAve = 0;
						for (float xAcc : xAccValues) {
							xAccAve += xAcc;
						}
						for (float yAcc : yAccValues) {
							yAccAve += yAcc;
						}
						for (float zAcc : zAccValues) {
							zAccAve += zAcc;
						}
						xAccAve = xAccAve / (float) xAccValues.size();
						yAccAve = yAccAve / (float) yAccValues.size();
						zAccAve = zAccAve / (float) zAccValues.size();

						float xAccVar = 0;
						float yAccVar = 0;
						float zAccVar = 0;
						for (float xAcc : xAccValues) {
							xAccVar += Math.pow(
									Math.pow((xAcc - xAccAve), 2.0), 1.0 / 2.0);
						}
						for (float yAcc : yAccValues) {
							yAccVar += Math.pow(
									Math.pow((yAcc - yAccAve), 2.0), 1.0 / 2.0);
						}
						for (float zAcc : zAccValues) {
							zAccVar += Math.pow(
									Math.pow((zAcc - zAccAve), 2.0), 1.0 / 2.0);
						}
						xAccVar = xAccVar / (float) xAccValues.size();
						yAccVar = yAccVar / (float) yAccValues.size();
						zAccVar = zAccVar / (float) zAccValues.size();

						float accVar = (float) ((xAccVar + yAccVar + zAccVar) / 3.0);
						float blinkFrequency = (float) blinkTimeStamps.size()
								/ (float) (WINDOW_SIZE / 1000);
						accTextView.setText(String.format("acc.: %.3f", accVar));
						blinkTextView.setText(String.format("blink: %.3f",
								blinkFrequency));

						if (blinkFrequency > 0.4 && accVar > 0.3) {
							if (activityStatus != 3) {
								activityStatus = 3;
								activityTextView.setText("Talking");
								mSoundPool.play(mSoundIDTalking, 1.0f, 1.0f, 0,
										0, 1.0f);
							}
						} else if (blinkFrequency < 0.3 && accVar < 0.6) {
							if (activityStatus != 2) {
								activityStatus = 2;
								activityTextView.setText("Reading");
								mSoundPool.play(mSoundIDReading, 1.0f, 1.0f, 0,
										0, 1.0f);
							}
						} else {
							if (activityStatus != 1) {
								activityStatus = 1;
								activityTextView.setText("");
							}
						}
					}
				});
			}
		}, WINDOW_SIZE, UPDATE_INTERVAL);
	}

	@Override
	protected void onPause() {
		getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		isRunning = false;
		mThread.interrupt();
		sensorManager.unregisterListener(this);
		mTimer.cancel();
		mSoundPool.release();
		super.onPause();
	}

	@Override
	public void onSensorChanged(SensorEvent event) {
		// TODO Auto-generated method stub
		switch (event.sensor.getType()) {
		case Sensor.TYPE_ACCELEROMETER:
			xAccValue = event.values[0];
			yAccValue = event.values[1];
			zAccValue = event.values[2];
		}
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		// TODO Auto-generated method stub

	}

}