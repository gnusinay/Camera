package com.mercury.gnusin.camera;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import org.androidannotations.annotations.AfterExtras;
import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Click;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.Extra;
import org.androidannotations.annotations.ViewById;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

@EActivity(R.layout.a_save_picture)
public class SavePictureActivity extends Activity {

    @ViewById(R.id.captured_picture)
    ImageView capturedPicture;

    @ViewById(R.id.save_button)
    ImageButton saveButton;

    @ViewById(R.id.not_save_button)
    ImageButton notSaveButton;

    @Extra
    String capturedPictureFilePath;

    private File capturedPictureFile;
    private Bitmap capturedPictureBtm;

    @AfterViews
    void init() {
        Bundle bundle = getIntent().getExtras();
        capturedPictureFilePath = bundle.getString("capturedPictureFilePath");
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

            Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
            mediaScanIntent.setData(Uri.fromFile(saveFile));
            sendBroadcast(mediaScanIntent);
        } catch (IOException e) {
            Toast.makeText(this, "Can not save picture on devaice", Toast.LENGTH_LONG).show();
        } finally {
            capturedPictureFile.delete();
        }
        finish();
    }

    @Click
    void notSaveButton() {
        capturedPictureFile.delete();
        finish();
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
