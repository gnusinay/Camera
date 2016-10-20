package com.mercury.gnusin.camera;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.sharedpreferences.Pref;
import java.util.ArrayList;
import java.util.List;

@EActivity
public class CameraActivity extends AppCompatActivity {

    public static final String CAPTURE_EVENT = "CAPTURE_EVENT";
    public static final String SAVE_EVENT = "SAVE_EVENT";
    public static final String CANCEL_EVENT = "CANCEL_EVENT";
    public static final String ERROR_EVENT = "ERROR_EVENT";

    @Pref
    MyPrefs_ myPrefs;

    private List<BroadcastReceiver> receiverList = new ArrayList<>(4);

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (Build.VERSION.SDK_INT < 16) {
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        } else {
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN);
        }

        if (savedInstanceState == null || myPrefs.isCaptureFragment().get()) {
            loadCaptureFragment();
        }

        receiverList = initBroadcastReceivers();
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        for (BroadcastReceiver receiver : receiverList) {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(receiver);
        }
    }


    private void loadCaptureFragment() {
        myPrefs.isCaptureFragment().put(true);

        CaptureFragment captureFragment = (CaptureFragment) getSupportFragmentManager().findFragmentByTag(CaptureFragment.TAG);
        if (captureFragment == null) {
            captureFragment = new CaptureFragment_();
        }
        FragmentTransaction tran = getSupportFragmentManager().beginTransaction();
        tran.replace(android.R.id.content, captureFragment, CaptureFragment.TAG);
        tran.commit();

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
    }


    private void loadSavePictureFragment(String filePath) {
        myPrefs.isCaptureFragment().put(false);
        SavePictureFragment savePictureFragment = (SavePictureFragment) getSupportFragmentManager().findFragmentByTag(SavePictureFragment.TAG);
        Log.d("AGn", getSupportFragmentManager().toString() + " - savePictureFragment - " + savePictureFragment);
        if (savePictureFragment == null) {
            savePictureFragment = new SavePictureFragment_();
            Bundle bundle = new Bundle();
            bundle.putString("value", filePath);
            savePictureFragment.setArguments(bundle);
        }
        FragmentTransaction tran = getSupportFragmentManager().beginTransaction();
        tran.replace(android.R.id.content, savePictureFragment, SavePictureFragment.TAG);
        tran.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
        tran.commit();

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
    }


    private List<BroadcastReceiver> initBroadcastReceivers() {
        List<BroadcastReceiver> resultList = new ArrayList<>(4);

        // CAPTURE_EVENT
        BroadcastReceiver captureReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String filePath = intent.getStringExtra("value");
                loadSavePictureFragment(filePath);
            }
        };
        resultList.add(captureReceiver);
        LocalBroadcastManager.getInstance(this).registerReceiver(captureReceiver, new IntentFilter(CAPTURE_EVENT));

        // SAVE_EVENT
        BroadcastReceiver saveReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String fileUri = intent.getStringExtra("value");
                Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                mediaScanIntent.setData(Uri.parse(fileUri));
                sendBroadcast(mediaScanIntent);
                loadCaptureFragment();
            }
        };
        resultList.add(saveReceiver);
        LocalBroadcastManager.getInstance(this).registerReceiver(saveReceiver, new IntentFilter(SAVE_EVENT));

        // CANCEL_EVENT
        BroadcastReceiver cancelReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                loadCaptureFragment();
            }
        };
        resultList.add(cancelReceiver);
        LocalBroadcastManager.getInstance(this).registerReceiver(cancelReceiver, new IntentFilter(CANCEL_EVENT));

        // ERROR_EVENT
        BroadcastReceiver errorReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String message = intent.getStringExtra("value");
                Toast.makeText(CameraActivity.this, message, Toast.LENGTH_LONG).show();
                if (!myPrefs.isCaptureFragment().get()) {
                    loadCaptureFragment();
                }
            }
        };
        resultList.add(errorReceiver);
        LocalBroadcastManager.getInstance(this).registerReceiver(errorReceiver, new IntentFilter(ERROR_EVENT));

        return resultList;
    }
}
