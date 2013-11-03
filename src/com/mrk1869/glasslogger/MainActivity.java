package com.mrk1869.glasslogger;

import android.os.Bundle;
import android.widget.TextView;
import android.app.Activity;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;

public class MainActivity extends Activity {
    
    private TextView mTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mTextView = (TextView)findViewById(R.id.sensor_value_text);
        
        if (!appIsInstalled("com.noshufou.android.su")) {
            mTextView.setText("This device dosen't have root access..");
        }
    }
    
    public boolean appIsInstalled(String PackageName) {
        PackageManager pm = this.getPackageManager();
        try {
          ApplicationInfo ai = pm.getApplicationInfo(PackageName, 0);
        } catch (NameNotFoundException e) {
          return false;
        }
        return true;
      }
    
}