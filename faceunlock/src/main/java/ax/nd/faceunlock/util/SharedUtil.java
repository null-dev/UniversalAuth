package ax.nd.faceunlock.util;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.text.TextUtils;

@SuppressWarnings("deprecation")
public class SharedUtil {
    private final Context mContext;

    public SharedUtil(Context context) {
        mContext = context.getApplicationContext();
    }

    @SuppressLint("ApplySharedPref")
    public void saveIntValue(String str, int i) {
        SharedPreferences.Editor edit = PreferenceManager.getDefaultSharedPreferences(mContext).edit();
        edit.putInt(str, i);
        edit.commit();
    }

    @SuppressLint("ApplySharedPref")
    public void saveBooleanValue(String str, boolean z) {
        SharedPreferences.Editor edit = PreferenceManager.getDefaultSharedPreferences(mContext).edit();
        edit.putBoolean(str, z);
        edit.commit();
    }

    @SuppressLint("ApplySharedPref")
    public void removeSharePreferences(String str) {
        SharedPreferences.Editor edit = PreferenceManager.getDefaultSharedPreferences(mContext).edit();
        edit.remove(str);
        edit.commit();
    }

    public int getIntValueByKey(String str, int defValue) {
        return PreferenceManager.getDefaultSharedPreferences(mContext).getInt(str, defValue);
    }

    public int getIntValueByKey(String str) {
        return getIntValueByKey(str, -1);
    }

    @SuppressLint("ApplySharedPref")
    public void saveStringValue(String str, String str2) {
        SharedPreferences.Editor edit = PreferenceManager.getDefaultSharedPreferences(mContext).edit();
        edit.putString(str, str2);
        edit.commit();
    }

    public String getStringValueByKey(String str) {
        return PreferenceManager.getDefaultSharedPreferences(mContext).getString(str, null);
    }

    @SuppressLint("ApplySharedPref")
    public void saveByteArrayValue(String str, byte[] bArr) {
        SharedPreferences.Editor edit = PreferenceManager.getDefaultSharedPreferences(mContext).edit();
        edit.putString(str, new String(bArr));
        edit.commit();
    }

    public byte[] getByteArrayValueByKey(String str) {
        String string = PreferenceManager.getDefaultSharedPreferences(mContext).getString(str, null);
        if (TextUtils.isEmpty(string)) {
            return null;
        }
        return string.getBytes();
    }

    public boolean getBooleanValueByKey(String str) {
        return PreferenceManager.getDefaultSharedPreferences(mContext).getBoolean(str, false);
    }
}
