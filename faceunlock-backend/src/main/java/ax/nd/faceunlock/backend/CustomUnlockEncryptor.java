package ax.nd.faceunlock.backend;

import android.security.keystore.KeyProperties;
import android.security.keystore.KeyProtection;
import android.util.Log;

import com.megvii.facepp.sdk.UnlockEncryptor;

import java.io.ByteArrayOutputStream;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.util.Arrays;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

public class CustomUnlockEncryptor implements UnlockEncryptor {
    public static final String AKS_PROVIDER = "AndroidKeyStore";
    public static final String SEED_ALIAS = "seed_faceunlock";
    private static final int PROFILE_KEY_IV_SIZE = 12;
    private static final String TAG = CustomUnlockEncryptor.class.getSimpleName();

    public CustomUnlockEncryptor() {
        saveSeed();
    }

    private boolean saveSeed() {
        try {
            KeyStore instance = KeyStore.getInstance(AKS_PROVIDER);
            instance.load(null);
            if (instance.containsAlias(SEED_ALIAS)) {
                Log.i(TAG, "key is already created");
                return true;
            }
            KeyGenerator instance2 = KeyGenerator.getInstance("AES");
            instance2.init(new SecureRandom());
            int purposes = KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT;
            instance.setEntry(SEED_ALIAS, new KeyStore.SecretKeyEntry(instance2.generateKey()),
                    new KeyProtection.Builder(purposes)
                            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                            .setUserAuthenticationRequired(false)
                            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build());
            Log.i(TAG, "create key successfully");
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "Exception in store. " + e.toString());
            return false;
        }
    }

    private byte[] encryptData(byte[] bArr) {
        SecretKey secretKey;
        if (bArr == null) {
            return null;
        }
        try {
            KeyStore instance = KeyStore.getInstance(AKS_PROVIDER);
            instance.load(null);
            if (instance.containsAlias(SEED_ALIAS)) {
                secretKey = (SecretKey) instance.getKey(SEED_ALIAS, null);
            } else {
                Log.i(TAG, "key not exist, create key!");
                saveSeed();
                secretKey = (SecretKey) instance.getKey(SEED_ALIAS, null);
            }
            if (secretKey != null) {
                Cipher instance2 = Cipher.getInstance("AES/GCM/NoPadding");
                instance2.init(1, secretKey);
                byte[] doFinal = instance2.doFinal(bArr);
                byte[] iv = instance2.getIV();
                if (iv.length == PROFILE_KEY_IV_SIZE) {
                    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                    byteArrayOutputStream.write(iv);
                    byteArrayOutputStream.write(doFinal);
                    return byteArrayOutputStream.toByteArray();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "Exception in encrypt. " + e.toString());
        }
        return new byte[0];
    }

    private byte[] decryptData(byte[] bArr) {
        SecretKey secretKey = null;
        if (bArr == null) {
            return null;
        }
        try {
            KeyStore instance = KeyStore.getInstance(AKS_PROVIDER);
            instance.load(null);
            if (instance.containsAlias(SEED_ALIAS)) {
                secretKey = (SecretKey) instance.getKey(SEED_ALIAS, null);
            } else {
                Log.e(TAG, "key not exist, something is wrong!");
            }
            if (secretKey != null) {
                byte[] copyOfRange = Arrays.copyOfRange(bArr, 0, PROFILE_KEY_IV_SIZE);
                byte[] copyOfRange2 = Arrays.copyOfRange(bArr, PROFILE_KEY_IV_SIZE, bArr.length);
                Cipher instance2 = Cipher.getInstance("AES/GCM/NoPadding");
                instance2.init(2, secretKey, new GCMParameterSpec(128, copyOfRange));
                return instance2.doFinal(copyOfRange2);
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "Exception in decrypt. " + e.toString());
        }
        return new byte[0];
    }

    @Override
    public byte[] encrypt(byte[] bArr) {
        return encryptData(bArr);
    }

    @Override
    public byte[] decrypt(byte[] bArr) {
        return decryptData(bArr);
    }
}

