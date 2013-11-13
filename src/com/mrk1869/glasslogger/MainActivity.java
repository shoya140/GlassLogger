package com.mrk1869.glasslogger;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.Toast;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;

public class MainActivity extends Activity{

    private Context mContext;
    LoggerService serviceBinder;
    IRSensorLogger irSensorLogger;

    private boolean mJustSelected;
    private View backgroundView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LinearLayout layout = new LinearLayout(this);
        setContentView(layout);
        backgroundView =  new View(this);
        layout.addView(backgroundView);
        backgroundView.setBackgroundColor(0xff000000);
        mContext = getBaseContext();
    }

    @Override
    protected void onResume(){
        super.onResume();
        openOptionsMenu();
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu){
        super.onPrepareOptionsMenu(menu);
        MenuItem startMenuItem = (MenuItem)menu.findItem(R.id.start_recording);
        MenuItem stopMenuItem = (MenuItem)menu.findItem(R.id.stop_recording);
        if (isServiceRunning()) {
            startMenuItem.setVisible(false);
            stopMenuItem.setVisible(true);
//            backgroundView.setBackgroundColor(0xff000000);
        }else{
            startMenuItem.setVisible(true);
            stopMenuItem.setVisible(false);
//            backgroundView.setBackgroundColor(0xffcd2e2e);
        }
        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.start_recording:
            startRecording();
//            backgroundView.setBackgroundColor(0xff000000);
            mJustSelected = true;
            return false;
        case R.id.stop_recording:
//            backgroundView.setBackgroundColor(0xffcd2e2e);
            stopRecording();
            mJustSelected = true;
            return false;
        case R.id.monitoring:
            Intent intent = new Intent(MainActivity.this, MonitoringActivity.class);
            startActivity(intent);
            mJustSelected = false;
            return false;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onOptionsMenuClosed(Menu menu) {
        if (mJustSelected) {
            //FIXME: need to wait a bit
            Handler h = new Handler(Looper.getMainLooper());
            h.post(new Runnable() {
                @Override
                public void run() {
                    openOptionsMenu();
                }
            });
            mJustSelected = false;
        } else {
            // User dismissed so back out completely
            // FIXME: right now, calling finish here doesn't seem to let us re-enable the receiver
            finish();
        }
    }

    private void startRecording(){
        if(isInstallationFinished()){
            if (android.os.Environment.getExternalStorageState().equals(android.os.Environment.MEDIA_MOUNTED)) {
                getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                Intent bindIndent = new Intent(MainActivity.this, LoggerService.class);
                mContext.startService(bindIndent);
            }else{
                Toast.makeText(getBaseContext(), "Error: Storage does not have enough space.", Toast.LENGTH_SHORT).show();
            }
        }else{
            Toast.makeText(getBaseContext(), "Error: permission error.", Toast.LENGTH_SHORT).show();
        }
    }

    private void stopRecording(){
        Intent bindIndent = new Intent(MainActivity.this, LoggerService.class);
        mContext.stopService(bindIndent);
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    private boolean isServiceRunning(){
        return LoggerService.isLogging;
    }
    
    @Override
    protected void onPause() {
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        super.onPause();
    }

    private boolean isInstallationFinished(){
        //TODO Check whether App has root permission.
        return true;
    }
}