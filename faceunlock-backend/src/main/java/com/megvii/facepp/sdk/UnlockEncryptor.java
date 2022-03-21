package com.megvii.facepp.sdk;

public interface UnlockEncryptor {
    byte[] decrypt(byte[] bArr);

    byte[] encrypt(byte[] bArr);
}
