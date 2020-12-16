# Insular

Isolate your big brother app.

This is a fork based on the excellent [Island](https://github.com/oasisfeng/island). Extra credit to [Shelter](https://github.com/PeterCxy/Shelter) which inspire me to make the completely FLOSS fork of Island.

## Differences from Island

Bascially no difference, except that

- all blobs (gms, crashlytics, etc) are removed to comply with [F-droid's policy](https://f-droid.org/en/docs/Inclusion_Policy/)
- Internet access for this app is removed because we just don't need it

## Features

With Insular, you can:
- Isolate app, for privacy protection.
- Clone app, for parallel running.
- Freeze app, to completely block its background behaviors.
- Hide app, for various reasons.
- Archive app, for potential future use on-demand.
- Use VPN only on one side, or different VPN on both sides.
- Prohibit USB access.

If your device is incompatible or not encrypted, you can skip this limitation manually. Please refer to [the XDA post](https://forum.xda-developers.com/android/-t3366295) for details.
To uninstall and remove Insular completely, please first "Destroy Insular" in Settings - Setup - Click the recycle-bin icon besides Insular. If you have already uninstalled Insular app, please "Remove work profile" in your device "Settings - Accounts".

## PERMISSIONS

We only request permissions to achieve what you want. So only the followings:

- **DEVICE-ADMIN**: Device administrator privilege is required to create the Insular space (work profile), which serves as the fundamental functionality of Insular. It will be explicitly requested for your consent.
- **PACKAGE_USAGE_STATS**: Required to correctly recognize the running state of apps. It will be explicitly requested for your consent.
We will never collect data related to your privacy, please read our privacy policy for more details.

## Build Instruction

Island depends on ["deagle" library](https://github.com/oasisfeng/deagle), which must be cloned alongside Island in the same path.

```
\--
  \- island
  \- deagle
```

This project is constructed into several modules, with **assembly** module as the build portal,
to support separate "light" build for core modules, in the form of "product flavor" in Gradle build configuration.

The **"engine"** module shares the same package name with the **"complete"** build, to inherit the profile/device owner privilege.
The **"mobile"** and other modules can be installed and updated separately alongside **"engine"** module for development convenience.
