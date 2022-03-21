package com.megvii.facepp.sdk;

import android.util.Log;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public class FeatureRestoreHelper {
    private static final int BUFFER_SIZE = 8192;
    private static final int RESTORE_IMAGE_SIZE = 144;
    private static final String TAG = "FeatureRestoreHelper";
    public static final byte[] sMagic = {1, 2, 3, 4, 5, 6, 7, 8};
    private UnlockEncryptor mEncryptor;

    public void setUnlockEncryptor(UnlockEncryptor unlockEncryptor) {
        this.mEncryptor = unlockEncryptor;
    }

    public void saveRestoreImage(byte[] bArr, String str, int i) {
        Log.i(TAG, "saveRestoreImage: length: " + bArr.length + " id " + i);
        writeFile(getRestoreFile(str, i).getAbsolutePath(), bArr);
    }

    public void deleteRestoreImage(String str, int i) {
        Log.i(TAG, "deleteRestoreImage: id " + i);
        getRestoreFile(str, i).delete();
    }

    public int restoreAllFeature(String str) {
        int i;
        File[] listFiles = new File(str).listFiles();
        if (listFiles == null || listFiles.length == 0) {
            return 24;
        }
        int i2 = 0;
        for (File file : listFiles) {
            String name = file.getName();
            if (name.startsWith("restore_") && name.length() > 8) {
                Log.i(TAG, "restoreAllFeature: " + name);
                try {
                    i = Integer.parseInt(name.substring(8));
                } catch (NumberFormatException unused) {
                    i = -1;
                }
                if (i != -1) {
                    byte[] readFile = readFile(file.getAbsolutePath());
                    Log.i(TAG, "restoreAllFeature: update old feature " + i);
                    if (restoreFeatureAtPosition(i, readFile) == 0) {
                        i2++;
                    }
                }
            }
        }
        if (i2 == 0) {
            return 24;
        }
        return 0;
    }

    private int restoreFeatureAtPosition(int i, byte[] bArr) {
        return Lite.getInstance().updateFeature(bArr, RESTORE_IMAGE_SIZE, RESTORE_IMAGE_SIZE, 90, true, new byte[Lite.FEATURE_SIZE], new byte[Lite.IMAGE_SIZE], i);
    }

    private File getRestoreFile(String str, int i) {
        return new File(str, "restore_" + i);
    }

    private void writeFile(String str, byte[] bArr) {
        File file = new File(str);
        if (file.exists()) {
            file.delete();
        }
        UnlockEncryptor unlockEncryptor = this.mEncryptor;
        int i = 0;
        if (unlockEncryptor != null) {
            byte[] encrypt = unlockEncryptor.encrypt(bArr);
            int length = encrypt.length;
            byte[] bArr2 = sMagic;
            bArr = new byte[(length + bArr2.length)];
            System.arraycopy(bArr2, 0, bArr, 0, bArr2.length);
            System.arraycopy(encrypt, 0, bArr, sMagic.length, encrypt.length);
        }
        int length2 = bArr.length;
        try {
            FileOutputStream fileOutputStream = new FileOutputStream(str);
            while (length2 > i) {
                int i2 = length2 - i;
                if (i2 > BUFFER_SIZE) {
                    i2 = BUFFER_SIZE;
                }
                fileOutputStream.write(bArr, i, i2);
                i += i2;
            }
        } catch (IOException e) {
            Log.e(TAG, "writeFile failed", e);
        }
    }

    private byte[] readFile(String str) {
        File file = new File(str);
        if (!file.exists()) {
            return null;
        }
        int length = (int) file.length();
        byte[] bArr = new byte[length];
        try {
            FileInputStream fileInputStream = new FileInputStream(str);
            int i = 0;
            while (length > i) {
                int i2 = length - i;
                if (i2 > BUFFER_SIZE) {
                    i2 = BUFFER_SIZE;
                }
                i += fileInputStream.read(bArr, i, i2);
            }
            if (this.mEncryptor == null || !startWithMagic(bArr)) {
                return bArr;
            }
            byte[] bArr2 = new byte[(bArr.length - sMagic.length)];
            System.arraycopy(bArr, sMagic.length, bArr2, 0, bArr2.length);
            return this.mEncryptor.decrypt(bArr2);
        } catch (IOException e) {
            Log.e(TAG, "readFile failed", e);
            return bArr;
        }
    }

    private boolean startWithMagic(byte[] bArr) {
        if (bArr.length < sMagic.length) {
            return false;
        }
        int i = 0;
        while (true) {
            byte[] bArr2 = sMagic;
            if (i >= bArr2.length) {
                return true;
            }
            if (bArr[i] != bArr2[i]) {
                return false;
            }
            i++;
        }
    }
}
