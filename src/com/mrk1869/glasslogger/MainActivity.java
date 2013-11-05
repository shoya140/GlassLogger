package com.mrk1869.glasslogger;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;

public class MainActivity extends Activity{

    private Button startButton;
    private Context mContext;
    LoggerService serviceBinder;
    IRSensorLogger irSensorLogger;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mContext = getBaseContext();

        startButton = (Button)findViewById(R.id.start_button);
        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isServiceRunning()) {
                    stopRecording();
                }else{
                    startRecording();
                }
            }
        });
    }
    private void startRecording(){
        if (isInstallationFinished()) {
            if (android.os.Environment.getExternalStorageState().equals(android.os.Environment.MEDIA_MOUNTED)) {
                Intent bindIndent = new Intent(MainActivity.this, LoggerService.class);
                mContext.startService(bindIndent);
                startButton.setText("Stop recording");
                startButton.setTextColor(0xcd2e2e);
            }else{
                Toast.makeText(getBaseContext(), "Error: Storage does not have enough space.", Toast.LENGTH_SHORT).show();
            }
        }
        Toast.makeText(getBaseContext(), "Error: Permission denied.", Toast.LENGTH_SHORT).show();
    }

    private void stopRecording(){
        Intent bindIndent = new Intent(MainActivity.this, LoggerService.class);
        mContext.stopService(bindIndent);
        startButton.setText("Start recording");
        startButton.setTextColor(0xffffff);
    }

    private boolean isServiceRunning(){
        return LoggerService.isLogging;
    }

    private boolean isInstallationFinished(){
        try {
            irSensorLogger.getIRSensorData();
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}