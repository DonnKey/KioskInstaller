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
import androidx.preference.PreferenceManager;

import androidx.annotation.NonNull;

import static com.donnKey.kioskInstaller.KioskInstall.ACTION_ERROR;
import static com.donnKey.kioskInstaller.KioskInstall.APP_TAG;
import static com.donnKey.kioskInstaller.KioskInstall.KEY_ENABLE_DEBUG;
import static com.donnKey.kioskInstaller.KioskInstall.KEY_INSTALL_LOCATION;
import static com.donnKey.kioskInstaller.KioskInstall.KEY_PACKAGE_NAME;
import static com.donnKey.kioskInstaller.KioskInstall.KEY_UNEXPECTED_FAILURE;

public class KioskInstallerDeviceAdmin extends DeviceAdminReceiver {
    private static final String TAG = APP_TAG + "DeviceAdmin";

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
        // This is the "important part" where the real work happens. Everything else is support.
        super.onProfileProvisioningComplete(context, intent);
        //Toast.makeText(context, "Complete", Toast.LENGTH_LONG).show();

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        try {
            final PersistableBundle bundle = intent.getParcelableExtra(DevicePolicyManager.EXTRA_PROVISIONING_ADMIN_EXTRAS_BUNDLE);
            if (bundle != null) {
                // Capture the package and location names (and debug flag).

                String packageName = bundle.getString("packageName");
                if (packageName == null) {
                    packageName = "";
                }
                String installLocation = bundle.getString("installLocation");
                if (installLocation == null) {
                    installLocation = "";
                }
                boolean enableDebug = false;
                Object s = bundle.get("debugMode");
                if (s != null) {
                    enableDebug = s.toString().equalsIgnoreCase("true");
                }
                sharedPreferences.edit().putString(KEY_PACKAGE_NAME, packageName).apply();
                sharedPreferences.edit().putString(KEY_INSTALL_LOCATION, installLocation).apply();
                sharedPreferences.edit().putString(KEY_ENABLE_DEBUG, enableDebug?"true":"false").commit();

                // Make the app able to lock... this works even when the package isn't installed yet
                if (!packageName.isEmpty()) {
                    enablePackageLock(context, packageName);
                }

                // Start play store, but delay a bit... the intent doesn't always work
                // if it happens (presumably) too soon in startup.
                if (!packageName.isEmpty() && !installLocation.isEmpty()) {
                    Handler handler = new Handler();
                    // To make the lambda happy:
                    String pn = packageName;
                    String il = installLocation;
                    handler.postDelayed(() -> startPlayStore(context, pn, il), 2000);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            // If a completely unexpected error, force it into debug mode.
            // For expected errors, we just post a reasonable message
            sharedPreferences.edit().putString(KEY_ENABLE_DEBUG, KEY_UNEXPECTED_FAILURE).commit();
        }
    }

    @Override
    public void onEnabled(@NonNull Context context, @NonNull Intent intent) {
        // Called when permission is changed from adb dpm... reflect the new state
        KioskInstall.refresh();
    }

    @Override
    public void onDisabled(@NonNull Context context, @NonNull Intent intent) {
        // Called when permission is changed from adb dpm... reflect the new state
        KioskInstall.refresh();
    }

    static void enablePackageLock(Context context, String packageName) {
        final String[] packageNames = {packageName};
        DevicePolicyManager dpm =
                (DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE);
        assert dpm != null;
        ComponentName adminName = new ComponentName(context,KioskInstallerDeviceAdmin.class);
        try {
            dpm.setLockTaskPackages(adminName, packageNames);
        } catch (SecurityException e) {
            e.printStackTrace();
            postError(context, "Unable to Lock " + packageName + ": " + e.getMessage());

        }
    }

    static void startPlayStore(Context context, String packageName, String installLocation) {
        switch (checkPlayInstalled(context, packageName)) {
            case NOT_INSTALLED:
                Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(installLocation));
                try {
                    context.startActivity(i);
                } catch (ActivityNotFoundException e) {
                    e.printStackTrace();
                    postError(context, "Unable to Install " + packageName + " from " + installLocation + ": " + e.getMessage());
                }
                break;
            case INSTALLED_MANUALLY:
            case INSTALLED_BY_PLAY:
                break;
        }
    }

    public static int checkPlayInstalled(Context context, String targetPackage) {
        PackageManager manager = context.getPackageManager();
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

    static void postError(Context context, String message) {
        // Since the "important part" doesn't have a UI, borrow the debug UI
        // to present "expected" errors. (Note the onDelay when starting the
        // install... there's no chance of a UI when that runs, so we use
        // bring up the UI via an intent to display the error)
        Intent i = new Intent(context, KioskInstall.class);
        i.setAction(ACTION_ERROR);
        i.putExtra(ACTION_ERROR, message);
        context.startActivity(i);
    }

    static boolean isDeviceOwner(@NonNull Context context) {
        DevicePolicyManager dpm =
                (DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE);
        assert dpm != null;
        return dpm.isDeviceOwnerApp(context.getPackageName());
    }

    @SuppressWarnings({"deprecation", "RedundantSuppression"})
    static void clearDeviceOwner(@NonNull Context context) {
        DevicePolicyManager dpm =
                (DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE);
        assert dpm != null;
        // Deprecated API: for purposes of testing this is exactly what we need, and docs say it's
        // OK for testing. (We don't want to wipe the device as they suggest).
        dpm.clearDeviceOwnerApp(context.getPackageName());
        ComponentName adminComponentName = new ComponentName(context, KioskInstallerDeviceAdmin.class);
        dpm.removeActiveAdmin(adminComponentName);
    }
}
