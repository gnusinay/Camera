package com.mercury.gnusin.camera;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Camera;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import java.io.IOException;
import java.util.List;


public class CustomCamera implements SurfaceHolder.Callback, Camera.PictureCallback {

    public static final String CAMERA_CAPTURE_EVENT = "CAMERA_CAPTURE_EVENT";

    private Context context;
    private Camera camera;
    private SurfaceView surfaceView;
    private Bitmap capturedPictureBitmap;

    public CustomCamera(Context context, int cameraID) throws Exception {
        this.context = context;
        camera = Camera.open(cameraID);
        surfaceView = new SurfaceView(context);
        surfaceView.getHolder().addCallback(this);
    }

    public void release() {
        if (camera != null) {
            camera.stopPreview();
            camera.release();
            camera = null;
        }
    }

    public SurfaceView getSurfaceView() {
        return surfaceView;
    }

    public void setFocusMode(String focusMode) {
        if (!focusMode.isEmpty()) {
            Camera.Parameters params = camera.getParameters();
            params.setFocusMode(focusMode);
            camera.setParameters(params);
        }
    }

    public boolean isSupportedFlash() {
        List<String> modes = camera.getParameters().getSupportedFlashModes();
        return modes != null && modes.size() > 0;
    }

    public void setFlashMode(String flashMode) {
        if (!flashMode.isEmpty()) {
            Camera.Parameters params = camera.getParameters();
            params.setFlashMode(flashMode);
            camera.setParameters(params);
        }
    }

    public void takePicture() {
        camera.takePicture(null, null, this);
    }

    public void setPictureSize(int width, int height) {
        Camera.Size size = null;
        Camera.Parameters params = camera.getParameters();
        List<Camera.Size> sizes = params.getSupportedPictureSizes();
        float sidesRatio = getPreviewSidesRatio();
        for (int i = 0; i < sizes.size(); i++) {
            float tRatio = Math.round(sizes.get(i).width * 10f / sizes.get(i).height) / 10f;
            if (sidesRatio > tRatio - 0.2f && sidesRatio < tRatio + 0.2f && sizes.get(i).width > width && sizes.get(i).height > height) {
                size = sizes.get(i);
            }
        }
        if (size == null) {
            for (int i = 0; i < sizes.size(); i++) {
                float tRatio = Math.round(sizes.get(i).width * 10f / sizes.get(i).height) / 10f;
                if (sidesRatio > tRatio - 0.2f && sidesRatio < tRatio + 0.2f && sizes.get(i).width > width) {
                    size = sizes.get(i);
                }
            }
        }
        params.setPictureSize(size.width, size.height);
        camera.setParameters(params);
    }

    public Bitmap getCapturedPicture() {
        return capturedPictureBitmap;
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        if (camera != null) {
            try {
                camera.setPreviewDisplay(surfaceView.getHolder());
                camera.startPreview();
            } catch (IOException e) {
                release();
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
        capturedPictureBitmap = BitmapFactory.decodeByteArray(data, 0, data.length - 1);
        Intent captureIntent = new Intent(CustomCamera.CAMERA_CAPTURE_EVENT);
        LocalBroadcastManager.getInstance(context).sendBroadcast(captureIntent);
    }


    private float getPreviewSidesRatio() {
        Camera.Size previewSize = camera.getParameters().getPreviewSize();
        return Math.round(previewSize.width * 10f / previewSize.height) / 10f;
    }
}
