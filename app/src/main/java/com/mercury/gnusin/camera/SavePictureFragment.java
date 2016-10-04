package com.mercury.gnusin.camera;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.Display;
import android.widget.ImageButton;
import android.widget.ImageView;
import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Click;
import org.androidannotations.annotations.EFragment;
import org.androidannotations.annotations.ViewById;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;


@EFragment(R.layout.f_save_picture)
public class SavePictureFragment extends Fragment {

    public static final String TAG = "SavePictureFragment";

    @ViewById(R.id.captured_picture)
    ImageView capturedPicture;

    @ViewById(R.id.save_button)
    ImageButton saveButton;

    @ViewById(R.id.not_save_button)
    ImageButton notSaveButton;


    private String capturedPictureFilePath;

    @AfterViews
    void init() {
        Bundle bundle = getArguments();
        Log.d("AGn", "SavePictureFragment init() capturedPictureFilePath - " + String.valueOf(capturedPictureFilePath == null));
        capturedPictureFilePath = bundle.getString("value");
        if (!capturedPictureFilePath.isEmpty()) {
            capturedPicture.setImageBitmap(scaleBitmapForScreen(capturedPictureFilePath));
        } else {
            deleteCapturedPictureTempFile(capturedPictureFilePath);
            Intent intent = new Intent(CameraActivity.ERROR_EVENT);
            intent.putExtra("value", "Can not open captured picture");
            LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(intent);
        }
    }


    @Click
    void saveButton() {
        FileOutputStream outputStream = null;
        FileInputStream inputStream = null;
        try {
            File saveFile = getOutputMediaPublicFile();
            outputStream = new FileOutputStream(saveFile);
            File tempFile = new File(capturedPictureFilePath);
            inputStream = new FileInputStream(tempFile);

            byte[] buffer = new byte[1024];
            int byteCount = inputStream.read(buffer);
            while (byteCount > 0) {
                outputStream.write(buffer, 0, byteCount);
                byteCount = inputStream.read(buffer);
            }

            Intent saveIntent = new Intent(CameraActivity.SAVE_EVENT);
            saveIntent.putExtra("value", Uri.fromFile(saveFile).toString());
            LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(saveIntent);
        } catch (IOException e) {
            Intent errorIntent = new Intent(CameraActivity.ERROR_EVENT);
            errorIntent.putExtra("value", "Can not save picture on device." + e.getMessage());
            LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(errorIntent);
        } finally {
            try {
                inputStream.close();
                outputStream.close();
            } catch (IOException e) {
            }
            deleteCapturedPictureTempFile(capturedPictureFilePath);
        }
    }


    @Click
    void notSaveButton() {
        deleteCapturedPictureTempFile(capturedPictureFilePath);
        Intent intent = new Intent(CameraActivity.CANCEL_EVENT);
        LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(intent);
    }


    private File getOutputMediaPublicFile() throws FileNotFoundException {
        File directory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        if (directory.exists()) {
            File file = new File(directory, String.format("IMG_%s.jpg", new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date())));
            return file;
        } else {
            throw new FileNotFoundException("Directory for pictures is not found");
        }
    }

    private Bitmap scaleBitmapForScreen(String filePath) {
        Display display = getActivity().getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(filePath, options);
        int widthBtn = options.outWidth;
        int heightBtn = options.outHeight;
        int scaleFactor = Math.min(widthBtn/size.x, heightBtn/size.y);
        options.inJustDecodeBounds = false;
        options.inSampleSize = scaleFactor;
        options.inPurgeable = true;

        return BitmapFactory.decodeFile(filePath, options);
    }

    private void deleteCapturedPictureTempFile(String filePath) {
        if (!filePath.isEmpty()) {
            File file = new File(filePath);
            if (file != null) {
                file.delete();
            }
        }
    }
}

