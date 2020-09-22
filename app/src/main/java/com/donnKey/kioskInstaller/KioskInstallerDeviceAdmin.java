/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2020 Donn S. Terry
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.donnKey.kioskInstaller;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.admin.DeviceAdminReceiver;
import android.app.admin.DevicePolicyManager;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Handler;
import android.os.PersistableBundle;
import android.preference.PreferenceManager;
import android.util.Log;

import androidx.annotation.NonNull;

import static com.donnKey.kioskInstaller.KioskInstall.APP_TAG;
import static com.donnKey.kioskInstaller.KioskInstall.KEY_ENABLE_DEBUG;
import static com.donnKey.kioskInstaller.KioskInstall.KEY_INSTALL_LOCATION;
import static com.donnKey.kioskInstaller.KioskInstall.KEY_PACKAGE_NAME;

public class KioskInstallerDeviceAdmin extends DeviceAdminReceiver {
    private static final String TAG = "DeviceAdmin";

    final static int NOT_INSTALLED = 0;
    final static int INSTALLED_MANUALLY = 1;
    final static int INSTALLED_BY_PLAY = 2;

    /* DOCS suggest this should not be implemented
    @Override
    public void onReceive(@NonNull Context context, @NonNull Intent intent) {
    }
    */

    // The callbacks are called when the privilege changes, even when
    // the app is NOT running. (It is run for a moment.)
    @SuppressLint("ApplySharedPref")
    @Override
    public void onProfileProvisioningComplete(@NonNull Context context, @NonNull Intent intent) {
        super.onProfileProvisioningComplete(context, intent);
        //?????????????? does this get called from ADB?
        //Toast.makeText(context, "Complete", Toast.LENGTH_LONG).show();

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        final PersistableBundle bundle = intent.getParcelableExtra(DevicePolicyManager.EXTRA_PROVISIONING_ADMIN_EXTRAS_BUNDLE);
        if (bundle != null) {
            Log.w(APP_TAG + TAG, "got a bundle");
            //Toast.makeText(this, "Got a bundle", Toast.LENGTH_LONG).show();
            // Capture the package and location names (and debug flag).
            String packageName = bundle.getString("packageName");
            String installLocation = bundle.getString("installLocation");
            String e = bundle.getString("enableWipe");
            boolean enableWipe = e!= null && e.equalsIgnoreCase("true");
            sharedPreferences.edit().putString(KEY_PACKAGE_NAME, packageName).apply();
            sharedPreferences.edit().putString(KEY_INSTALL_LOCATION, installLocation).apply();
            sharedPreferences.edit().putBoolean(KEY_ENABLE_DEBUG, enableWipe).commit();

            // Make the app able to lock... this works even when the package isn't installed yet
            enablePackageLock(context, packageName);

            // Start play store, but delay a bit... the intent doesn't always work
            // if it happens (presumably) too soon in startup.
            Handler handler = new Handler();
            handler.postDelayed(
                    () -> startPlayStore(context, packageName, installLocation) , 2000);
        }
    }

    /*
    @Override
    public void onEnabled(@NonNull Context context, @NonNull Intent intent) {
        Log.w(APP_TAG + TAG, "onEnabled complete");
    }

    @Override
    public void onDisabled(@NonNull Context context, @NonNull Intent intent) {
        Log.w(APP_TAG + TAG, "onDisabled (owner)");
    }
     */

    private void enablePackageLock(Context context, String packageName) {
        final String[] packageNames = {packageName};
        DevicePolicyManager dpm =
                (DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE);
        ComponentName adminName = new ComponentName(context,KioskInstallerDeviceAdmin.class);
        Log.w(APP_TAG + TAG, "Comp name 2" + adminName);
        assert dpm != null;
        try {
            Log.w(APP_TAG + TAG, "doing set");
            dpm.setLockTaskPackages(adminName, packageNames);
            Log.w(APP_TAG + TAG, "set succeeded");
        } catch (SecurityException e) {
            Log.w(APP_TAG + TAG, "set failed with " + e.getMessage());
        }
    }

    static
    void startPlayStore(Context context, String packageName, String installLocation) {
        switch (checkPlayInstalled(context, packageName)) {
            case NOT_INSTALLED:
                Log.w(APP_TAG + TAG, "Not installed");
                Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(installLocation));
                try {
                    context.startActivity(i);
                    Log.w(APP_TAG + TAG, "Intent going");
                } catch (ActivityNotFoundException e) {
                    Log.e(APP_TAG + TAG, "Activity Not found");
                    e.printStackTrace();
                }
                break;
            case INSTALLED_MANUALLY:
                Log.w(APP_TAG + TAG, "manually installed, not...");
            case INSTALLED_BY_PLAY:
                Log.w(APP_TAG + TAG, "By play");
                break;
        }

    }

    public static int checkPlayInstalled(Context context, String targetPackage) {
        PackageManager manager = context.getPackageManager();
        Intent launcherIntent = new Intent(Intent.ACTION_MAIN);
        launcherIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        try {
            manager.getPackageInfo(targetPackage, PackageManager.GET_ACTIVITIES);
        } catch (PackageManager.NameNotFoundException e) {
            return NOT_INSTALLED;
        }
        final String installer = manager.getInstallerPackageName(targetPackage);
        if (installer != null
                && installer.contains("com.android.vending")) {
            return INSTALLED_BY_PLAY;
        }
        else {
            return INSTALLED_MANUALLY;
        }
    }

    public static boolean isDeviceOwner(Context context) {
        return API21.isDeviceOwner(context);
    }

    public static void clearDeviceOwner(Context context) {
        API21.clearDeviceOwnerAndAdmin(context);
    }

    @TargetApi(21)
    private static class API21 {

        static boolean isDeviceOwner(@NonNull Context context) {
            DevicePolicyManager dpm =
                    (DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE);
            assert dpm != null;
            return dpm.isDeviceOwnerApp(context.getPackageName());
        }

        static void clearDeviceOwnerAndAdmin(@NonNull Context context) {
            DevicePolicyManager dpm =
                    (DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE);
            assert dpm != null;
            dpm.clearDeviceOwnerApp(context.getPackageName());
            ComponentName adminComponentName = new ComponentName(context, KioskInstallerDeviceAdmin.class);
            dpm.removeActiveAdmin(adminComponentName);
        }

        static void enableLockTask(@NonNull Context context) {
            DevicePolicyManager dpm =
                    (DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE);
            ComponentName adminComponentName = new ComponentName(context, KioskInstallerDeviceAdmin.class);
            assert dpm != null;
            if (dpm.isAdminActive(adminComponentName) &&
                   dpm.isDeviceOwnerApp(context.getPackageName()))
                dpm.setLockTaskPackages(adminComponentName, new String[]{context.getPackageName()});
        }
    }
}
