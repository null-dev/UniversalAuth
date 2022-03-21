# UniversalAuth
This project aims to bring a variety of custom authentication options to various Android ROMs.

**Your phone needs to have Xposed (or EdXposed/Lsposed).**

**This project has only been tested on Android 11. It may work in Android 10 and 12 though.** Support for more Android versions is a goal and I welcome contributions!

**You currently cannot use this project to authenticate in apps. It can only unlock your lockscreen.** Support for using this project to authenticate in apps is under development/investigation.
## Authentication modules
Currently available authentication modules:

- [Face unlock](#face-unlock)

### Face unlock
Face unlock allows you to unlock your phone with your face.

#### WARNING
The face unlock module depends on closed source, proprietary libraries developed by Megvii.
I do not have a license to distribute these libraries and **Megvii has filed
DMCA takedown notices to take down repositories containing these modules in
the past**.

Therefore, to use the face unlock module, you will need to obtain these library files yourself.
The module will ask you to provide these files during setup.

Here are the `SHA1` hashes of the library files:

```
For arm64 phones (most phones):
Filename: libFaceDetectCA.so          SHA1: 0e9e08c7cc976e86a8b2c80732b0f3cef3ed09be  Size (bytes): 256088
Filename: libmegface.so               SHA1: 9b598b767a124076d87bb7cc0097d968df620a1d  Size (bytes): 7213272
Filename: libMegviiUnlock-jni-1.2.so  SHA1: cd3747023c62b3f489da6d104ce3ab51dd3d8bb7  Size (bytes): 260176
Filename: libMegviiUnlock.so          SHA1: 25f86732d5bf05679b69e4fa41957efb5491ebc6  Size (bytes): 1066008

For 32-bit ARM phones (very very old phones):
Filename: libFaceDetectCA.so          SHA1: cc5acf3fdc29d85a59fb7b1970fc41c7120e7fa2  Size (bytes): 136888
Filename: libmegface.so               SHA1: 6fe6848ab6ff0a29afb398dc6782f0d30031875a  Size (bytes): 5266192
Filename: libMegviiUnlock-jni-1.2.so  SHA1: 44e77a1b87690d3571209a40a228e30fe185332c  Size (bytes): 136888
Filename: libMegviiUnlock.so          SHA1: 448eca6eabd5f9cbc16b960813cb9996f15fe01c  Size (bytes): 655788

x86_64 phones are not supported, but they are pretty rare.
```

#### Installation
1. Install and enable the UniversalAuth Xposed module. You can download it from [the releases page](https://github.com/null-dev/UniversalAuth/releases).
2. Reboot to make sure the Xposed module is enabled.
3. Install the face unlock auth module APK. You can download it from [the releases page](https://github.com/null-dev/UniversalAuth/releases).
4. A new app called "Face unlock" should now appear, open it.
5. Add the [library files](#warning) when asked.
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
