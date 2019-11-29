package com.mvivekanandji.mocklocationdetector.interfaces;

import android.content.pm.ApplicationInfo;

import java.util.List;

public interface OnMockLocationDetectorAppInfoListener {

    /**
     *
     * @param mockAppAvailable boolean
     * @param mockAppCount int
     * @param mockAppNames List<String>
     * @param mockAppPackages List<String>
     * @param mockAppApplicationInfos List<ApplicationInfo>
     */
    void onResult(boolean mockAppAvailable,
                  int mockAppCount,
                  List<String> mockAppNames,
                  List<String> mockAppPackages,
                  List<ApplicationInfo> mockAppApplicationInfos);

    /**
     *
     * @param exception Exception
     */
    void onError(Exception exception);
}
