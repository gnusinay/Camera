package com.mercury.gnusin.camera;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Camera;
import android.hardware.SensorManager;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.view.OrientationEventListener;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
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
import java.util.Arrays;
import java.util.Date;
import java.util.List;


@EFragment(R.layout.f_camera)
public class CaptureFragment extends Fragment implements SurfaceHolder.Callback, Camera.PictureCallback {

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

    private Camera camera;
    private SurfaceHolder previewHolder;

    private int currentSectorNumber;
    private int currentAngleRotationButtons;
    private boolean checkOrientation;

    @AfterViews
    void init() {
        currentSectorNumber = 1 + getActivity().getWindowManager().getDefaultDisplay().getRotation();
        currentAngleRotationButtons = 0;
        checkOrientation = true;

        new OrientationEventListener(getActivity(), SensorManager.SENSOR_DELAY_NORMAL) {
            @Override
            public void onOrientationChanged(int orientation) {
                if (checkOrientation) {
                    int toSectorNumber = RotationHelper.getSectorNumber(orientation);
                    if (toSectorNumber != -1 && currentSectorNumber != toSectorNumber) {
                        currentAngleRotationButtons = RotationHelper.calcNewAngleRotation(currentSectorNumber, toSectorNumber, currentAngleRotationButtons);
                        rotateButtons(1000, currentAngleRotationButtons);
                        currentSectorNumber = toSectorNumber;
                    }
                }
            }
        }.enable();
    }


    @Override
    public void onResume() {
        super.onResume();
        boolean isInitial = initCamera();

        if (!isInitial) {
            Intent intent = new Intent(CameraActivity.ERROR_EVENT);
            intent.putExtra("value", "Failed initialization camera");
            LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(intent);
        }
        checkOrientation = true;
    }


    @Override
    public void onPause() {
        super.onPause();
        releaseCamera();
        checkOrientation = false;
    }

    @Click
    void camerasSwitchButton() {
        int cameraId = myPrefs.camerasSwitch().get();
        cameraId = cameraId == 0 ? 1 : 0;
        myPrefs.camerasSwitch().put(cameraId);

        releaseCamera();
        initCamera();
    }


    @Click
    void flashModeButton() {
        int mode = myPrefs.flashMode().get();
        mode = mode == 2 ? 0 : ++mode;
        myPrefs.flashMode().put(mode);
        changeFlashMode(mode);
    }

    @Click
    void captureButton() {
        camera.takePicture(null, null, this);
    }


    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        if (camera != null) {
            try {
                camera.setPreviewDisplay(previewHolder);
                camera.startPreview();
            } catch (IOException e) {
                releaseCamera();
            }
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {

    }

    @Override
    public void onPictureTaken(byte[] data, Camera camera) {
        File pictureTempFile = null;
        FileOutputStream fos = null;
        try {
            Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length - 1);
            Bitmap rotatedBitmap = RotationHelper.rotateCapturedPicture(bitmap, currentSectorNumber);
            pictureTempFile = getOutputMediaTempFile();
            fos = new FileOutputStream(pictureTempFile);
            rotatedBitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
            fos.close();
        } catch (IOException e) {
            Intent intent = new Intent(CameraActivity.ERROR_EVENT);
            intent.putExtra("value", "Can not save captured picture");
            LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(intent);
            return;
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                }
            }
        }

        Intent captureIntent = new Intent(CameraActivity.CAPTURE_EVENT);
        captureIntent.putExtra("value", pictureTempFile.getAbsoluteFile().toString());
        LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(captureIntent);
    }

    private boolean initCamera() {
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

            camera = Camera.open(myPrefs.camerasSwitch().get());

            SurfaceView surfaceView = new SurfaceView(getActivity());
            previewHolder = surfaceView.getHolder();
            previewHolder.addCallback(this);
            cameraPreview.removeAllViews();
            cameraPreview.addView(surfaceView);

            Camera.Parameters params = camera.getParameters();
            List<String> focusMode = params.getSupportedFocusModes();
            if (focusMode != null && focusMode.contains(Camera.Parameters.FOCUS_MODE_AUTO)) {
                params.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
                camera.setParameters(params);
            }

            List<String> flashModes = params.getSupportedFlashModes();
            if (flashModes != null && flashModes.containsAll(Arrays.asList(Camera.Parameters.FLASH_MODE_AUTO, Camera.Parameters.FLASH_MODE_OFF, Camera.Parameters.FLASH_MODE_ON))) {
                flashModeButton.setVisibility(View.VISIBLE);
                changeFlashMode(myPrefs.flashMode().get());
            } else {
                flashModeButton.setVisibility(View.INVISIBLE);
            }
        } catch (Exception e) {
            if (camera != null) {
                camera.release();
                camera = null;
            }
            return false;
        }

        return true;
    }

    private void releaseCamera() {
        if (camera != null) {
            camera.stopPreview();
            camera.release();
            camera = null;
            previewHolder = null;
        }
    }

    private void changeFlashMode(int mode) {
        Camera.Parameters params = camera.getParameters();
        switch (mode) {
            case 0:
                flashModeButton.setImageResource(R.mipmap.ic_flash_auto);
                params.setFlashMode(Camera.Parameters.FLASH_MODE_AUTO);
                break;
            case 1:
                flashModeButton.setImageResource(R.mipmap.ic_flash_on);
                params.setFlashMode(Camera.Parameters.FLASH_MODE_ON);
                break;
            case 2:
                flashModeButton.setImageResource(R.mipmap.ic_flash_off);
                params.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
                break;
        }
        camera.setParameters(params);
    }


    private void rotateButtons(int duration, int angle) {
        captureButton.animate().setDuration(duration).rotation(angle);
        camerasSwitchButton.animate().setDuration(duration).rotation(angle);
        flashModeButton.animate().setDuration(duration).rotation(angle);
    }


    private File getOutputMediaTempFile() throws IOException {
        String tempFileName = String.format("temp_img_from_camera_%s", new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date()));
        return File.createTempFile(tempFileName, null, null);
    }
}

