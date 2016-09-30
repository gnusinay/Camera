package com.mercury.gnusin.camera;

import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.hardware.Camera;
import android.hardware.SensorManager;
import android.util.Log;
import android.view.OrientationEventListener;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Toast;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Click;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.ViewById;
import org.androidannotations.annotations.sharedpreferences.Pref;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

@EActivity(R.layout.a_camera)
public class CameraActivity extends Activity implements SurfaceHolder.Callback, Camera.PictureCallback {

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

    private Camera mCamera;
    private SurfaceHolder mPreviewHolder;
    private int currentSectorNumber = 1;

    static int counter = 0;

    private int id;

    @AfterViews
    void init() {
        id = ++counter;
        Log.d("AGn", "Init activity, id - " + id);

        currentSectorNumber = currentSectorNumber + getWindowManager().getDefaultDisplay().getRotation();

        new OrientationEventListener(this, SensorManager.SENSOR_DELAY_NORMAL) {
            @Override
            public void onOrientationChanged(int orientation) {
                int toSectorNumber = getSectorNumber(orientation);
                if (toSectorNumber != -1 && currentSectorNumber != toSectorNumber) {
                    rotateButtons(currentSectorNumber, toSectorNumber);
                    currentSectorNumber = toSectorNumber;
                }
            }
        }.enable();
    }

    @Override
    protected void onResume() {
        super.onResume();
        boolean isInitial = initCamera();

        if (!isInitial) {
            Toast.makeText(this, "Failed initialization camera", Toast.LENGTH_LONG).show();
        }

        Log.d("AGn", "Resume activity, id - " + id);
    }

    @Override
    protected void onPause() {
        super.onPause();
        releaseCamera();
        Log.d("AGn", "Pause activity, id - " + id);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d("AGn", "Destroy activity, id - " + id);
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
        mCamera.takePicture(null, null, this);
    }


    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        Log.d("AGn", "surfaceCreated, activity id - " + id);

        if (mCamera != null) {
            try {
                mCamera.setPreviewDisplay(mPreviewHolder);
                mCamera.startPreview();
            } catch (IOException e) {
                if (mCamera != null) {
                    mCamera.release();
                    mCamera = null;
                }
            }
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        Log.d("AGn", "surfaceDestroyed, activity id - " + id);
    }

    @Override
    public void onPictureTaken(byte[] data, Camera camera) {
        File pictureTempFile = null;
        FileOutputStream fos = null;
        try {
            Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length - 1);
            Bitmap rotatedBitmap = rotateCapturedPicture(bitmap, currentSectorNumber);
            pictureTempFile = getOutputMediaTempFile();
            fos = new FileOutputStream(pictureTempFile);
            rotatedBitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
            fos.close();
        } catch (IOException e) {
            Toast.makeText(this, "Can not save captured picture", Toast.LENGTH_LONG).show();
            return;
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                }
            }
        }

        SavePictureActivity_.intent(this).capturedPictureFilePath(pictureTempFile.getAbsolutePath()).start();
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

            mCamera = Camera.open(myPrefs.camerasSwitch().get());

            SurfaceView surfaceView = new SurfaceView(this);
            mPreviewHolder = surfaceView.getHolder();
            mPreviewHolder.addCallback(this);
            cameraPreview.removeAllViews();
            cameraPreview.addView(surfaceView);

            Camera.Parameters params = mCamera.getParameters();

            List<String> flashModes = params.getSupportedFlashModes();
            if (flashModes != null && flashModes.containsAll(Arrays.asList(Camera.Parameters.FLASH_MODE_AUTO, Camera.Parameters.FLASH_MODE_OFF, Camera.Parameters.FLASH_MODE_ON))) {
                flashModeButton.setVisibility(View.VISIBLE);
                changeFlashMode(myPrefs.flashMode().get());
            } else {
                flashModeButton.setVisibility(View.INVISIBLE);
            }

            List<String> focusMode = params.getSupportedFocusModes();
            if (focusMode != null && focusMode.contains(Camera.Parameters.FOCUS_MODE_AUTO)) {
                params.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
            }

            mCamera.setParameters(params);
        } catch (Exception e) {
            if (mCamera != null) {
                mCamera.release();
                mCamera = null;
            }
            return false;
        }

        return true;
    }

    private void releaseCamera() {
        if (mCamera != null) {
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
        }
    }


    private void changeFlashMode(int mode) {
        Camera.Parameters params = mCamera.getParameters();
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
        mCamera.setParameters(params);
    }

    private int getSectorNumber(int degrees) {
        if (degrees >= 330 || degrees < 30) {
            return 1;
        } else if (degrees >= 240 && degrees < 300) {
            return 2;
        } else if (degrees >= 150 && degrees < 210) {
            return 3;
        } else if (degrees >= 60 && degrees < 120) {
            return 4;
        } else {
            return -1;
        }
    }

    private void rotateButtons(int fromSectorNumber, int toSectorNumber) {
        int newDegrees = 0;
        int oldDegrees = (int) captureButton.getRotation();
        if (fromSectorNumber == 4 && toSectorNumber == 1) {
            newDegrees = oldDegrees + 90;
        } else if (fromSectorNumber == 1 && toSectorNumber == 4) {
            newDegrees = oldDegrees - 90;
        } else {
            int delta = toSectorNumber - fromSectorNumber;
            newDegrees = oldDegrees + delta * 90;
        }

        captureButton.setRotation(newDegrees);
        camerasSwitchButton.setRotation(newDegrees);
        flashModeButton.setRotation(newDegrees);
    }

    private Bitmap rotateCapturedPicture(Bitmap picture, int currentSector) {
        //currentSector = currentSector - getWindowManager().getDefaultDisplay().getRotation();
        int degrees = 0;//getWindowManager().getDefaultDisplay().getRotation();
        switch (currentSector) {
            case 1:
                degrees = 90;
                break;
            case 2:
                degrees = 0;
                break;
            case 3:
                degrees = 180;
                break;
            case 4:
                degrees = 270;
                break;
        }
        Matrix matrix = new Matrix();
        matrix.postRotate(degrees);
        return Bitmap.createBitmap(picture, 0, 0, picture.getWidth(), picture.getHeight(), matrix, true);

       // mCamera.set(degrees);

    }

    private File getOutputMediaTempFile() throws IOException {
        String tempFileName = String.format("temp_img_from_camera_%s", new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date()));
        //File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        return File.createTempFile(tempFileName, null, null);
    }
}


