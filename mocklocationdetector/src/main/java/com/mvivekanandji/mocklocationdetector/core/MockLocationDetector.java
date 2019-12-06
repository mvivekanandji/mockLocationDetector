package com.mvivekanandji.mocklocationdetector.core;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import com.mvivekanandji.mocklocationdetector.interfaces.OnMockLocationDetectorAppInfoListener;
import com.mvivekanandji.mocklocationdetector.interfaces.OnMockLocationDetectorAppListener;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
 *
 * @author vivekanand
 * @version 1.0
 */

@SuppressWarnings({"unused", "weakeraccess"})
public class MockLocationDetector {

    //region variables
    private static final String TAG = MockLocationDetector.class.getSimpleName();

    private final Context context;
    private boolean verbose;
    private boolean debug;
    private static int userAddedPackageCount;
    private static String blackListAppsFilePath;

    private static Set<String> blacklistApplicationPackageSet;

    @SuppressLint("StaticFieldLeak") //application context will be used,
    private static MockLocationDetector mockLocationDetector;

    //endregion

    /**
     * private constructor
     */
    private MockLocationDetector(Context context) {
        this.context = context;
        verbose = false;
        debug = false;
        blacklistApplicationPackageSet = new HashSet<>();
        blackListAppsFilePath = "BlackListApps.txt";
        displayInfo("Singleton Object created: " + mockLocationDetector);
    }

    /**
     * @param context Activity Context
     * @return MockLocationDetector
     */
    public static MockLocationDetector with(Context context) {
        if (context == null)
            throw new IllegalArgumentException(TAG + ": Context must not be null.");

        if (mockLocationDetector == null)
            mockLocationDetector = new MockLocationDetector(context.getApplicationContext());

        return mockLocationDetector;
    }


    /**
     * prefer using {{@link #isMockLocation(Location)}} instead
     * For Build.VERSION.SDK_INT <= 21 i.e. LOLLIPOP
     * Detects if Mock location setting is enabled on the device
     *
     * @return true if mock location setting is enabled, false if disabled
     * @throws UnsupportedOperationException if Build version is greater than Lollipop (API 22)
     *
     */
    @Deprecated
    public boolean isAllowMockLocationsEnabled() throws UnsupportedOperationException {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.LOLLIPOP)
            return !Settings.Secure.getString(context.getContentResolver(),
                    Settings.Secure.ALLOW_MOCK_LOCATION).equals("0");

        else throw new UnsupportedOperationException("Operation not supported for " +
                "Marshmallow (API 23) and above.");
    }


    /**
     * prefer using {{@link #isMockLocation(Location)}} instead
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
    public boolean isMockLocationOrMockEnabled(Context context, Location location)
    throws UnsupportedOperationException{
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.LOLLIPOP)
            return isAllowMockLocationsEnabled();
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
    public boolean isMockLocation(Location location) throws UnsupportedOperationException{
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2)
            return location != null && location.isFromMockProvider();

        else throw new UnsupportedOperationException("Operation not supported for " +
                "Jelly Bean MR2 (API 18) and below.");
    }

    /**
     * Check if device contains any app that require mock location permission
     * @param onMockLocationDetectorAppListener OnMockLocationDetectorAppListener
     */
    public void checkForAllowMockLocationApp(@NonNull final OnMockLocationDetectorAppListener
                                                     onMockLocationDetectorAppListener) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                for (ApplicationInfo applicationInfo : getAllApps()) {
                    try {
                        PackageInfo packageInfo = context.getPackageManager()
                                .getPackageInfo(applicationInfo.packageName, PackageManager.GET_PERMISSIONS);

                        String[] requestedPermissions = packageInfo.requestedPermissions;

                        if (requestedPermissions != null)
                            for (String requestedPermission : requestedPermissions)
                                if (requestedPermission.equals("android.permission.ACCESS_MOCK_LOCATION")
                                        && !applicationInfo.packageName.equals(context.getPackageName())){
                                    onMockLocationDetectorAppListener.onResult(true);
                                    displayInfo("Mock app detected:" +
                                            applicationInfo.loadLabel(context.getPackageManager()).toString());
                                    return;
                                }
                    } catch (PackageManager.NameNotFoundException e) {
                        onMockLocationDetectorAppListener.onError(e);
                        displayError(e);
                    }
                }
                onMockLocationDetectorAppListener.onResult(false);
                displayInfo("No mock app detected");
            }
        }).start();
    }

    /**
     * Check if device contains any app that require mock location permission and returns
     * associated information using listener interface
     * @param onMockLocationDetectorAppInfoListener OnMockLocationDetectorAppInfoListener
     */
    public void getMockLocationAppsApplicationInfo(@NonNull final OnMockLocationDetectorAppInfoListener
                                                           onMockLocationDetectorAppInfoListener) {

        new Thread(new Runnable() {
            @Override
            public void run() {
                List<ApplicationInfo> mockApplicationInfoList = new ArrayList<>();
                List<String> appNamesList = new ArrayList<>();
                List<String> appPackageList = new ArrayList<>();

                for (ApplicationInfo applicationInfo : getAllApps()) {
                    try {
                        PackageInfo packageInfo = context.getPackageManager()
                                .getPackageInfo(applicationInfo.packageName, PackageManager.GET_PERMISSIONS);

                        String[] requestedPermissions = packageInfo.requestedPermissions;

                        if (requestedPermissions != null)
                            for (String requestedPermission : requestedPermissions)
                                if (requestedPermission.equals("android.permission.ACCESS_MOCK_LOCATION")
                                        && !applicationInfo.packageName.equals(context.getPackageName())) {
                                    mockApplicationInfoList.add(applicationInfo);
                                    appNamesList.add((applicationInfo.loadLabel(context.getPackageManager()).toString()));
                                    appPackageList.add(applicationInfo.packageName);
                                }

                    } catch (PackageManager.NameNotFoundException e) {
                        onMockLocationDetectorAppInfoListener.onError(e);
                        displayError(e);
                    }
                }
                onMockLocationDetectorAppInfoListener.onResult(
                        mockApplicationInfoList.size() > 0,
                        mockApplicationInfoList.size(),
                        appNamesList,
                        appPackageList,
                        mockApplicationInfoList
                );
                displayInfo(mockApplicationInfoList.toString());
            }
        }).start();

    }

    public void checkForKnownMockApps(@NonNull final OnMockLocationDetectorAppListener
                                              onMockLocationDetectorAppListener) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                for (ApplicationInfo applicationInfo : getAllApps())
                    if (blacklistApplicationPackageSet.contains(applicationInfo.packageName)) {
                        onMockLocationDetectorAppListener.onResult(true);
                        displayInfo("Mock app detected:" +
                                applicationInfo.loadLabel(context.getPackageManager()).toString());
                        return;
                    }
                onMockLocationDetectorAppListener.onResult(false);
                displayInfo("No mock app detected");
            }
        }).start();
    }

    public void getKnownMockLocationAppsApplicationInfo(@NonNull final OnMockLocationDetectorAppInfoListener
                                                                onMockLocationDetectorAppInfoListener) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                List<ApplicationInfo> mockApplicationInfoList = new ArrayList<>();
                List<String> appNamesList = new ArrayList<>();
                List<String> appPackageList = new ArrayList<>();

                for (ApplicationInfo applicationInfo : getAllApps())
                    if (blacklistApplicationPackageSet.contains(applicationInfo.packageName)) {
                        mockApplicationInfoList.add(applicationInfo);
                        appNamesList.add((applicationInfo.loadLabel(context.getPackageManager()).toString()));
                        appPackageList.add(applicationInfo.packageName);
                    }
                onMockLocationDetectorAppInfoListener.onResult(
                        mockApplicationInfoList.size() > 0,
                        mockApplicationInfoList.size(),
                        appNamesList,
                        appPackageList,
                        mockApplicationInfoList
                );
                displayInfo(mockApplicationInfoList.toString());
            }
        }).start();
    }

    /**
     * Removes the mock/test location provider (so location spoofing can be mitigated)
     *
     * @throws NullPointerException if getSystemService(service name) returns null
     */
    public void removeMockLocationProvider() {
        LocationManager locationManager = (LocationManager) context.getSystemService(LOCATION_SERVICE);

        try {
            if (locationManager != null){
                locationManager.removeTestProvider(LocationManager.GPS_PROVIDER);
                displayInfo("Test location provider removed");
            }


            else throw new NullPointerException("Location Manager is null.");

        } catch (IllegalArgumentException e) {
           displayError(e);
        }
    }


    //region getter/setter

    /**
     * Setter - to add package name to check
     * @param packageName String
     * @return this object
     */
    public MockLocationDetector addBlackListApplicationPackage(String packageName) {
        blacklistApplicationPackageSet.add(packageName);
        displayInfo("Package added: " + packageName);
        return this;
    }

    /**
     * Setter - to add list of package names to check
     * @param packageNameList List<String>
     * @return this object
     */
    public MockLocationDetector addBlackListApplicationPackage(List<String> packageNameList) {
        blacklistApplicationPackageSet.addAll(packageNameList);
        displayInfo("Packages added: " + packageNameList);
        return this;
    }

    /**
     * Getter - to get list of package names of blacklist apps
     * @return Set<String>
     */
    public Set<String> getBlackListApps() {
        if (blacklistApplicationPackageSet.isEmpty()
                || blacklistApplicationPackageSet.size() == userAddedPackageCount)
            blacklistApplicationPackageSet = readFileToSet(blackListAppsFilePath);

        return blacklistApplicationPackageSet;
    }

    /**
     * Setter - to set blacklist apps file path
     * @param filePtah String
     * @return this object
     */
    public MockLocationDetector setBlackListAppsFilePath(String filePtah) {
        blackListAppsFilePath = filePtah;
        displayInfo("Blacklist apps file changed to: " + filePtah);
        return this;
    }

    /**
     * Getter - to get blacklist apps file path
     * @return String
     */
    public static String getBlackListAppsFilePath() {
        return blackListAppsFilePath;
    }

    /**
     * Getter
     * @return boolean
     */
    public boolean isVerbose() {
        return verbose;
    }

    /**
     * Setter - to set debug (will log info messages when true)
     * @param verbose boolean
     * @return this object
     */
    public MockLocationDetector setVerbose(boolean verbose) {
        this.verbose = verbose;
        displayInfo("Verbose: " + verbose);
        return this;
    }

    /**
     * Getter
     * @return boolean
     */
    public boolean isDebug() {
        return debug;
    }

    /**
     * Setter - to set debug (will log error messages when true)
     * @param debug boolean
     * @return this object
     */
    public MockLocationDetector setDebug(boolean debug) {
        this.debug = debug;
        displayInfo("Debug: " + debug);
        return this;
    }

    //endregion

    // region private methods
    private List<ApplicationInfo> getAllApps() {
        final PackageManager packageManager = context.getPackageManager();
        return packageManager.getInstalledApplications(PackageManager.GET_META_DATA);
    }

    private Set<String> readFileToSet(String filePath) {
        Set<String> stringSet = new HashSet<>();

        try (BufferedReader bufferedReader = new BufferedReader(new FileReader(filePath))) {
            String currentLine;
            while ((currentLine = bufferedReader.readLine()) != null)
                stringSet.add(currentLine);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return stringSet;
    }

    private void displayError(Exception e) {
        if (debug) {
            Log.i(TAG, e.getMessage());
            e.printStackTrace();
        }
    }

    private void displayInfo(String infoMessage) {
        if (verbose) Log.i(TAG, infoMessage);
    }

    //endregion


}
