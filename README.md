Kiosk Installer
---------------
NOT DONE YET... this is how it WILL be.

KioskInstaller is a small, simple-seeming, program to install an application, likely using
Google Play Store, and give it the permissions to be able to lock the screen into Kiosk Mode.
(A.k.a Lock Screen mode.)

### Background

A Kiosk Mode application locks the screen so that you can't get out of the application, which is
needed in certain business situations, or if it is required to keep accidental button pushes from causing
a user (with possible physical or mental limitations) to effectively lose control of the device.

To put an application into Kiosk Mode requires certain permissions. Those permissions are not easily granted.
The "usual/historical" way required a PC, cables, and PC software, which is at best inconvenient for users.
The permissions can also can be granted immediately after doing a Factory Reset on the device.

If you don't want to Factory Reset your device, there doesn't appear to be any solution but the
PC/cables/software alternative. Thus this is a solution for "Dedicated Devices" only.

For many devices the permissions can be granted by using a special program called a DPC (Device Policy Controller),
which must be installed just after a Factory Reset (but at no other time!).
The DPC can be installed using or (on newer devices) QR codes (on devices with the appropriate hardware).
The details for using the NFC sender app are on its web page. //?????????? (NFC link)
Setting up QR codes is discussed below.
Google provides this capability for business use, but it works fine for any application needing to use Kiosk mode
on a device dedicated to the purpose.

KioskInstaller's primary function is to be a (minimal) DPC. It can install a designated application
as a Lock Enabled program based on input from the QR code or NFC sender.

You don't need to build a copy of this application if you want it as-is; an already built .apk file should work,
which can be found on this GitHub website. You can copy it to your own host to serve it when installing.

This README is primarily for developers, since users really needn't be more than vaguely aware this program exists.
The documentation for your application should provide the user-focused instructions. You can find an example //??? here.

## Usage Overview

The process of installing a Kiosk Mode application (when not using PC/cable)
requires that first you Factory Reset the device.
You'll also need to have WiFi available.
When the device gets to the initial start screen, take an action to tell the device to download and
install KioskInstaller which then "provisions" the device as needed.
* For NFC installation you'll need another device running an application that provides the
necessary information via NFC.
You simply "bump" the devices to start things. (See this <????> NFC app for details.)
* For QR installation tap the screen (in the same place, not on a button or text) 5 times to get into
QR provisioning state.
Work through the steps (to set up WiFi) and it will ultimately load a QR camera app.
Show the camera the QR code produced from the `.json` file below, and it will proceed with downloading
and setting up Kiosk Mode for the application specified by the QR code.
Android 7.0 and later support QR-mode provisioning.

Once everything is set up, KioskInstaller can take the user to Play Store where they can authenticate
themselves and download the Kiosk Mode application in the usual way.

Both the QR and NFC installations work off the same `.json` file format to provide the information
about the application to install. Nothing is hardwired into KioskInstaller.

Note that KioskInstaller doesn't visibly run when using it as an installer helper... it runs for just
a moment and does not (normally) create a UI. (See below about the user UI.)

Note 1: "Factory Reset Protection" (FRP) can effectively "brick" the device if you use it incorrectly.
Be sure to do the Factory Reset from the Settings screen if there are any accounts on the device.
If you do it any other way, after the restart you will be required to authenticate to the device
before it will boot. If you don't have the login information, the device becomes useless.
There's more about FRP on the Web.

Note 2: Even if you don't intend to use NFC provisioning, just QR, you might want to set up the NFC
application for your testing. It can save you the tedious process of re-entering the WiFi information for
each trial.

## Usage Details

It is not possible to (usefully) put KioskInstaller into Play Store...
it won't work because it can't be downloaded early enough.
(You have to authenticate to Play Store to download, and KioskInstaller must run before any accounts
are established on the device, and an account is required to use Play Store.)
Thus, you must host KioskInstaller on a friendly website.
GitHub or your application's WebSite are good candidates.

We'll be referring to a sample `.json` file below, often using abbreviated names since the field names are long.
You'll need to modify it to match your (Kiosk Mode) application, and either copy it to the NFC sender device
or convert it to a QR code. The section on the JSON script has details.

If you use the .apk on this GitHub page for KioskInstaller, the `.json` file below is pre-populated with
the checksum information needed to install it. If you build your own, see the section on Building it yourself below.

You must fill in the values on the ...DOWNLOAD_LOCATION entry to locate your copy of KioskInstaller,
and `packageName` and `installLocation` to find the entry in the Store for your Kiosk Mode application.

The web page [http://down-box.appspot.com/qr] is handy for creating the QR code,
but any of several others on the Web will do.

In the simplest case, simply put the .apk for KioskInstaller where it can be downloaded, fix
the entries entries in the `.json` file, and you're ready to scan QR codes (or copy it
onto the NFC provider).

### The (User) UI

KioskInstaller does have a UI, and can be run from the Apps screen.
The primary purpose of the UI is to tell the user to ignore KioskInstaller's existence, should they find it
while browsing.
When started as an ordinary app, it identifies itself as an installer,
and provides a few buttons to (possibly) undo any damage if the Kiosk Mode application is deleted.

If any "expected" errors occur during the install process (due to bad input),
the UI will start with an appropriate error message.

Most of the screen is an explanation of why it exists.
It lists (immutably) the information to identify the Kiosk Mode application for enabling Lock Mode, and
the download location of the application.
There are three buttons:
* Reenable: which simply grants the Lock Mode privilege to the same app, again.
* Reinstall: does a Reenable and then re-launches the install of the Kiosk Mode application.
* "Owner" (or "Not Owner"), which indicates whether KioskInstaller is currently the Device Owner.
Pressing it when "Owner", and confirming in the dialog, removes the Device Owner Privilege.
This is necessary to completely remove KioskInstaller, but is otherwise discouraged because
restoring that privilege is very difficult. (The Device Owner is the only application that can
revoke the Device Owner privilege.)
There's further, user-level, information about that at https:/?????????????

## Building it yourself

You can of course take a branch and modify KioskInstaller as you wish.
Although it's a simple application, getting the setup to build and debug it is tricky
because of the way it's downloaded and installed.

You can run it as an ordinary application in either the emulator or on the device; that's
useful for checking the UI.
You can also give it the Device Owner privilege
using the PC/cable solution, which as a developer you probably already have set up.
(That will not trigger setting Lock Mode, but you can do that with the Reenable UI button.)
I have not found a way to test the "important part" (calling onProfileProvisioningComplete()) except
via actually installing the application via QR or NFC.

The adb command to grant the Device Owner privilege (for the .apk on this GitHub page):
```
adb shell dpm set-device-owner com.donnKey.kioskInstaller/com.donnKey.kioskInstaller.KioskInstallerDeviceAdmin
```

To build it yourself as something that can be installed by the provisioning process you must build a
**signed** .apk file.
The install process will yield a (very unhelpful) error if you try to install an unsigned .apk.
You must use (in Android Studio) Build->Build Signed Bundle/APK to do that (at least I was unable to find any other way).
The sample .apk file is signed with a key that won't be used for any other purpose.

You must provide one of two forms of checksum in the `.json` file, either that of the package as a whole,
or that of the signature.  If the checksum isn't correct, the errors are very clear.

The ...SIGNATURE_CHECKSUM field is easier to use, because it doesn't change each time you rebuild the application.
The command below will compute the right value after you've built the .apk once.
(Probably any version of apksigner.bat will work, that's the one I used.)
(Be careful of the space after the ':', it should be there.)
```
${ANDROID_SDK_ROOT}/build-tools/28.0.2/apksigner.bat verify -print-certs -v kioskInstaller-release.apk | grep -Po "(?<=SHA-256 digest: ).*" -m 1 | xxd -r -p | openssl base64 | tr -- '+/' '-_' | tr -d '='
```

The ...PACKAGE_CHECKSUM is generated using
```
openssl dgst -binary -sha256 <kioskInstaller-release.apk | openssl base64 | tr '+/' '-_' | tr -d '='
```
Replace the ...SIGNATURE_CHECKSUM with ...PACKAGE_CHECKSUM to use the package checksum.

We noted above some possible solutions for serving the .apk,
they all are a bit inconvenient for testing new builds.
The Jekyll static web page hosting server works very nicely for that.
You can install Jekyll from [https://jekyllrb.com/].
Use `jekyll serve --host 0.0.0.0` to provide a local host, and put the executable in `release` under the directory
in which you started Jekyll ("JekyllStuffDirectory" below).
(It can go anywhere Jekyll allows it, but putting it in `release` lets you set up your Android Studio build
to deliver it there directly.)

In this case, the URL in DOWNLOAD_LOCATION should look like
`http://yourLocalHost:4000/JekyllStuffDirectory/release/kioskInstaller-release.apk`.

To test the "important stuff" you'll need a real device that you can factory reset.
(Android 7 or newer for QR, Android 5.0 (5.1? - the documents are fuzzy) for NFC.)

If all goes well, KioskInstaller will run (invisibly) just as the cold boot finishes, and
the Play Store sign-in screen appear later (10-20 seconds).

For the purpose of developer testing, the "enableDebug" flag is useful.
Just don't leave it in `.json` files your customers will use.
It exists because the device won't be set up for debugging (not yet in "Developer" mode) and getting the logcat
information for debugging can be painful each time you test it (particularly if it crashes).

What enableDebug adds to the UI:
* A view of the logcat data up to the moment that KioskInstaller's onCreate is run, scrollable and
highlighted to find KioskInstaller lines; the logcat data starts as they system is booting.
The "refresh" button rereads that data (to which the system has probably added more).
It starts scrolled to the bottom-most line (where the recent action is).
If there are only a few lines shown, use "refresh"; it's possible to get ahead of logcat's processing.
* A "Wipe It" button that will immediately (no questions asked) start the Factory Reset process, but
only if there are no accounts installed. See the note about "Factory Reset Protection" above.
This is very handy when testing repeatedly.
* A long-press of the "Refresh" button clears the log file of "really old" stuff, but the subsequent
read of logcat will restore more recent entries.

If you start KioskInstaller as an ordinary application (when installed using the debugMode), you'll see the logcat data.
You can customise what you see by changing the logcat command line filters,
as well as the filtering and enhancement KioskInstaller does.
The code should be fairly obvious.
If the UI started due to an expected error, restarting it (close the copy with the error message)
will get you to the debugging version if you've enabled that.

When testing, you can simply back out of the Play Store login, nothing prevents that.
If you do stop Play Store, and since an application which is not yet installed can be given lock privilege,
you could then install a test version of your Kiosk Mode app which would have Lock Mode enabled.

If the application crashes too early for this to work, creating Toasts can be helpful.
There's a commented source line to start that process if you need it.

If the "important part" should crash with an unexpected error, it will enter a hybrid mode where the Wipe button
is not available, but the logcat information is shown.

Note: frequently Device Owner and Lock Mode are handled together in a Kiosk Mode application.
That won't be the case here, so you should test your Kiosk Mode app carefully.

### The JSON script

A complete sample JSON script appears below:
* You can use "PROVISIONING_DEVICE_ADMIN_PACKAGE_CHECKSUM" instead of "PROVISIONING_DEVICE_ADMIN_SIGNATURE_CHECKSUM"
if you wish to provide the .apk checksum.
* You need to change "yourLocalHost", "JekyllStuffDirectory", and the packageName and installLocation lines
for all uses. Typically the installLocation would be a link into Play Store for your application.
* You need to change the ...COMPONENT_NAME line if you're building your own version.
* Other lines are set to normal/useful defaults, but you could add or delete or change them as appropriate based
on the Android documentation. A good place to start is [https://developers.google.com/android/management/provision-device]
* For NFC you must set all four WIFI entries appropriately, or omit the ...SSID entry completely.
(Be sure you get ...HIDDEN correct; you can just remove it for the 'false' case.)
* For NFC, omitting the ...SSID entry is a flag to set up WiFi manually. The other WiFi fields are ignored.
(If the device doesn't show a place to add a hidden SSID, the 'more' button will help.)
* For QR you should remove the _WIFI_ entries.
If the ...SSID entry is present, it appears to re-enter the WiFi fields after the QR code is read.
(I guess you could download the QR camera app from one network and then switch networks for the actual install...)
* Be sure to remove the "enableDebug" line (and the prior comma) completely for deployed use.

Note: It does work to provide a downloadable http[s] location for your Kiosk Mode .apk file.
Installing an application that way requires a number of clicks to get past all the protections
that Android puts in the way of directly installing any .apk file.
(Since this is a fresh device, you'll be doing _all_ the clicks for the first time.)

Since the NFC/QR install process works for any application that you want to be a Device Owner,
if you're installing your Kiosk Mode .apk directly, consider whether making your Kiosk Mode application
be a Device Owner application directly would be easier. The advantage of KioskInstaller is
that your Kiosk Mode application can use Play Store's automatic features for installation
configuration and updates.

```
{
"android.app.extra.PROVISIONING_DEVICE_ADMIN_PACKAGE_DOWNLOAD_LOCATION":"http://yourLocalHost:4000/JekyllStuffDirectory/kioskInstaller-release.apk",
"android.app.extra.PROVISIONING_DEVICE_ADMIN_SIGNATURE_CHECKSUM":"pENMk3hOipvxp1OsZF8sGLDofYEYtkJ-CBplAR30fCI",
"android.app.extra.PROVISIONING_DEVICE_ADMIN_COMPONENT_NAME":"com.donnKey.kioskInstaller/com.donnKey.kioskInstaller.KioskInstallerDeviceAdmin",
"android.app.extra.PROVISIONING_WIFI_HIDDEN":true,
"android.app.extra.PROVISIONING_WIFI_SSID":"mySSID",
"android.app.extra.PROVISIONING_WIFI_SECURITY_TYPE":"WPA",
"android.app.extra.PROVISIONING_WIFI_PASSWORD":"myPassword",
"android.app.extra.PROVISIONING_LEAVE_ALL_SYSTEM_APPS_ENABLED":true,
"android.app.extra.PROVISIONING_SKIP_ENCRYPTION":true,
"android.app.extra.PROVISIONING_ADMIN_EXTRAS_BUNDLE":
  {
   "packageName":"com.yourOrganization.yourKioskApp",
   "installLocation":"https://play.google.com/store/apps/details?id=com.yourOrganization.yourKioskApp",
   "enableDebug":true
  }
}
```
