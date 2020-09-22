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

import androidx.appcompat.app.AppCompatActivity;

import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.BackgroundColorSpan;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.List;
import java.util.ListIterator;

public class KioskInstall extends AppCompatActivity {
    static final String APP_TAG = "KIOSK ";
    static final private String TAG = APP_TAG + " " + "KioskInstaller";

    static final String KEY_PACKAGE_NAME = "package_name";
    static final String KEY_INSTALL_LOCATION = "install_location";
    static final String KEY_ENABLE_DEBUG = "enable_debug";

    static final String UNKNOWN_PLACEHOLDER = "Unknown";

    SharedPreferences sharedPreferences;

    String packageName;
    String installLocation;
    boolean debugMode = true;

    TextView introduction_field;
    EditText packageName_field;
    EditText installLocation_field;
    Button button_clearOwner;
    Button button_wipe;
    Button button_refresh;
    View view;
    private static final String FILENAME = "LogFile";
    File logFile;

    // All the real work in this app is done in the callback in KioskInstallerDeviceAdmin.
    // This is just debugging tools and an explanation to the user that finds this app.
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //This is useful if the logcat dumper doesn't function...
        //Toast.makeText(this, "onCreate", Toast.LENGTH_LONG).show();

        Log.w(TAG, "onCreate");

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        File directory = this.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
        logFile = new File(directory, FILENAME);

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        view = findViewById(android.R.id.content).getRootView();
        introduction_field = view.findViewById(R.id.introduction);
        introduction_field.setText(R.string.introduction);

        Log.w(TAG, "Vanilla startup");
        packageName = sharedPreferences.getString(KEY_PACKAGE_NAME, UNKNOWN_PLACEHOLDER);
        installLocation = sharedPreferences.getString(KEY_INSTALL_LOCATION, UNKNOWN_PLACEHOLDER);
        debugMode = sharedPreferences.getBoolean(KEY_ENABLE_DEBUG, false);

        SpannableString ss = new SpannableString(captureLog());
        setHighLightedText(ss, APP_TAG);
        setHighLightedLine(ss, "KioskInstall");
        introduction_field.setText(ss);
        debugMode = true;

        packageName_field = view.findViewById(R.id.package_name);
        installLocation_field = view.findViewById(R.id.install_location);
        button_clearOwner = view.findViewById(R.id.button_clearOwner);
        button_wipe = view.findViewById(R.id.button_wipe);
        button_refresh = view.findViewById(R.id.button_refresh);

        packageName_field.setText(packageName);
        packageName_field.setFocusable(false);

        installLocation_field.setText(installLocation);
        installLocation_field.setFocusable(false);

        if (debugMode) {
            // Set up button to factory reset easily while testing
            button_wipe.setVisibility(View.VISIBLE);
            button_wipe.setOnClickListener((v) -> {
                DevicePolicyManager dpm =
                        (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
                assert dpm != null;
                dpm.wipeData(0);
            });
        }
        else {
            button_wipe.setVisibility(View.GONE);
        }

        button_clearOwner.setText(KioskInstallerDeviceAdmin.isDeviceOwner(this)? "owner" : "not owner" );
        button_clearOwner.setOnClickListener((v) -> {
            if (KioskInstallerDeviceAdmin.isDeviceOwner(this)) {
                KioskInstallerDeviceAdmin.clearDeviceOwner(this);
            }
        });

        // Refresh... reread the log.
        button_refresh.setOnClickListener((v) -> {
            finish();
            startActivity(getIntent());
        });
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
        try {
            Process process = Runtime.getRuntime().exec("logcat -d -f " + logFile);
            // This should be a "with timeout, but since that isn't available everywhere
            // and the likelihood of it failing is small and this is debug-only code...
            process.waitFor();
        } catch (Exception e) {
            return "Failure Starting Logcat: " + stackTrace(e);
        }

        List<String> lines;
        try {
            InputStream in = new BufferedInputStream(new FileInputStream(logFile));
            //noinspection unchecked
            lines = IOUtils.readLines(in);
        } catch (IOException e) {
            return "Failure reading logcat data: " + stackTrace(e);
        }

        // There's a benign bug in older releases of Android that causes a huge
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
}