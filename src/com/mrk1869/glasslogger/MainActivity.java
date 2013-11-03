package com.mrk1869.glasslogger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import android.os.Bundle;
import android.widget.TextView;
import android.app.Activity;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;

public class MainActivity extends Activity {

    private TextView mTextView;
    //    private boolean mSuperUser;

    //    private Process mShell = null;
    //    private DataInputStream mInputStream = null;
    //    private DataOutputStream mOutputStream = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mTextView = (TextView)findViewById(R.id.sensor_value_text);

        //        try {
        //            mShell = Runtime.getRuntime().exec("sh");
        //            mInputStream = new DataInputStream(mShell.getInputStream());
        //            mOutputStream = new DataOutputStream(mShell.getOutputStream());
        //        } catch (Exception e) {
        //            e.printStackTrace();
        //        }

        //        setSuperUser(true);
        if (appIsInstalled("com.noshufou.android.su")) {
            //            writeStream("/system/bin/ls /system/");
            //            String response = readStream();
            //            mTextView.setText(response);
//            getRootPermission();
            mTextView.setText(getIRSensorData());
        }else{
            mTextView.setText("This device dosen't have root access..");
        }
    }

    //    private String readStream(){
    //        String res = "";
    //        try {
    //            if(mInputStream != null){
    //                byte[] buffer = new byte[64];
    //                int size = mInputStream.read(buffer);
    //                if (0 < size)
    //                    res = new String(buffer, 0, size - 1);
    //            }
    //        } catch (IOException e) {
    //            e.printStackTrace();
    //        }
    //        return res;
    //    }

    //    private void writeStream(String command){
    //        try{
    //            if (mOutputStream == null){
    //                return;
    //            }
    //            mOutputStream.writeBytes(command);
    //            mOutputStream.flush();
    //        }catch (IOException e){
    //            e.printStackTrace();
    //        }
    //    }

    //    private boolean setSuperUser(boolean on){
    //        if(mSuperUser == on){
    //            return true;
    //        }
    //        
    //        if (on) {
    //            writeStream("su\n");
    //            writeStream("whoami\n");
    //            String res = readStream();
    //            mSuperUser = res.equals("root");
    //        }else{
    //            writeStream("exit\n");
    //            mSuperUser = false;
    //        }
    //        return mSuperUser;
    //    }

    private String getIRSensorData() {
        String data = new String();        
        try {
            String[] command = {"su", "-c", "cat /sys/bus/i2c/devices/4-0035/ir"};
            Process process = Runtime.getRuntime().exec(command);
//            Process process = Runtime.getRuntime().exec("cat /sys/bus/i2c/devices/4-0035/ir");
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            int read;
            char[] buffer = new char[4096];
            StringBuffer output = new StringBuffer();
            while ((read = reader.read(buffer)) > 0) {
                output.append(buffer, 0, read);
            }
            reader.close();
            process.waitFor();
            data = output.toString();
        }catch (IOException e){
            throw new RuntimeException(e);
        }catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        return data;
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

    public void getRootPermission() {
        try{
            Process process = Runtime.getRuntime().exec("su");
            process.destroy();
        }catch (IOException e){
            throw new RuntimeException(e);
        }
    }

}