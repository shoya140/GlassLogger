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
//    private LoggerService mLoggerService;
    LoggerService serviceBinder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mContext = getBaseContext();
//        mLoggerService = new LoggerService();

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
        if (android.os.Environment.getExternalStorageState().equals(android.os.Environment.MEDIA_MOUNTED)) {
            Intent bindIndent = new Intent(MainActivity.this, LoggerService.class);
            mContext.startService(bindIndent);
            startButton.setText("Stop");
        }else{
            String message = "Logger could not be started because sdcard ist not mounted!";
            Toast.makeText(getBaseContext(), message, Toast.LENGTH_SHORT).show();
        }        
    }

    private void stopRecording(){
        Intent bindIndent = new Intent(MainActivity.this, LoggerService.class);
        mContext.stopService(bindIndent);
        startButton.setText("Start");
    }
    
    private boolean isServiceRunning(){
        return LoggerService.isLogging;
    }
}