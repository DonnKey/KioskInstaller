---
layout: default
---

When you start KioskInstaller on your Android Device, it will show the name of an applicaton 
near the middle of the screen.
KioskInstaller was used to install that application as a "Kiosk Mode" application.
Under normal circumstances, you should not need to run KioskInstaller again.
However, if Kiosk Mode application has lost the privileges needed to be a Kiosk application, 
KioskInstaller can restore that. 
The most likely cause is that you uninstalled and reinstalled the Kiosk Mode application.

Clicking "Reenable" on KioskInstaller's screen will restore those privileges.

Clicking "Reinstall" will install the application from the source named on the screen (and reenable it).

It is difficult, but not impossible, to uninstall KioskInstaller from your device, and
doing so has potential undesirable consequences.

### Uninstalling
KioskInstaller is installed as a "Device Owner" application, which grants it special privileges.
It's very difficult to regain the Device Owner privilege if you give it up.
The documentation for your Kiosk Mode application should describe how to gain the privilege.
It probably requires either a Factory Reset (complete reset) of the device, or a PC, cables, and special software.

If you do not intend to use the Kiosk Mode application that KioskInstaller displays on its screen,
you can uninstall KioskInstaller if you really need the space. But it is a fairly small application.

If you're not sure, do not uninstall it. It gives you the ability to restore the Kiosk Mode privilege to your
Kiosk Mode application should that be lost somehow.

The Device Owner application is special: Android will not allow it to be removed.
Only the Device Owner can revoke the Device Owner privilege, so the Device Owner must remove
itself as the Device Owner. 

KioskInstaller has a button labeled "Owner" (if it actually is the Device Owner; otherwise it says "Not Owner".)
If you tap the "Owner" button, then Continue, it will remove itself as the Device Owner (and the button label
will change).
After that you can uninstall it in the usual ways.
