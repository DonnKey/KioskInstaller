Kiosk Installer
---------------
Kiosk Installer is a small, simple-seeming, program to install an application, presumably from
Google Play Store, and give it the permissions to be able to lock the screen into Kiosk Mode.
  
Although it will run as an ordinary application, it's primary function is to be run at device
setup time using QR-code provisioning. The outline of the process is that you Factory Reset the
device (there's no other possible choice) and then when it gets to the initial start screen,
tap the screen (in the same place) 5 times to get into QR provisioning state. Work through the
steps (you're setting up WiFi) and it will  get to a camera screen.
Show the camera the QR code produced from the text below,  and it will proceed with downloading
and setting up Kiosk (LockScreen) for the application you named.
  
You CANNOT put this application into Play Store... it won't work because it can't be downloaded
early enough. You must host it on a friendly website.
GitHub provides one alternative, but for testing it's handy to use the Jekyll local-testing
web-page server.
Use `jekyll serve --host 0.0.0.0` to provide a local host, and put the executable under the directory
in which you started Jekyll ("JekyllStuffDirectory").
The URL in DOWNLOAD_LOCATION should look like `http://yourLocalHost:4000/JekyllStuffDirectory/kioskInstaller-release.apk`.
  
Below you'll find a serialized JSON string which can be pasted into any of several QR-code
generator applications (after filling the right values).
You must fill in the values on the first three lines and the packageName and installLocation
for the entry in the Store. The PACKAGE_CHECKSUM must the the checksum for the file you're
downloading, generated using this line (from git Bash, e.g.)
`openssl dgst -binary -sha256 <kioskInstaller-release.apk | openssl base64 | tr '+/' '-_' | tr -d '='`
This web page is handy for creating the QR codes: http://down-box.appspot.com/qr.

When building the app, you can test it like any other app, but the "important stuff" isn't
tested easily. To do that you'll need a real device (Android 7 or newer!) that you can factory
reset. (Hopefully you won't have too many tries since I did that.) It **must be** a signed build
of the APK. It will refuse to install otherwise, with an unhelpful error.
And the checksum must match, although the error is clear if you make that mistake.

The "enableWipe" entry below is for testing. Don't ship it to your customers (just omit it and the prior comma),
but it's very handy to allow you to cause a factory reset with a single click while you're testing.
(You could also simply turn that off in production builds, of course.)

Since the application runs before most things (such as your debug USB connection) are set up,
it's difficult to track crashes. I found it helpful to use Toasts to see what it's done if it
crashes. The try/catch stuff that looks odd is also there to attempt to provide a clue as to
what failed.

To get the signature value to use in EXTRA_PROVISIONING_DEVICE_ADMIN_SIGNATURE_CHECKSUM
(xxd -p is not defined, but looks to mean "pipe".)  Note the space just after "digest:", it's critical,
as the -m 1 (take just one line).

When testing, if you set up an account, be sure to use manual factory reset (not the wipe button)
because FRP may bite you on the next test cycle.

```
/D/Android/AppData/build-tools/28.0.2/apksigner.bat verify -print-certs -v kioskInstaller-release.apk | grep -Po "(?<=SHA-256 digest: ).*" -m 1 | xxd -r -p | openssl base64 | tr -d '=' | tr -- '+/=' '-_'
```
  
```
{  
"android.app.extra.PROVISIONING_DEVICE_ADMIN_COMPONENT_NAME":"com.yourinstaller.kioskInstaller/com.yourinstaller.kioskInstaller.KioskInstallerDeviceAdmin",  
"android.app.extra.PROVISIONING_DEVICE_ADMIN_PACKAGE_DOWNLOAD_LOCATION":"http://yourdownloadsite/kioskInstaller-release.apk",  
"android.app.extra.PROVISIONING_DEVICE_ADMIN_PACKAGE_CHECKSUM":"wVLvkQMX1p2fvzkO2KTm-kg_eO4wri4tlSwe3L8ecJ4",  
"android.app.extra.PROVISIONING_LEAVE_ALL_SYSTEM_APPS_ENABLED":true,  
"android.app.extra.PROVISIONING_SKIP_ENCRYPTION":true,  
"android.app.extra.PROVISIONING_ADMIN_EXTRAS_BUNDLE":  
{  
"packageName":"com.yourPackageName.yourKioskApp",  
"installLocation":"https://play.google.com/store/apps/details?id=com.yourPackageName.yourKioskApp",  
"enableWipe":true  
}   
}
```

```
{
"android.app.extra.PROVISIONING_WIFI_HIDDEN":true,
"android.app.extra.PROVISIONING_WIFI_SSID":"justforemily",
"android.app.extra.PROVISIONING_WIFI_SECURITY_TYPE":"WPA",
"android.app.extra.PROVISIONING_WIFI_PASSWORD":"LuxSitW00f",
"android.app.extra.PROVISIONING_DEVICE_ADMIN_COMPONENT_NAME":"com.donnKey.kioskInstaller/com.donnKey.kioskInstaller.KioskInstallerDeviceAdmin",
"android.app.extra.PROVISIONING_DEVICE_ADMIN_PACKAGE_DOWNLOAD_LOCATION":"http://DonnteH02:4000/aesopPlayer/kioskInstaller-release.apk",
"android.app.extra.PROVISIONING_DEVICE_ADMIN_PACKAGE_CHECKSUM":"wVLvkQMX1p2fvzkO2KTm-kg_eO4wri4tlSwe3L8ecJ4",
"android.app.extra.PROVISIONING_LEAVE_ALL_SYSTEM_APPS_ENABLED":true,
"android.app.extra.PROVISIONING_SKIP_ENCRYPTION":true,
"android.app.extra.PROVISIONING_ADMIN_EXTRAS_BUNDLE":
  {
   "packageName":"github.io.donnKey.aesopPlayer",
   "installLocation":"https://play.google.com/store/apps/details?id=github.io.donnKey.aesopPlayer",
   "enableWipe":true
  }
}
```

{
"android.app.extra.PROVISIONING_WIFI_HIDDEN":true,
"android.app.extra.PROVISIONING_WIFI_SSID":"justforemily",
"android.app.extra.PROVISIONING_WIFI_SECURITY_TYPE":"WPA",
"android.app.extra.PROVISIONING_WIFI_PASSWORD":"LuxSitW00f",
"android.app.extra.PROVISIONING_DEVICE_ADMIN_COMPONENT_NAME":"com.donnKey.kioskInstaller/com.donnKey.kioskInstaller.KioskInstallerDeviceAdmin",
"android.app.extra.PROVISIONING_DEVICE_ADMIN_PACKAGE_DOWNLOAD_LOCATION":"http://DonnteH02:4000/aesopPlayer/kioskInstaller-release.apk",
"android.app.extra.PROVISIONING_DEVICE_ADMIN_SIGNATURE_CHECKSUM":"5nOyMA8s60dtyOlYNt2rq1UktogUCBA5u2_LJPPbeUc",
"android.app.extra.PROVISIONING_LEAVE_ALL_SYSTEM_APPS_ENABLED":true,
"android.app.extra.PROVISIONING_SKIP_ENCRYPTION":true,
"android.app.extra.PROVISIONING_ADMIN_EXTRAS_BUNDLE":
  {
   "packageName":"github.io.donnKey.aesopPlayer",
   "installLocation":"https://play.google.com/store/apps/details?id=github.io.donnKey.aesopPlayer",
   "enableWipe":true
  }
}