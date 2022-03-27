# UniversalAuth
This project aims to bring a variety of custom authentication options to various Android ROMs.

**Your phone needs to have Xposed (or EdXposed/Lsposed).**

**This project has only been tested on Android 11/12. It may work in Android 10 though.** Support for more Android versions is a goal and I welcome contributions!

**You currently cannot use this project to authenticate in apps. It can only unlock your lockscreen.** Support for using this project to authenticate in apps is under development/investigation.

![Face unlock demo](https://raw.githubusercontent.com/null-dev/UniversalAuth/master/branding/face-unlock.gif)
## Authentication modules
Currently available authentication modules:

- [Face unlock](#face-unlock)

### Face unlock
Face unlock allows you to unlock your phone with your face.

The face unlock module depends on closed source, proprietary libraries developed by Megvii.
These libraries can be found in this free app: https://play.google.com/store/apps/details?id=com.motorola.faceunlock so this app will download it and pull the libraries out of it.


#### Installation
1. Install and enable the UniversalAuth Xposed module. You can download it from [the releases page](https://github.com/null-dev/UniversalAuth/releases).
2. Reboot to make sure the Xposed module is enabled.
3. Install the face unlock auth module APK. You can download it from [the releases page](https://github.com/null-dev/UniversalAuth/releases).
4. A new app called "Face unlock" should now appear, open it.
5. Allow the app to download the libraries when requested.
6. Grant the app permission to unlock your phone when asked.
7. Enable the accessibility service when asked.
8. Press the "START SETUP" button to enroll your face. The app will ask you to grant it camera permissions, make sure to select "allow while using the app" if that option is available.
9. Lock your phone and test that you are able to use face unlock!

# Credits
Thanks to:

- the PixelExperience devs for writing the core UI and face unlock logic code.
- The LsPosed/EdXposed devs and rovo89 for Xposed.
- topjohnwu for Magisk.
- Google for AOSP.
