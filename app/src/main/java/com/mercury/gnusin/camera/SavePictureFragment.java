package com.mercury.gnusin.camera;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.widget.ImageButton;
import android.widget.ImageView;
import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Click;
import org.androidannotations.annotations.EFragment;
import org.androidannotations.annotations.ViewById;
import java.io.File;
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


    private File capturedPictureFile;
    private Bitmap capturedPictureBtm;

    @AfterViews
    void init() {
        Bundle bundle = getArguments();
        String capturedPictureFilePath = bundle.getString("value");
        if (!capturedPictureFilePath.isEmpty()) {
            capturedPictureFile = new File(capturedPictureFilePath);

            if (capturedPictureFile.exists()) {
                capturedPictureBtm = BitmapFactory.decodeFile(capturedPictureFile.getAbsolutePath());
                capturedPicture.setImageBitmap(capturedPictureBtm);
            }
        }
    }


    @Click
    void saveButton() {
        try {
            File saveFile = getOutputMediaPublicFile();
            FileOutputStream outputStream = new FileOutputStream(saveFile);
            capturedPictureBtm.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
            outputStream.close();

            Intent saveIntent = new Intent(CameraActivity.SAVE_EVENT);
            saveIntent.putExtra("value", Uri.fromFile(saveFile).toString());
            LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(saveIntent);
        } catch (IOException e) {
            Intent errorIntent = new Intent(CameraActivity.ERROR_EVENT);
            errorIntent.putExtra("value", "Can not save picture on device." + e.getMessage());
            LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(errorIntent);
        } finally {
            capturedPictureFile.delete();
        }
    }


    @Click
    void notSaveButton() {
        capturedPictureFile.delete();
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
}
