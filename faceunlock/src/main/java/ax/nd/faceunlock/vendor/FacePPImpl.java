package ax.nd.faceunlock.vendor;

import android.content.Context;
import android.util.Log;

import ax.nd.faceunlock.AppConstants;
import ax.nd.faceunlock.R;
import ax.nd.faceunlock.util.ConUtil;
import ax.nd.faceunlock.util.SharedUtil;
import ax.nd.faceunlock.util.Util;
import ax.nd.faceunlock.backend.CustomUnlockEncryptor;
import ax.nd.faceunlock.backend.FaceUnlockVendorImpl;

import java.io.File;

public class FacePPImpl {
    private static final String SDK_VERSION = "1";
    private static final String TAG = FacePPImpl.class.getSimpleName();
    private final Context mContext;
    private final SharedUtil mShareUtil;
    private SERVICE_STATE mCurrentState = SERVICE_STATE.INITING;

    public FacePPImpl(Context context) {
        mContext = context;
        mShareUtil = new SharedUtil(mContext);
    }

    public void init() {
        synchronized (this) {
            long startTime = System.currentTimeMillis();
            if (mCurrentState != SERVICE_STATE.INITING) {
                Log.d(TAG, " Has been init, ignore");
                return;
            }
            if (Util.DEBUG) {
                Log.i(TAG, "init start");
            }
            boolean z = !SDK_VERSION.equals(mShareUtil.getStringValueByKey(AppConstants.SHARED_KEY_SDK_VERSION));
            File dir = mContext.getDir("faceunlock_data", 0);
            if (!dir.exists()) {
                dir.mkdirs();
            }
            String raw = ConUtil.getRaw(mContext, ax.nd.faceunlock.backend.R.raw.model_file, "model", "model_file", z);
            if (raw == null) {
                Log.e(TAG, "Unavalibale memory, init failed, stop self");
                return;
            }
            String raw2 = ConUtil.getRaw(mContext, ax.nd.faceunlock.backend.R.raw.panorama_mgb, "model", "panorama_mgb", z);
            FaceUnlockVendorImpl.getInstance().initHandle(dir.getAbsolutePath(), new CustomUnlockEncryptor());
            long initAllWithPath = FaceUnlockVendorImpl.getInstance().initAllWithPath(raw2, "", raw);
            if (Util.DEBUG) {
                Log.i(TAG, "init stop");
            }
            if (initAllWithPath != 0) {
                Log.e(TAG, "init failed, stop self");
                return;
            }
            if (z) {
                restoreFeature();
                mShareUtil.saveStringValue(AppConstants.SHARED_KEY_SDK_VERSION, SDK_VERSION);
            }
            Log.d("MeasureFaceUnlock", "Init time: " + (System.currentTimeMillis() - startTime));
            mCurrentState = SERVICE_STATE.IDLE;
        }
    }

    public void restoreFeature() {
        if (Util.DEBUG) {
            Log.i(TAG, "RestoreFeature");
        }
        synchronized (this) {
            FaceUnlockVendorImpl.getInstance().prepare();
            FaceUnlockVendorImpl.getInstance().restoreFeature();
            FaceUnlockVendorImpl.getInstance().reset();
        }
    }

    public void compareStart() {
        synchronized (this) {
            if (mCurrentState == SERVICE_STATE.INITING) {
                init();
            }
            if (mCurrentState == SERVICE_STATE.UNLOCKING) {
                return;
            }
            if (mCurrentState != SERVICE_STATE.IDLE) {
                Log.e(TAG, "unlock start failed: current state: " + mCurrentState);
                return;
            }
            if (Util.DEBUG) {
                Log.i(TAG, "compareStart");
            }
            FaceUnlockVendorImpl.getInstance().prepare();
            mCurrentState = SERVICE_STATE.UNLOCKING;
        }
    }

    public int compare(byte[] bArr, int i, int i2, int i3, boolean z, boolean z2, int[] iArr) {
        synchronized (this) {
            if (mCurrentState != SERVICE_STATE.UNLOCKING) {
                Log.e(TAG, "compare failed: current state: " + mCurrentState);
                return -1;
            }
            int compare = FaceUnlockVendorImpl.getInstance().compare(bArr, i, i2, i3, z, z2, iArr);
            if (Util.DEBUG) {
                Log.i(TAG, "compare finish: " + compare);
            }
            if (compare == 0) {
                compareStop();
            }
            return compare;
        }
    }

    public void compareStop() {
        synchronized (this) {
            if (mCurrentState != SERVICE_STATE.UNLOCKING) {
                Log.e(TAG, "compareStop failed: current state: " + mCurrentState);
                return;
            }
            if (Util.DEBUG) {
                Log.i(TAG, "compareStop");
            }
            FaceUnlockVendorImpl.getInstance().reset();
            mCurrentState = SERVICE_STATE.IDLE;
        }
    }

    public void saveFeatureStart() {
        synchronized (this) {
            if (mCurrentState == SERVICE_STATE.INITING) {
                init();
            } else if (mCurrentState == SERVICE_STATE.UNLOCKING) {
                Log.e(TAG, "save feature, stop unlock");
                compareStop();
            }
            if (mCurrentState != SERVICE_STATE.IDLE) {
                Log.e(TAG, "saveFeatureStart failed: current state: " + mCurrentState);
            }
            if (Util.DEBUG) {
                Log.i(TAG, "saveFeatureStart");
            }
            FaceUnlockVendorImpl.getInstance().prepare();
            mCurrentState = SERVICE_STATE.ENROLLING;
        }
    }

    public int saveFeature(byte[] bArr, int i, int i2, int i3, boolean z, byte[] bArr2, byte[] bArr3, int[] iArr) {
        synchronized (this) {
            if (mCurrentState != SERVICE_STATE.ENROLLING) {
                Log.e(TAG, "save feature failed , current state : " + mCurrentState);
                return -1;
            }
            if (Util.DEBUG) {
                Log.i(TAG, "saveFeature");
            }
            return FaceUnlockVendorImpl.getInstance().saveFeature(bArr, i, i2, i3, z, bArr2, bArr3, iArr);
        }
    }

    public void saveFeatureStop() {
        synchronized (this) {
            if (mCurrentState != SERVICE_STATE.ENROLLING) {
                Log.d(TAG, "saveFeatureStop failed: current state: " + mCurrentState);
            }
            if (Util.DEBUG) {
                Log.i(TAG, "saveFeatureStop");
            }
            FaceUnlockVendorImpl.getInstance().reset();
            mCurrentState = SERVICE_STATE.IDLE;
        }
    }

    public void setDetectArea(int i, int i2, int i3, int i4) {
        synchronized (this) {
            if (Util.DEBUG) {
                Log.i(TAG, "setDetectArea start");
            }
            FaceUnlockVendorImpl.getInstance().setDetectArea(i, i2, i3, i4);
        }
    }

    public void deleteFeature(int i) {
        synchronized (this) {
            if (Util.DEBUG) {
                Log.i(TAG, "deleteFeature start");
            }
            FaceUnlockVendorImpl.getInstance().deleteFeature(i);
            if (Util.DEBUG) {
                Log.i(TAG, "deleteFeature stop");
            }
            release();
        }
    }

    public void release() {
        synchronized (this) {
            if (mCurrentState == SERVICE_STATE.INITING) {
                if (Util.DEBUG) {
                    Log.i(TAG, "has been released, ignore");
                }
                return;
            }
            if (Util.DEBUG) {
                Log.i(TAG, "release start");
            }
            FaceUnlockVendorImpl.getInstance().release();
            mCurrentState = SERVICE_STATE.INITING;
            if (Util.DEBUG) {
                Log.i(TAG, "release stop");
            }
        }
    }

    private enum SERVICE_STATE {
        INITING,
        IDLE,
        ENROLLING,
        UNLOCKING,
        ERROR
    }
}
