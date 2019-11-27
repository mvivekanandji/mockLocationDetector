package com.mvivekanandji.mocklocationdetector;

import android.Manifest;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;

import androidx.annotation.RequiresApi;
import androidx.annotation.RequiresPermission;

import java.util.List;

import static android.Manifest.permission.ACCESS_COARSE_LOCATION;
import static android.Manifest.permission.ACCESS_FINE_LOCATION;
import static android.content.Context.LOCATION_SERVICE;

/**
 * Copyright 2019 Vivekanand Mishra.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * <p>
 * Created by Vivekanand Mishra on 27/11/19.
 */
public class MockLocationDetector {

    private static final String TAG = MockLocationDetector.class.getSimpleName();

    private boolean verbose;
    private boolean quite;
    private boolean debug;


    /**
     * For Build.VERSION.SDK_INT <= 21 i.e. LOLLIPOP
     * Detects if Mock location setting is enabled the device
     *
     * Will always return false on Marshmallow and above because the settings have been
     * updated and its not possible to check if an app has been allowed
     *
     * @param context Application Context
     * @return true if mock location setting is enabled, false if disabled
     * @throws UnsupportedOperationException if Build version is greater than Lollipop (API 22)
     *
     */
    @Deprecated
    public static boolean isAllowMockLocationsEnabled(Context context) {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.LOLLIPOP)
            return !Settings.Secure.getString(context.getContentResolver(),
                    Settings.Secure.ALLOW_MOCK_LOCATION).equals("0");

        else throw new UnsupportedOperationException("Operation not supported for " +
                    "Marshmallow (API 23) and above.");
    }

    /**
     * For Build.VERSION.SDK_INT >= 18 i.e. JELLY_BEAN_MR2
     * Check if the location recorded is a mocked location or not
     *
     * @param location Pass Location object received from the OS's onLocationChanged() callback
     * @return true if location is mocked, false if location is not mocked
     * @throws UnsupportedOperationException if Build version is less than Jelly Bean MR2 (API 18)
     */
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    public static boolean isMockLocation(Location location) {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2)
            return location != null && location.isFromMockProvider();

        else throw new UnsupportedOperationException("Operation not supported for " +
                "Jelly Bean MR2 (API 18) and below.");
    }

    /**
     * Check if device contains app that require mock location permission
     *
     * @param context Application Context
     * @return true if app requiring mock permission is available, false if no such app present
     */
    public static boolean checkForAllowMockLocationsApps(Context context) {
        int count = 0;

        PackageManager packageManager = context.getPackageManager();
        List<ApplicationInfo> applicationInfoList =
                packageManager.getInstalledApplications(PackageManager.GET_META_DATA);

        for (ApplicationInfo applicationInfo : applicationInfoList) {
            try {
                PackageInfo packageInfo = packageManager.getPackageInfo(applicationInfo.packageName,
                        PackageManager.GET_PERMISSIONS);

                String[] requestedPermissions = packageInfo.requestedPermissions;

                if (requestedPermissions != null)
                    for (String requestedPermission : requestedPermissions)
                        if (requestedPermission.equals("android.permission.ACCESS_MOCK_LOCATION")
                                && !applicationInfo.packageName.equals(context.getPackageName()))
                            count++;

            } catch (PackageManager.NameNotFoundException e) {
                Log.e(TAG, "Got exception " + e.getMessage());
            }
        }
        return count > 0;
    }

    /**
     * Removes the mock/test location provider (so location spoofing can be mitigated)
     *
     * @param context Application Context
     * @throws NullPointerException if getSystemService(service name) returns null
     *
     */
    public static void removeMockLocationProvider(Context context){
        LocationManager locationManager = (LocationManager) context.getSystemService(LOCATION_SERVICE);

        try {
            if(locationManager != null)
                locationManager.removeTestProvider(LocationManager.GPS_PROVIDER);

            else throw new NullPointerException("Location Manager is null.");

        } catch (IllegalArgumentException error) {
            Log.d(TAG,"Got exception in removing test  provider");
        }

    }

}
