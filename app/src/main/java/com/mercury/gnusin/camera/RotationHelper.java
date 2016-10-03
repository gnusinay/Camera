package com.mercury.gnusin.camera;

import android.graphics.Bitmap;
import android.graphics.Matrix;

/**
 * Created by gnusin on 03.10.2016.
 */
public class RotationHelper {
    static int getSectorNumber(int degrees) {
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

    static int calcNewAngleRotation(int fromSectorNumber, int toSectorNumber, int currentAngle) {
        int degrees = 0;
        if (fromSectorNumber == 4 && toSectorNumber == 1) {
            degrees = 90;
        } else if (fromSectorNumber == 1 && toSectorNumber == 4) {
            degrees = -90;
        } else {
            int delta = toSectorNumber - fromSectorNumber;
            degrees = delta * 90;
        }

        return currentAngle + degrees;
    }

    static Bitmap rotateCapturedPicture(Bitmap picture, int currentSector) {
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
}
