package com.mvivekanandji.mocklocationdetector.interfaces;

import android.content.pm.ApplicationInfo;

import java.util.List;

public interface OnMockLocationDetectorAppListener {

    /**
     *
     * @param mockAppAvailable boolean
     */
    void onResult(boolean mockAppAvailable);

    /**
     *
     * @param exception Exception
     */
    void onError(Exception exception);
}
