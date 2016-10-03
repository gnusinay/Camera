package com.mercury.gnusin.camera;

import org.androidannotations.annotations.sharedpreferences.DefaultBoolean;
import org.androidannotations.annotations.sharedpreferences.DefaultInt;
import org.androidannotations.annotations.sharedpreferences.SharedPref;



@SharedPref
public interface MyPrefs {

    @DefaultInt(0)
    int flashMode();          // 0 - auto, 1 - on, 2 - off

    @DefaultInt(0)
    int camerasSwitch();      // 0 - rear camera, 1 - front camera

    @DefaultBoolean(true)
    boolean isCaptureFragment();

    long lastUpdated();
}
