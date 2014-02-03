package com.mrk1869.glasslogger;

import java.security.Timestamp;
import java.util.ArrayList;

import android.app.Activity;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.view.WindowManager;
import android.widget.TextView;

public class CalibrationActivity extends Activity {

    private IRSensorLogger irSensorLogger;
    private Thread mThread;
    private boolean isRunning;

    private TextView thresholdTextView;
    private SoundPool mSoundPool;
    private int mSoundID1;
    private int mSoundID2;
    private SharedPreferences mSharedPreferences;
    private Handler mHandler;
    private TextView timingTextView;
    private String timingText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        irSensorLogger = new IRSensorLogger();

        View view = this.getLayoutInflater().inflate(
                R.layout.activity_calibration, null);
        addContentView(view, new LayoutParams(LayoutParams.FILL_PARENT,
                LayoutParams.FILL_PARENT));
        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        float threshold = mSharedPreferences.getFloat("threshold", 4.0f);
        thresholdTextView = (TextView) findViewById(R.id.thresholdLabel);
        thresholdTextView.setText("threshold: " + String.valueOf(threshold));
        
        timingTextView = (TextView) findViewById(R.id.timingLabel);
        timingText = new String();
        timingText = "";
        timingTextView.setText(timingText);
    }

    @Override
    protected void onResume() {
        super.onResume();

        // SE
        mSoundPool = new SoundPool(1, AudioManager.STREAM_MUSIC, 0);
        mSoundID1 = mSoundPool.load(getApplicationContext(), R.raw.timing1, 0);
        mSoundID2 = mSoundPool.load(getApplicationContext(), R.raw.timing2, 0);

        mThread = new Thread() {
            @Override
            public void run() {
                ArrayList<Float> irValues = new ArrayList<Float>();
                ArrayList<Long> timestampValues = new ArrayList<Long>();
                ArrayList<Long> labelValues = new ArrayList<Long>();
                // Step1. recording
                while (isRunning) {
                    Long timestamp = System.currentTimeMillis();
                    try {
                        Float logData = irSensorLogger.getIRSensorData();
                        irValues.add(logData);
                        timestampValues.add(timestamp);
                    } catch (Exception e) {
                        Log.v("Monitoring Activity",
                                "IRSensorLogger has some errors..");
                    }
                    final int size = irValues.size();
                    if (size % 60 == 0) {
                        mSoundPool.play(mSoundID2, 1.0f, 1.0f, 0, 0, 1.0f);
                        labelValues.add(timestamp);
                        timingText = timingText + ";)";
                        mHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                timingTextView.setText(timingText);
                                timingText = "";
                            }
                            });
                    } else if (size % 15 == 0) {
                        mSoundPool.play(mSoundID1, 1.0f, 1.0f, 0, 0, 1.0f);
                        timingText = timingText + ".  ";
                        mHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                timingTextView.setText(timingText);
                            }
                            });
                    }
                    if (irValues.size() > 250) {
                        isRunning = false;
                    }
                }

                // Step2. distance calculation
                ArrayList<Float> dValues = new ArrayList<Float>();

                for (int i = 0; i < irValues.size() - 4; i++) {
                    if (i < 4) {
                        dValues.add(0.0f);
                        continue;
                    }

                    Float left = (irValues.get(i - 3) + irValues.get(i - 2) + irValues
                            .get(i - 1)) / 3.0f;
                    Float right = (irValues.get(i + 1) + irValues.get(i + 2) + irValues
                            .get(i + 3)) / 3.0f;
                    Float peak = irValues.get(i);

                    if (left < peak && peak < right) {
                        dValues.add(0.0f);
                        continue;
                    }
                    if (left > peak && peak > right) {
                        dValues.add(0.0f);
                        continue;
                    }

                    Float peak_to_left = (float) Math.pow((peak - left)
                            * (peak - left), 0.5);
                    Float peak_to_right = (float) Math.pow((peak - right)
                            * (peak - right), 0.5);
                    Float left_to_right = (float) Math.pow((right - left)
                            * (right - left), 0.5);

                    if (peak_to_left < left_to_right
                            || peak_to_right < left_to_right) {
                        dValues.add(0.0f);
                        continue;
                    }

                    dValues.add((float) Math.pow(
                            Math.pow(peak - (left + right) / 2.0f, 2), 0.5));
                }

                float maxFmeasure = 0.0f;
                ArrayList<Float> thresholds = new ArrayList<Float>();

                // Step3. peak detection and evaluation
                for (float threshold = 3.0f; threshold < 7.0f; threshold += 0.1) {

                    // Step3.1 peak detection
                    ArrayList<Long> peaks = new ArrayList<Long>();
                    ArrayList<Long> labels = new ArrayList<Long>();
                    labels.addAll(labelValues);
                    Long lastBlinkTimestamp = timestampValues.get(0);
                    for (int i = 0; i < dValues.size(); i++) {
                        if (dValues.get(i) > threshold) {
                            Long blinkTimestamp = timestampValues.get(i);
                            if (blinkTimestamp > lastBlinkTimestamp + 300) {
                                if (labelValues.get(i / 90) - 100 < timestampValues
                                        .get(i)
                                        && timestampValues.get(i) < labelValues
                                                .get(i / 90) + 100) {
                                    // MEMO: adjust to actual blink
                                    peaks.add(labelValues.get(i / 90));
                                } else {
                                    peaks.add(blinkTimestamp);
                                }
                            }
                            lastBlinkTimestamp = blinkTimestamp;
                        }
                    }

                    // Step3.2 evaluation
                    float truePositive = 0.0f;
                    float falsePositive = 0.0f;
                    float falseNegative = 0.0f;
                    float trueNegative = 0.0f;

                    long windowSize = 100;
                    // MEMO: skip first timing
                    long endTime = timestampValues.get(90);
                    while (endTime < timestampValues.get(timestampValues.size()-1)) {
                        boolean isPeak = false;
                        boolean isLabel = false;

                        while (labels.size() > 0
                                && labels.get(0) < endTime) {
                            isLabel = true;
                            labels.remove(0);
                        }
                        while (peaks.size() > 0
                                && peaks.get(0) < endTime) {
                            isPeak = true;
                            peaks.remove(0);
                        }

                        if (isPeak && isLabel) {
                            truePositive += 1.0;
                        } else if (isPeak && !isLabel) {
                            falsePositive += 1.0;
                        } else if (!isPeak && isLabel) {
                            falseNegative += 1.0;
                        } else {
                            trueNegative += 1.0;
                        }

                        endTime += windowSize;
                    }
                    if (truePositive + falsePositive > 0.0 && truePositive + falseNegative > 0) {

                        float precision = truePositive
                                / (truePositive + falsePositive);
                        float recall = truePositive
                                / (truePositive + falseNegative);
                        float fMeasure = (2 * precision * recall)
                                / (precision + recall);

                        if (fMeasure > maxFmeasure) {
                            maxFmeasure = fMeasure;
                            thresholds.clear();
                            thresholds.add(threshold);
                        }else if (fMeasure == maxFmeasure) {
                            thresholds.add(threshold);
                        }
                    }
                    Log.v("Monitoring Activity", "ir-progress threshold:"+threshold + "tp:"+truePositive+" fp:"+falsePositive+" fn:"+falseNegative+" tn:"+trueNegative);
                }
                float threshold = 0.0f;
                for (Float fValue : thresholds) {
                    threshold += fValue;
                }
                threshold = (float) ((Math.round((threshold/thresholds.size()) * 10))/10.0);
                SharedPreferences.Editor edit = mSharedPreferences.edit();
                edit.putFloat("threshold",threshold);
                edit.commit();
                Log.v("Monitoring Activity", "ir-finished threshold:"
                        + threshold + "fMeasure:" + maxFmeasure);
                mThread.interrupt();
                finish();
            }
        };
        mHandler= new Handler();
        mThread.start();
        isRunning = true;
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    @Override
    protected void onPause() {
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        isRunning = false;
        mThread.interrupt();
        mSoundPool.release();
        super.onPause();
    }
}
