Setup Guide
============

On most middle to high end Android devices released after 2016, Insular can be setup straightforward without hassle. But still on some devices, you may got “incompatible with your device” message on Google Play Store, or be notified during the setup with error message “Sorry, your device (or ROM) is incompatible with Insular”, or other failures. In these cases, Insular could probably still work on your device if setup manually.

If you are prompted to encrypt your device during the setup, it means your device was not pre-encrypted out of box. If you don't want device decription (which may significantly degrade overall I/O performance on some low-end devices), it can be avoided with manual setup.


Preparation
------------

First of all, you need to connect your Android device to a computer with USB cable, and the official [ADB tool](https://developer.android.com/studio/releases/platform-tools.html) provided by Google.

To check whether the USB-connected Android device is properly recognized by your computer, type the following command in the shell (or Command Prompt on Windows):

`adb devices`

If no device is listed in the output, your Android device is not correctly recognized by the computer.

For Windows PC, [this official guide and driver list for common OEM](https://developer.android.com/studio/run/oem-usb.html) might be helpful. If it does not work out, the [official Google Android USB driver](http://dl.google.com/android/repository/usb_driver_r11-windows.zip) should work for most Android devices, just manually install it, and select "Android ADB Interface" or "Android Composite ADB Interface".

**If your device is Xiaomi branded or runs MIUI**, extra steps are required:

- In system "Settings - Additional settings - Developer options", enable "USB debugging (Security settings)".
- In system "Settings - Permissions - Autostart", enable "Insular". (grant auto-start permission)


Activate Managed Mainland
--------------------------

IMPORTANT: Please read the [**LIMITATIONS OF MANAGED MAINLAND**](README.md/#managed-mainland) before proceeding to the following steps.

1. Backup all data of your logged-in accounts.

2. Remove all accounts in system "Settings" - "Accounts" (may vary on devices).

3. Execute the following command in ADB shell.

   `dpm set-profile-owner --user 0 --name Mainland com.oasisfeng.island.fdroid/com.oasisfeng.island.IslandDeviceAdminReceiver`

   If you get error message "... Not allowed to set the profile/device owner because there are already some **accounts** on the device". make sure all accounts are removed. You may use ADB command `dumpsys account|grep -A 3 Accounts:` to reveal remaining accounts (including the hidden ones on some devices). Forcible removal of all accounts will be implemented in coming version of Insular, please stay tuned.

   Some ROM variants (e.g. MIUI) enforce extra security policy which may block the above command, if you got permission-related error message, please check the development (or security) settings to enable USB-debugging related security options, then retry the "`dpm ...`" command again.

4. Start Insular app and your Mainland is now managed.


Deactivate Managed Mainland
----------------------------

Before Insular can be uninstalled, Managed Mainland must be deactivated.

Open Insular, Settings - Scoped Settings - Mainland, scroll to the bottom, click "Deactivate".


Manual setup for Insular
------------------------

**USE THIS GUIDE ONLY IF THE IN-APP INSULAR SETUP IS NOT VIABLE OR UNSUCCESSFUL.**

Type `adb -d shell` to open ADB shell, and execute the following commands one by one in sequence:

1. `pm create-user --profileOf 0 --managed Insular`

   If succeed, you will be prompted with the ID of newly created user (usually 10 or above). Remember it and replace the `<user id>` in following commands with this ID.

   If you got "Error: couldn't create User", execute `setprop fw.max_users 10` first, then retry the command above.

2. `pm install-existing --user <user id> com.oasisfeng.island.fdroid`

3. `dpm set-profile-owner --user <user id> com.oasisfeng.island.fdroid/com.oasisfeng.island.IslandDeviceAdminReceiver`

   If you get error message `java.lang.SecurityException: Neither user 2000 nor current process has android.permission.MANAGE_DEVICE_ADMIN`, please review the MIUI-specific steps above in "Preparation".

4. `am start-user <user id>`

If all goes well, Insular will show the app list.
