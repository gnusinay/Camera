package com.mercury.gnusin.camera;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.hardware.Camera;
import android.hardware.SensorManager;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.OrientationEventListener;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Click;
import org.androidannotations.annotations.EFragment;
import org.androidannotations.annotations.ViewById;
import org.androidannotations.annotations.sharedpreferences.Pref;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;


@EFragment(R.layout.f_camera)
public class CaptureFragment extends Fragment {

    public static final String TAG = "CaptureFragment";

    @ViewById(R.id.camera_preview)
    LinearLayout cameraPreview;

    @ViewById(R.id.capture_button)
    ImageButton captureButton;

    @ViewById(R.id.flash_mode_button)
    ImageButton flashModeButton;

    @ViewById(R.id.cameras_switch_button)
    ImageButton camerasSwitchButton;

    @Pref
    MyPrefs_ myPrefs;

    private CustomCamera customCamera;

    private int currentSectorNumber;
    private int currentAngleRotationButtons;
    private BroadcastReceiver capturePictureReceiver;
    private BroadcastReceiver remoteControlReceiver;

    @AfterViews
    void init() {
        currentSectorNumber = 1 + getActivity().getWindowManager().getDefaultDisplay().getRotation();
        currentAngleRotationButtons = 0;

        new OrientationEventListener(getActivity(), SensorManager.SENSOR_DELAY_NORMAL) {
            @Override
            public void onOrientationChanged(int orientation) {
                if (customCamera != null) {
                    int toSectorNumber = ScreenRotationHelper.getSectorNumber(orientation);
                    if (toSectorNumber != -1 && currentSectorNumber != toSectorNumber) {
                        currentAngleRotationButtons = ScreenRotationHelper.calcNewAngleRotation(currentSectorNumber, toSectorNumber, currentAngleRotationButtons);
                        rotateButtons(700, currentAngleRotationButtons);
                        currentSectorNumber = toSectorNumber;
                    }
                }
            }
        }.enable();


        capturePictureReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String filePath = saveCapturedBitmapToFile();
                Intent captureIntent = new Intent(CameraActivity.CAPTURE_EVENT);
                captureIntent.putExtra("value", filePath);
                LocalBroadcastManager.getInstance(context).sendBroadcast(captureIntent);
            }
        };
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(capturePictureReceiver, new IntentFilter(CustomCamera.CAMERA_CAPTURE_EVENT));


    }


    @Override
    public void onResume() {
        super.onResume();
        customCamera = initCamera();
        getView().setFocusableInTouchMode(true);
        getView().requestFocus();
        getView().setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (event.getKeyCode() == KeyEvent.KEYCODE_VOLUME_DOWN || event.getKeyCode() == KeyEvent.KEYCODE_VOLUME_UP) {
                    if (event.getAction() == KeyEvent.ACTION_DOWN) {
                        customCamera.takePicture();
                    }
                    return true;
                }

                return false;
            }
        });
    }


    @Override
    public void onPause() {
        super.onPause();
        customCamera.release();
        customCamera = null;
    }

    @Click
    void camerasSwitchButton() {
        int cameraId = myPrefs.camerasSwitch().get();
        cameraId = cameraId == 0 ? 1 : 0;
        myPrefs.camerasSwitch().put(cameraId);
        customCamera.release();
        customCamera = initCamera();
    }



    @Click
    void flashModeButton() {
        int mode = myPrefs.flashMode().get();
        mode = mode == 2 ? 0 : ++mode;

        switch(mode) {
            case 0:
                customCamera.setFlashMode(Camera.Parameters.FLASH_MODE_AUTO);
                flashModeButton.setImageResource(R.mipmap.ic_flash_auto);
                break;
            case 1:
                customCamera.setFlashMode(Camera.Parameters.FLASH_MODE_ON);
                flashModeButton.setImageResource(R.mipmap.ic_flash_on);
                break;
            case 2:
                customCamera.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
                flashModeButton.setImageResource(R.mipmap.ic_flash_off);
                break;
        }

        myPrefs.flashMode().put(mode);

    }


    @Click
    void captureButton() {
        customCamera.takePicture();
    }

    @Click
    void cameraPreview() {
       // customCamera.setFocusMode(Camera.Parameters.FOCUS_MODE_FIXED);
    }

    @Override
    public void onDestroy() {
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(capturePictureReceiver);
        super.onDestroy();
    }

    private CustomCamera initCamera() {
        CustomCamera camera = null;
        try {
            if (Camera.getNumberOfCameras() > 1) {
                switch (myPrefs.camerasSwitch().get()) {
                    case 0:
                        camerasSwitchButton.setImageResource(R.mipmap.ic_camera_rear);
                        break;
                    case 1:
                        camerasSwitchButton.setImageResource(R.mipmap.ic_camera_front);
                        break;
                }
            } else {
                camerasSwitchButton.setVisibility(View.INVISIBLE);
            }

            camera = new CustomCamera(getActivity(), myPrefs.camerasSwitch().get());

            camera.setPictureSize(2000, 1000);
            camera.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
            if (camera.isSupportedFlash()) {
                switch (myPrefs.flashMode().get()) {
                    case 0:
                        camera.setFlashMode(Camera.Parameters.FLASH_MODE_AUTO);
                        flashModeButton.setImageResource(R.mipmap.ic_flash_auto);
                        break;
                    case 1:
                        camera.setFlashMode(Camera.Parameters.FLASH_MODE_ON);
                        flashModeButton.setImageResource(R.mipmap.ic_flash_on);
                        break;
                    case 2:
                        camera.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
                        flashModeButton.setImageResource(R.mipmap.ic_flash_off);
                        break;
                }
                flashModeButton.setVisibility(View.VISIBLE);
            } else {
                flashModeButton.setVisibility(View.INVISIBLE);
            }

            cameraPreview.removeAllViews();
            cameraPreview.addView(camera.getSurfaceView());
        } catch (Exception e) {
            Intent intent = new Intent(CameraActivity.ERROR_EVENT);
            intent.putExtra("value", "Failed initialization camera");
            LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(intent);
        }

        return camera;
    }


    private void rotateButtons(int duration, int angle) {
        captureButton.animate().setDuration(duration).rotation(angle);
        camerasSwitchButton.animate().setDuration(duration).rotation(angle);
        flashModeButton.animate().setDuration(duration).rotation(angle);
    }

    private String saveCapturedBitmapToFile() {
        File pictureTempFile = null;
        FileOutputStream fos = null;
        try {
            Bitmap bitmap = customCamera.getCapturedPicture();
            Bitmap rotatedBitmap = ScreenRotationHelper.rotateCapturedPicture(bitmap, currentSectorNumber);
            pictureTempFile = getOutputMediaTempFile();
            fos = new FileOutputStream(pictureTempFile);
            rotatedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
            fos.close();
        } catch (IOException e) {
            Intent intent = new Intent(CameraActivity.ERROR_EVENT);
            intent.putExtra("value", "Can not save captured picture");
            LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(intent);
            return "";
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                }
            }
        }

        return pictureTempFile.getAbsolutePath().toString();
    }


    private File getOutputMediaTempFile() throws IOException {
        String tempFileName = String.format("temp_img_from_camera_%s", new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date()));
        return File.createTempFile(tempFileName, null, null);
    }
}

