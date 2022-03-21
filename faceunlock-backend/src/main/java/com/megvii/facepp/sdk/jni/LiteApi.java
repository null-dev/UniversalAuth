package com.megvii.facepp.sdk.jni;

import com.megvii.facepp.sdk.Lite;

public class LiteApi {
    public static native int nativeCheckFeatureValid(long j, int i);

    public static native int nativeCompare(long j, byte[] bArr, int i, int i2, int i3, boolean z, boolean z2, int[] iArr);

    public static native int nativeCompareFeatures(long j, byte[] bArr, float[] fArr, int i, boolean z);

    public static native int nativeCompareMultiImages(long j, Lite.MGULKImage[] mGULKImageArr, int[] iArr);

    public static native int nativeDeleteFeature(long j, int i);

    public static native long nativeGetConfig(long j, Lite.LiteConfig liteConfig);

    public static native int nativeGetFeature(long j, byte[] bArr, int i, int i2, int i3, byte[] bArr2);

    public static native int nativeGetFeatureCount();

    public static native String nativeGetVersion(long j);

    public static native long nativeInitAll(long j, String str, String str2, byte[] bArr);

    public static native long nativeInitAllWithPath(long j, String str, String str2, String str3);

    public static native long nativeInitDetect(long j, byte[] bArr);

    public static native long nativeInitDetectWithPath(long j, String str);

    public static native long nativeInitHandle(String str);

    public static native long nativeInitLive(long j, String str, String str2);

    public static native int nativePrepare(long j);

    public static native int nativePrepareWithPower(long j, int i);

    public static native long nativeRelease(long j);

    public static native long nativeReleaseDetect(long j);

    public static native long nativeReleaseLive(long j);

    public static native int nativeReset(long j);

    public static native int nativeSaveFeature(long j, byte[] bArr, int i, int i2, int i3, int i4, byte[] bArr2, byte[] bArr3, int[] iArr);

    public static native int nativeSaveFeatureMultiImages(long j, Lite.MGULKImage[] mGULKImageArr, byte[] bArr, byte[] bArr2, int[] iArr);

    public static native int nativeSetConfig(long j, float f, float f2, float f3, float f4, boolean z, boolean z2);

    public static native int nativeSetConfigV2(long j, Lite.LiteConfig liteConfig);

    public static native int nativeSetDetectArea(long j, int i, int i2, int i3, int i4);

    public static native long nativeSetLogLevel(int i);

    public static native int nativeUpdateFeature(long j, byte[] bArr, int i, int i2, int i3, int i4, byte[] bArr2, byte[] bArr3, int i5);
}
