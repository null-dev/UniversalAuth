package com.megvii.facepp.sdk;

import android.annotation.TargetApi;
import android.media.Image;
import android.os.Environment;
import android.os.StatFs;
import android.util.Log;

import com.megvii.facepp.sdk.jni.LiteApi;
import java.nio.ByteBuffer;

public class Lite {
    public static final int FEATURE_SIZE = 10000;
    public static final int IMAGE_SIZE = 40000;
    public static final int RESULT_SIZE = 20;
    private static Lite sInstance;
    private long handle = 0;
    private final FeatureRestoreHelper mFeatureRestoreHelper = new FeatureRestoreHelper();
    private String mPath;

    public enum MGULKPowerMode {
        MG_UNLOCK_POWER_NONE,
        MG_UNLOCK_POWER_LOW,
        MG_UNLOCK_POWER_HIGH
    }

    public int setConfig(float f, float f2, float f3) {
        return 0;
    }

    public static Lite getInstance() {
        if (sInstance == null) {
            sInstance = new Lite();
        }
        return sInstance;
    }

    public Lite() {
    }

    public void initHandle(String str, UnlockEncryptor unlockEncryptor) {
        initHandle(str);
        mFeatureRestoreHelper.setUnlockEncryptor(unlockEncryptor);
    }

    public void initHandle(String str) {
        if (handle == 0) {
            handle = LiteApi.nativeInitHandle(str);
            mPath = str;
        }
    }

    public int initAll(String str, String str2, byte[] bArr) {
        return (int) LiteApi.nativeInitAll(handle, str, str2, bArr);
    }

    public int initAllWithPath(String str, String str2, String str3) {
        return (int) LiteApi.nativeInitAllWithPath(handle, str, str2, str3);
    }

    public int initLive(String str, String str2) {
        return (int) LiteApi.nativeInitLive(handle, str, str2);
    }

    public int initDetect(byte[] bArr) {
        return (int) LiteApi.nativeInitDetect(handle, bArr);
    }

    public int initDetectWithPath(String str) {
        return (int) LiteApi.nativeInitDetectWithPath(handle, str);
    }

    public int releaseLive() {
        return (int) LiteApi.nativeReleaseLive(handle);
    }

    public int releaseDetect() {
        return (int) LiteApi.nativeReleaseDetect(handle);
    }

    public void release() {
        LiteApi.nativeRelease(handle);
        handle = 0;
    }

    public int compare(byte[] bArr, int i, int i2, int i3, boolean z, boolean z2, int[] iArr) {
        if (iArr.length < RESULT_SIZE) {
            return 1;
        }
        return LiteApi.nativeCompare(handle, bArr, i, i2, i3, z, z2, iArr);
    }

    public int compare(byte[] bArr, int i, int i2, int i3, int[] iArr) {
        if (iArr.length < RESULT_SIZE) {
            return 1;
        }
        return LiteApi.nativeCompare(handle, bArr, i, i2, i3, false, false, iArr);
    }

    public int compareMultiImages(MGULKImage[] mGULKImageArr, int[] iArr) {
        if (iArr.length < RESULT_SIZE) {
            return 1;
        }
        return LiteApi.nativeCompareMultiImages(handle, mGULKImageArr, iArr);
    }

    public int saveFeature(byte[] bArr, int i, int i2, int i3, boolean z, byte[] bArr2, byte[] bArr3) {
        return updateFeature(bArr, i, i2, i3, z, bArr2, bArr3, 0);
    }

    public int saveFeature(byte[] bArr, int i, int i2, int i3, byte[] bArr2, byte[] bArr3) {
        return updateFeature(bArr, i, i2, i3, true, bArr2, bArr3, 0);
    }

    public int saveFeature(byte[] bArr, int i, int i2, int i3, boolean z, byte[] bArr2, byte[] bArr3, int[] iArr) {
        if ((new StatFs(Environment.getDataDirectory().getPath()).getAvailableBlocksLong()) < 256) {
            return 33;
        }
        if (bArr3.length < 40000 || bArr2.length < 10000) {
            return 1;
        }
        int nativeSaveFeature = LiteApi.nativeSaveFeature(handle, bArr, i, i2, i3, z ? 1 : 0, bArr2, bArr3, iArr);
        if (nativeSaveFeature == 0) {
            mFeatureRestoreHelper.saveRestoreImage(bArr3, mPath, iArr[0]);
        }
        return nativeSaveFeature;
    }

    public int saveFeature(byte[] bArr, int i, int i2, int i3, byte[] bArr2, byte[] bArr3, int[] iArr) {
        if (bArr3.length < 40000 || bArr2.length < 10000) {
            return 1;
        }
        int nativeSaveFeature = LiteApi.nativeSaveFeature(handle, bArr, i, i2, i3, 1, bArr2, bArr3, iArr);
        if (nativeSaveFeature == 0) {
            mFeatureRestoreHelper.saveRestoreImage(bArr3, mPath, iArr[0]);
        }
        return nativeSaveFeature;
    }

    public int saveFeatureMultiImages(MGULKImage[] mGULKImageArr, byte[] bArr, byte[] bArr2, int[] iArr) {
        int nativeSaveFeatureMultiImages = LiteApi.nativeSaveFeatureMultiImages(handle, mGULKImageArr, bArr, bArr2, iArr);
        if (nativeSaveFeatureMultiImages == 0) {
            mFeatureRestoreHelper.saveRestoreImage(bArr2, mPath, iArr[0]);
        }
        return nativeSaveFeatureMultiImages;
    }

    public int updateFeature(byte[] bArr, int i, int i2, int i3, boolean z, byte[] bArr2, byte[] bArr3, int i4) {
        if (bArr3.length < 40000 || bArr2.length < 10000) {
            return 1;
        }
        int nativeUpdateFeature = LiteApi.nativeUpdateFeature(handle, bArr, i, i2, i3, z ? 1 : 0, bArr2, bArr3, i4);
        if (nativeUpdateFeature == 0) {
            mFeatureRestoreHelper.saveRestoreImage(bArr3, mPath, i4);
        }
        return nativeUpdateFeature;
    }

    public int deleteFeature() {
        return deleteFeature(0);
    }

    public int deleteFeature(int i) {
        int nativeDeleteFeature = LiteApi.nativeDeleteFeature(handle, i);
        mFeatureRestoreHelper.deleteRestoreImage(mPath, i);
        return nativeDeleteFeature;
    }

    public int restoreFeature() {
        return mFeatureRestoreHelper.restoreAllFeature(mPath);
    }

    public int setConfig(float f, float f2, float f3, float f4, boolean z, boolean z2) {
        return LiteApi.nativeSetConfig(handle, f, f2, f3, f4, z, z2);
    }

    public int setConfig(float f, float f2, float f3, float f4) {
        return LiteApi.nativeSetConfig(handle, f, f2, f3, f4, false, false);
    }

    public int setConfig(LiteConfig liteConfig) {
        if (liteConfig == null) {
            return -1;
        }
        return LiteApi.nativeSetConfigV2(handle, liteConfig);
    }

    public int reset() {
        return LiteApi.nativeReset(handle);
    }

    public int prepare(MGULKPowerMode mGULKPowerMode) {
        int i = mGULKPowerMode.ordinal();
        int i2 = 0;
        if (i != 1) {
            if (i == 2) {
                i2 = 1;
            } else if (i == 3) {
                i2 = 2;
            }
        }
        return LiteApi.nativePrepareWithPower(handle, i2);
    }

    public int prepare() {
        MGULKPowerMode.MG_UNLOCK_POWER_HIGH.ordinal();
        return LiteApi.nativePrepare(handle);
    }

    public int setDetectArea(int i, int i2, int i3, int i4) {
        return LiteApi.nativeSetDetectArea(handle, i, i2, i3, i4);
    }

    public String getVersion() {
        return LiteApi.nativeGetVersion(handle);
    }

    public int getFeature(byte[] bArr, int i, int i2, int i3, byte[] bArr2) {
        if (bArr2.length < 10000) {
            return 1;
        }
        return LiteApi.nativeGetFeature(handle, bArr, i, i2, i3, bArr2);
    }

    public int compareFeatures(byte[] bArr, float[] fArr, int i, boolean z) {
        return LiteApi.nativeCompareFeatures(handle, bArr, fArr, i, z);
    }

    public int checkFeatureValid(int i) {
        return LiteApi.nativeCheckFeatureValid(handle, i);
    }

    public int getFeatureCount() {
        return LiteApi.nativeGetFeatureCount();
    }

    public long setLogLevel(int i) {
        return LiteApi.nativeSetLogLevel(i);
    }

    public LiteConfig getConfig() {
        LiteConfig liteConfig = new LiteConfig(this, null);
        LiteApi.nativeGetConfig(handle, liteConfig);
        return liteConfig;
    }

    @TargetApi(21)
    public static int image2NV21(Image image, byte[] bArr) {
        int readImageIntoBuffer = readImageIntoBuffer(image, bArr);
        if (readImageIntoBuffer == 1) {
            return 1;
        }
        revertHalf(bArr);
        return readImageIntoBuffer;
    }

    @TargetApi(21)
    private static int readImageIntoBuffer(Image image, byte[] bArr) {
        int i;
        int i2;
        if (image == null) {
            Log.e("NULL Image", "image is null");
            return 1;
        }
        int width = image.getWidth();
        int height = image.getHeight();
        Image.Plane[] planes = image.getPlanes();
        int i3 = 0;
        for (int i4 = 0; i4 < planes.length; i4++) {
            ByteBuffer buffer = planes[i4].getBuffer();
            int rowStride = planes[i4].getRowStride();
            int pixelStride = planes[i4].getPixelStride();
            if (i4 == 0) {
                i = width;
            } else {
                i = width / 2;
            }
            if (i4 == 0) {
                i2 = height;
            } else {
                i2 = height / 2;
            }
            if (pixelStride == 1 && rowStride == i) {
                int i5 = i * i2;
                buffer.get(bArr, i3, i5);
                i3 += i5;
            } else {
                byte[] bArr2 = new byte[rowStride];
                int i6 = i3;
                int i7 = 0;
                while (i7 < i2 - 1) {
                    buffer.get(bArr2, 0, rowStride);
                    int i8 = i6;
                    int i9 = 0;
                    while (i9 < i) {
                        bArr[i8] = bArr2[i9 * pixelStride];
                        i9++;
                        i8++;
                    }
                    i7++;
                    i6 = i8;
                }
                buffer.get(bArr2, 0, Math.min(rowStride, buffer.remaining()));
                int i10 = 0;
                while (i10 < i) {
                    bArr[i6] = bArr2[i10 * pixelStride];
                    i10++;
                    i6++;
                }
                i3 = i6;
            }
        }
        return 0;
    }

    private static void revertHalf(byte[] bArr) {
        int length = bArr.length;
        int i = length / 3;
        byte[] bArr2 = new byte[i];
        int i2 = length / 6;
        int i3 = i2 * 4;
        int i4 = i2 * 5;
        int i5 = 0;
        while (i5 < bArr2.length - 1) {
            bArr2[i5] = bArr[i4];
            bArr2[i5 + 1] = bArr[i3];
            i5 += 2;
            i4++;
            i3++;
        }
        int i6 = i * 2;
        if (length - i6 >= 0) System.arraycopy(bArr2, 0, bArr, i6, length - i6);
    }

    public static class MGULKImage {
        public static int MG_UNLOCK_IMG_2PD = 1;
        public static int MG_UNLOCK_IMG_BGR = 2;
        public static int MG_UNLOCK_IMG_DEPTH = 5;
        public static int MG_UNLOCK_IMG_IR = 3;
        public static int MG_UNLOCK_IMG_IR_PATTERN = 4;
        public static int MG_UNLOCK_IMG_NV21;
        int angle;
        int height;
        byte[] imageData;
        int imageSize;
        int imageType;
        int width;

        public MGULKImage(int i, byte[] bArr, int i2, int i3, int i4, int i5) {
            imageType = i;
            imageData = bArr;
            imageSize = i2;
            width = i3;
            height = i4;
            angle = i5;
        }
    }

    public class LiteConfig {
        public static final int MG_UNLOCK_BIG_CPU_CORE_HIGH = 4;
        public static final int MG_UNLOCK_BIG_CPU_CORE_LOW = 0;
        public static final int MG_UNLOCK_COMPARE_ALL = 0;
        public static final int MG_UNLOCK_COMPARE_LIVE = 1;
        public static final int MG_UNLOCK_COMP_DEVICE_CPU = 1;
        public static final int MG_UNLOCK_COMP_DEVICE_NONE = 0;
        public static final int MG_UNLOCK_COMP_DEVICE_OPENCL = 3;
        public static final int MG_UNLOCK_COMP_DEVICE_SNPE = 2;
        public static final int MG_UNLOCK_EXTRACT_APU = 4;
        public static final int MG_UNLOCK_EXTRACT_DOUBLE_CORE_NORMAL = 1;
        public static final int MG_UNLOCK_EXTRACT_DSP = 3;
        public static final int MG_UNLOCK_EXTRACT_OPENCL = 2;
        public static final int MG_UNLOCK_EXTRACT_SINALE_CORE_NORMAL = 0;
        public static final int MG_UNLOCK_STORE_DEBUG_IMAGE_NONE = 0;
        public static final int MG_UNLOCK_STORE_DEBUG_IMAGE_NV21 = 1;
        public static final int MG_UNLOCK_STORE_DEBUG_IMAGE_NV21_LANDMARK = 2;
        public float ComparePitchDownThreshold;
        public float ComparePitchTopThreshold;
        public float CompareYawLeftThreshold;
        public float CompareYawRightThreshold;
        public int bigCpuCore;
        public boolean blurness;
        public int compDeviceType;
        public boolean compareBlurness;
        public int compareType;
        public int extractConfig;
        public boolean eyeOcclusion;
        public boolean eyeStatus;
        public boolean faceIntact;
        public boolean light;
        public boolean mouthOcclusion;
        public String nativeLibraryPath;
        public String openclCachePath;
        public float pitchDownThreshold;
        public float pitchTopThreshold;
        public int rectBottom;
        public int rectLeft;
        public int rectRight;
        public int rectTop;
        public String saveImagePath;
        public String snpeCachePath;
        public int storeDebugImgMode;
        public boolean useModelToCheck3dPose;
        public float yawLeftThreshold;
        public float yawRightThreshold;

        LiteConfig(Lite lite, MGULKPowerMode powerMode) {
            this();
        }

        private LiteConfig() {
        }

        public String toString() {
            return "LiteConfig{compDeviceType=" + compDeviceType + ", bigCpuCore=" + bigCpuCore + ", useModelToCheck3dPose=" + useModelToCheck3dPose + ", eyeOcclusion=" + eyeOcclusion + ", mouthOcclusion=" + mouthOcclusion + ", eyeStatus=" + eyeStatus + ", light=" + light + ", blurness=" + blurness + ", compareBlurness=" + compareBlurness + ", faceIntact=" + faceIntact + ", yawLeftThreshold=" + yawLeftThreshold + ", yawRightThreshold=" + yawRightThreshold + ", pitchTopThreshold=" + pitchTopThreshold + ", pitchDownThreshold=" + pitchDownThreshold + ", CompareYawLeftThreshold=" + CompareYawLeftThreshold + ", CompareYawRightThreshold=" + CompareYawRightThreshold + ", ComparePitchTopThreshold=" + ComparePitchTopThreshold + ", ComparePitchDownThreshold=" + ComparePitchDownThreshold + ", rectLeft=" + rectLeft + ", rectTop=" + rectTop + ", rectRight=" + rectRight + ", rectBottom=" + rectBottom + ", storeDebugImgMode=" + storeDebugImgMode + ", saveImagePath='" + saveImagePath + "', compareType=" + compareType + ", extractConfig=" + extractConfig + ", nativeLibraryPath='" + nativeLibraryPath + "', openclCachePath='" + openclCachePath + "', snpeCachePath='" + snpeCachePath + "'}";
        }
    }
}
