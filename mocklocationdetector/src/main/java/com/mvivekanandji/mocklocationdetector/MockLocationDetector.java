package com.mvivekanandji.mocklocationdetector;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;

import androidx.annotation.RequiresApi;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

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
 * @author vivekanand
 * @version 1.0
 */
public class MockLocationDetector {

    private static final String TAG = MockLocationDetector.class.getSimpleName();

    private final Context context;
    private boolean verbose;
    private boolean debug;
    private static  boolean userAddedPackageCount;
    private static String blackListAppsFilePath = "BlackListApps.txt";

    private static Set<String> applicationPackageMap = new HashSet<>();

    @SuppressLint("StaticFieldLeak") //application context will be used,
    private static MockLocationDetector mockLocationDetector;

    /**
     * private constructor
     *
     */
    private MockLocationDetector(Context context){
        this.context = context;
        verbose = false;
        debug = false;
    }

    /**
     *
     * @param context Activity Context
     * @return MockLocationDetector
     */
    public static MockLocationDetector with(Context context){
        if(context == null)
            throw new IllegalArgumentException("Context must not be null.");

        if(mockLocationDetector == null)
            mockLocationDetector = new MockLocationDetector(context.getApplicationContext());

        return mockLocationDetector;
    }



    /**
     * For Build.VERSION.SDK_INT <= 21 i.e. LOLLIPOP
     * Detects if Mock location setting is enabled on the device
     *
     * @param context Application Context
     * @return true if mock location setting is enabled, false if disabled
     * @throws UnsupportedOperationException if Build version is greater than Lollipop (API 22)
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
     * Detects if Mock location setting is enabled on the device or is the location is mocked
     * <p>
     * Will always return false on Marshmallow and above because the settings have been
     * updated and its not possible to check if an app has been allowed
     *
     * @param context  Application Context
     * @param location Pass Location object received from the OS's onLocationChanged() callback
     * @return true if mock location setting is enabled, false if disabled (for api <=21)
     * else true if location is mocked, false if location is not mocked
     */
    @Deprecated
    public static boolean isMockLocationOrMockEnabled(Context context, Location location) {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.LOLLIPOP)
            return isAllowMockLocationsEnabled(context);
        else return isMockLocation(location);
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2)
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
    public static boolean checkForAllowMockLocationApp(Context context) {

        for (ApplicationInfo applicationInfo : getAllApps(context)) {
            try {
                PackageInfo packageInfo = context.getPackageManager()
                        .getPackageInfo(applicationInfo.packageName, PackageManager.GET_PERMISSIONS);

                String[] requestedPermissions = packageInfo.requestedPermissions;

                if (requestedPermissions != null)
                    for (String requestedPermission : requestedPermissions)
                        if (requestedPermission.equals("android.permission.ACCESS_MOCK_LOCATION")
                                && !applicationInfo.packageName.equals(context.getPackageName()))
                          return true;

            } catch (PackageManager.NameNotFoundException e) {
                Log.e(TAG, "Got exception " + e.getMessage());
            }
        }
        return false;
    }

    public static List<ApplicationInfo> getMockLocationAppsApplicationInfo(Context context) {
        List<ApplicationInfo> mockApplicationInfoList = new ArrayList<>();

        for (ApplicationInfo applicationInfo : getAllApps(context)) {
            try {
                PackageInfo packageInfo = context.getPackageManager()
                        .getPackageInfo(applicationInfo.packageName, PackageManager.GET_PERMISSIONS);

                String[] requestedPermissions = packageInfo.requestedPermissions;

                if (requestedPermissions != null)
                    for (String requestedPermission : requestedPermissions)
                        if (requestedPermission.equals("android.permission.ACCESS_MOCK_LOCATION")
                                && !applicationInfo.packageName.equals(context.getPackageName()))
                            mockApplicationInfoList.add(applicationInfo);

            } catch (PackageManager.NameNotFoundException e) {
                Log.e(TAG, "Got exception " + e.getMessage());
            }
        }
        return mockApplicationInfoList;
    }

    public static int getMockLocationAppsCount(Context context) {
        return getMockLocationAppsApplicationInfo(context).size();

    }

    public static List<String> getMockLocationAppsNames(Context context) {
        List<String> appNamesList = new ArrayList<>();

        for(ApplicationInfo applicationInfo : getMockLocationAppsApplicationInfo(context))
            appNamesList.add(applicationInfo.loadLabel(context.getPackageManager()).toString());

        return appNamesList;
    }

    public static List<String> getMockLocationAppsPackageNames(Context context) {
        List<String> appPackageList = new ArrayList<>();

        for(ApplicationInfo applicationInfo : getMockLocationAppsApplicationInfo(context))
            appPackageList.add(applicationInfo.packageName);

        return appPackageList;
    }

    public static boolean checkForKnownMockApps(Context context) {
        for (ApplicationInfo applicationInfo : getAllApps(context))
            if (applicationPackageMap.contains(applicationInfo.packageName))
                return true;
        return false;
    }

    /**
     * Removes the mock/test location provider (so location spoofing can be mitigated)
     *
     * @param context Application Context
     * @throws NullPointerException if getSystemService(service name) returns null
     */
    public static void removeMockLocationProvider(Context context) {
        LocationManager locationManager = (LocationManager) context.getSystemService(LOCATION_SERVICE);

        try {
            if (locationManager != null)
                locationManager.removeTestProvider(LocationManager.GPS_PROVIDER);

            else throw new NullPointerException("Location Manager is null.");

        } catch (IllegalArgumentException error) {
            Log.d(TAG, "Got exception in removing test  provider");
        }
    }

    public static void setBlackListAppsFilePath(String filePtah){
        blackListAppsFilePath = filePtah;
    }

    private static List<ApplicationInfo> getAllApps(Context context) {
        final PackageManager packageManager = context.getPackageManager();
        return packageManager.getInstalledApplications(PackageManager.GET_META_DATA);
    }



    private static Set<String> readFileToSet(String filePath)
    {
        Set<String> stringSet = new HashSet<>();

        try (BufferedReader bufferedReader = new BufferedReader(new FileReader(filePath))) {
            String currentLine;
            while ((currentLine = bufferedReader.readLine()) != null)
                stringSet.add(currentLine);
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        return stringSet;
    }

    private void displayError(String errorMessage){
        if(debug) Log.e(TAG, errorMessage);
    }

    private void displayInfo(String infoMessage){
        if(verbose) Log.i(TAG, infoMessage);
    }


}
