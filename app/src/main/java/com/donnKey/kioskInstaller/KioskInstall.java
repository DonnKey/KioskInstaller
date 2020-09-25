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

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.method.ScrollingMovementMethod;
import android.text.style.BackgroundColorSpan;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.ref.WeakReference;
import java.util.List;
import java.util.ListIterator;

public class KioskInstall extends AppCompatActivity {
    static final String APP_TAG = "KIOSK ";
    static final private String TAG = APP_TAG + " " + "KioskInstaller";

    static final String KEY_PACKAGE_NAME = "package_name";
    static final String KEY_INSTALL_LOCATION = "install_location";
    static final String KEY_ENABLE_DEBUG = "enable_debug";

    static final String UNKNOWN_PLACEHOLDER = "Unknown";
    private static final String FILENAME = "LogFile";

    static WeakReference<KioskInstall> myInstance;

    SharedPreferences sharedPreferences;

    String packageName;
    String installLocation;
    boolean debugMode;

    TextView introduction_field;
    TextView logText_field;
    TextView packageName_field;
    TextView installLocation_field;
    Button button_clearOwner;
    Button button_wipe;
    Button button_refresh;
    Button button_reenable;
    Button button_reinstall;
    View view;

    // All the real work in this app is done in the callback in KioskInstallerDeviceAdmin.
    // This is just debugging tools and an explanation to the user that finds this app.
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //This is useful if the logcat dumper doesn't function...
        //Toast.makeText(this, "onCreate", Toast.LENGTH_LONG).show();

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        view = findViewById(android.R.id.content).getRootView();
        introduction_field = view.findViewById(R.id.introduction);
        logText_field = view.findViewById(R.id.log_text);
        introduction_field.setText(R.string.introduction);

        packageName = sharedPreferences.getString(KEY_PACKAGE_NAME, UNKNOWN_PLACEHOLDER);
        installLocation = sharedPreferences.getString(KEY_INSTALL_LOCATION, UNKNOWN_PLACEHOLDER);
        debugMode = sharedPreferences.getBoolean(KEY_ENABLE_DEBUG, false);

        packageName_field = view.findViewById(R.id.package_name);
        installLocation_field = view.findViewById(R.id.install_location);
        button_clearOwner = view.findViewById(R.id.button_clearOwner);
        button_wipe = view.findViewById(R.id.button_wipe);
        button_refresh = view.findViewById(R.id.button_refresh);
        button_reenable = view.findViewById(R.id.button_reenable);
        button_reinstall = view.findViewById(R.id.button_reinstall);

        packageName_field.setText(packageName);
        packageName_field.setFocusable(false);

        installLocation_field.setText(installLocation);
        installLocation_field.setFocusable(false);
    }

    @Override
    protected void onResume() {
        super.onResume();
        myInstance= new WeakReference<>(this);

        // Text window: description or debug text
        if (debugMode) {
            // Get the current logcat output and display it.
            SpannableString ss = new SpannableString(captureLog());
            setHighLightedText(ss, APP_TAG);
            setHighLightedLine(ss, "KioskInstall");
            logText_field.setText(ss);
            logText_field.setVisibility(View.VISIBLE);
            logText_field.setMovementMethod(new ScrollingMovementMethod());
            logText_field.onPreDraw();
            introduction_field.setVisibility(View.GONE);
        }
        else {
            // Otherwise, self-describe
            logText_field.setVisibility(View.GONE);
            introduction_field.setVisibility(View.VISIBLE);
        }

        // Wipe button
        if (debugMode) {
            button_wipe.setVisibility(View.VISIBLE);
            AccountManager am = AccountManager.get(this);
            Account[] accounts = am.getAccounts();
            // Set up button to factory reset easily while testing
            if (accounts.length <= 0 && KioskInstallerDeviceAdmin.isDeviceOwner(this)) {
                button_wipe.setOnClickListener((v) -> {
                    DevicePolicyManager dpm =
                            (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
                    assert dpm != null;
                    dpm.wipeData(0);
                });
            }
            else {
                // If there are any accounts, wiping this way enables "Factory Reset Protection",
                // which is more trouble than doing it from settings, which doesn't do that.
                // And of course attempting it when not owner is hopeless.
                button_wipe.setText(R.string.wipe_from_settings);
            }
        }
        else {
            button_wipe.setVisibility(View.GONE);
        }

        // The owner button
        button_clearOwner.setText(KioskInstallerDeviceAdmin.isDeviceOwner(this)
               ? R.string.is_device_owner : R.string.not_device_owner);
        if (KioskInstallerDeviceAdmin.isDeviceOwner(this)) {
            button_clearOwner.setOnClickListener((v) -> {
                AlertDialog dialog = new AlertDialog.Builder(this)
                    .setTitle(R.string.clear_device_owner)
                    .setMessage(R.string.drop_device_owner_warning)
                    .setPositiveButton(R.string.continue_clearing, (a, b)-> {
                            KioskInstallerDeviceAdmin.clearDeviceOwner(this);
                            finish();
                            startActivity(getIntent());
                        }
                    )
                    .setNegativeButton(android.R.string.cancel, (a, b)-> {})
                    .show();
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(
                        getResources().getColor(android.R.color.holo_red_dark));
                dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(
                        getResources().getColor(android.R.color.holo_green_dark));
            });
        }

        // Refresh... reread the log.
        if (debugMode) {
            button_refresh.setVisibility(View.VISIBLE);
            button_refresh.setOnClickListener((v) -> {
                finish();
                startActivity(getIntent());
            });
            button_refresh.setOnLongClickListener((v) -> {
                flushLog();
                finish();
                startActivity(getIntent());
                return true;
            });
        }
        else {
            button_refresh.setVisibility(View.GONE);
        }

        // We can't do reenable and reinstall if we're not the owner...
        // (We could install, but then what?)
        if (KioskInstallerDeviceAdmin.isDeviceOwner(this)) {
            button_reenable.setEnabled(true);
            button_reinstall.setEnabled(true);
            button_reenable.setOnClickListener(
                    (v) -> KioskInstallerDeviceAdmin.enablePackageLock(this, packageName));

            button_reinstall.setOnClickListener((v) -> {
                KioskInstallerDeviceAdmin.enablePackageLock(this, packageName);
                KioskInstallerDeviceAdmin.startPlayStore(this, packageName, installLocation);
            });
        }
        else {
            button_reenable.setEnabled(false);
            button_reinstall.setEnabled(false);
        }
    }

    // use myInstance to update the screen if we get an enable or disable while displaying.
    // This is crude but simple, and should suffice for debugging.
    @Override
    protected void onPause() {
        super.onPause();
        myInstance = null;
    }

    static void refresh() {
        // We were asynchronously enabled;
        if (myInstance == null) {
            return;
        }
        myInstance.get().finish();
        myInstance.get().startActivity(myInstance.get().getIntent());
    }

    public void setHighLightedText(Spannable textToMarkUp, String pattern) {
        String rawText = textToMarkUp.toString();
        for (int offset = 0; offset < rawText.length(); ) {
            int spanStart = rawText.indexOf(pattern, offset);
            if (spanStart == -1) {
                break;
            }
            textToMarkUp.setSpan(new BackgroundColorSpan(0xFFFFFF00), spanStart, spanStart + pattern.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            offset = spanStart + pattern.length() + 1;
        }
    }

    public void setHighLightedLine(Spannable textToMarkUp, String pattern) {
        String rawText = textToMarkUp.toString();
        for (int offset = 0; offset < rawText.length(); ) {
            int matchStart = rawText.indexOf(pattern, offset);
            if (matchStart == -1) {
                break;
            }
            int spanStart = rawText.lastIndexOf("\n", matchStart);
            if (spanStart == -1) {
                spanStart = 0;
            }
            int spanEnd = rawText.indexOf("\n", matchStart);
            if (spanEnd == -1) {
                spanEnd = rawText.length();
            }
            textToMarkUp.setSpan(new BackgroundColorSpan(0x77FFFF00), spanStart, spanEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            offset = spanEnd;
        }
    }

    static String stackTrace(Exception e) {
        Writer writer = new StringWriter();
        e.printStackTrace(new PrintWriter(writer));
        return writer.toString();
    }

    private String captureLog() {
        // Note: this always just appends to any existing file.
        // Since we're presuming this is on a factory reset device, that does the right thing:
        // we probably want all of the log since startup.
        // A long press on the refresh button empties the log file, but this will
        // refill it from "old" buffers. You can tweak the logcat and local filtering
        // here and in flushLog to see what you need if this proves too noisy in practice.
        File directory = this.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
        assert directory != null;
        if (!directory.exists()) {
            //noinspection ResultOfMethodCallIgnored
            directory.mkdir();
        }
        File logFile = new File(directory, FILENAME);
        try {
            Process process = Runtime.getRuntime().exec("logcat -d -f " + logFile);
            // This should be a "with timeout, but since that isn't available everywhere
            // and the likelihood of it failing is small and this is debug-only code...
            process.waitFor();
        } catch (Exception e) {
            // If we fail here, put that into the text window!
            return "Failure Starting Logcat: " + stackTrace(e);
        }

        List<String> lines;
        try {
            InputStream in = new BufferedInputStream(new FileInputStream(logFile));
            //noinspection unchecked
            lines = IOUtils.readLines(in);
            in.close();
        } catch (IOException e) {
            return "Failure reading logcat data: " + stackTrace(e);
        }

        // There's a benign bug in some older releases of Android that causes a huge
        // stack dump for a function that doesn't exist in (old) androidx.
        // This filters that out.
        ListIterator<String> line = lines.listIterator();
        while(line.hasNext()){
            if(line.next().contains("zygote")) {
                line.remove();
            }
        }

        return StringUtils.join(lines,"\n");
    }

    private void flushLog() {
        // set the file size to zero.
        File directory = this.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
        assert directory != null;
        if (!directory.exists()) {
            //noinspection ResultOfMethodCallIgnored
            directory.mkdir();
        }
        File logFile = new File(directory, FILENAME);
        try {
            OutputStream out = new FileOutputStream(logFile);
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
            // ignore
        }
    }
}