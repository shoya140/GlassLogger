package com.mrk1869.glasslogger;

import android.os.Bundle;
import android.widget.TextView;
import android.app.Activity;

public class MainActivity extends Activity{

    private TextView mTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mTextView = (TextView)findViewById(R.id.sensor_value_text);
        
        IRSensorLogger mIrSensorLogger = new IRSensorLogger();
        Thread mThread = new Thread(mIrSensorLogger);
        mThread.start();
    }
    
}