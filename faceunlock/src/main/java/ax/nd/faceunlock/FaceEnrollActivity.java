package ax.nd.faceunlock;

import android.Manifest;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.hardware.Camera;

import ax.nd.faceunlock.service.FaceAuthService;
import ax.nd.faceunlock.stub.ContextShim;
import ax.nd.faceunlock.stub.biometrics.BiometricFaceConstants;
import ax.nd.faceunlock.stub.face.FaceManager;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.RemoteException;
import android.text.TextUtils;
import android.util.Log;
import android.widget.TextView;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import ax.nd.faceunlock.camera.CameraFaceEnrollController;
import ax.nd.faceunlock.util.CircleSurfaceView;
import ax.nd.faceunlock.util.Settings;
import ax.nd.faceunlock.util.SharedUtil;
import ax.nd.faceunlock.util.Util;

import static ax.nd.faceunlock.FaceConstants.MG_UNLOCK_DARKLIGHT;
import static ax.nd.faceunlock.FaceConstants.MG_UNLOCK_FACE_BLUR;
import static ax.nd.faceunlock.FaceConstants.MG_UNLOCK_FACE_MULTI;
import static ax.nd.faceunlock.FaceConstants.MG_UNLOCK_FACE_NOT_COMPLETE;
import static ax.nd.faceunlock.FaceConstants.MG_UNLOCK_FACE_OFFSET_LEFT;
import static ax.nd.faceunlock.FaceConstants.MG_UNLOCK_FACE_OFFSET_RIGHT;
import static ax.nd.faceunlock.FaceConstants.MG_UNLOCK_FACE_ROTATED_LEFT;
import static ax.nd.faceunlock.FaceConstants.MG_UNLOCK_FACE_ROTATED_RIGHT;
import static ax.nd.faceunlock.FaceConstants.MG_UNLOCK_FACE_SCALE_TOO_LARGE;
import static ax.nd.faceunlock.FaceConstants.MG_UNLOCK_FACE_SCALE_TOO_SMALL;
import static ax.nd.faceunlock.FaceConstants.MG_UNLOCK_FAILED;
import static ax.nd.faceunlock.FaceConstants.MG_UNLOCK_HALF_SHADOW;
import static ax.nd.faceunlock.FaceConstants.MG_UNLOCK_HIGHLIGHT;
import static ax.nd.faceunlock.FaceConstants.MG_UNLOCK_KEEP;

import com.android.internal.util.custom.faceunlock.IFaceService;
import com.android.internal.util.custom.faceunlock.IFaceServiceReceiver;

public class FaceEnrollActivity extends FaceBaseActivity {
    private static final String TAG = FaceEnrollActivity.class.getSimpleName();
    protected Handler mHandler;
    protected HandlerThread mHandlerThread;
    protected CancellationSignal mEnrollmentCancel = new CancellationSignal();
    protected SharedUtil mSharedUtil;
    private TextView mFaceEnrollMsg;
    private boolean mHasCameraPermission = false;
    private CameraFaceEnrollController mCameraEnrollService;
    private FaceManager mFaceManager;
    private boolean mIsActivityPaused = false;
    private boolean mIsFaceDetected = false;
    private float mPercent = 0.0f;
    private CircleSurfaceView mSurface;
    private final CameraFaceEnrollController.CameraCallback mCameraCallback = new CameraFaceEnrollController.CameraCallback() {
        @Override
        public int handleSaveFeature(byte[] bArr, int i, int i2, int i3) {
            return -1;
        }

        @SuppressWarnings("deprecation")
        @Override
        public void setDetectArea(Camera.Size size) {
        }

        @Override
        public void handleSaveFeatureResult(int error) {
            runOnUiThread(() -> {
                int errorString = 0;
                switch (error) {
                    case MG_UNLOCK_FACE_SCALE_TOO_SMALL:
                        errorString = R.string.unlock_failed_face_small;
                        break;
                    case MG_UNLOCK_FACE_SCALE_TOO_LARGE:
                        errorString = R.string.unlock_failed_face_large;
                        break;
                    case MG_UNLOCK_FACE_OFFSET_LEFT:
                    case MG_UNLOCK_FACE_OFFSET_RIGHT:
                    case MG_UNLOCK_FACE_ROTATED_LEFT:
                    case MG_UNLOCK_FACE_ROTATED_RIGHT:
                        mPercent += 10.0f;
                        break;
                    case MG_UNLOCK_KEEP:
                        mPercent += 10.0f;
                        mIsFaceDetected = true;
                        break;
                    case MG_UNLOCK_FACE_MULTI:
                        errorString = R.string.unlock_failed_face_multi;
                        break;
                    case MG_UNLOCK_FACE_BLUR:
                        errorString = R.string.unlock_failed_face_blur;
                        break;
                    case MG_UNLOCK_FACE_NOT_COMPLETE:
                        errorString = R.string.unlock_failed_face_not_complete;
                        break;
                    case MG_UNLOCK_DARKLIGHT:
                        errorString = R.string.attr_light_dark;
                        break;
                    case MG_UNLOCK_HIGHLIGHT:
                        errorString = R.string.attr_light_high;
                        break;
                    case MG_UNLOCK_HALF_SHADOW:
                        errorString = R.string.attr_light_shadow;
                        break;
                }
                if (mPercent < 100.0f) {
                    if (mIsFaceDetected) {
                        if (mPercent >= 60.0f) {
                            mPercent = 60.0f;
                        }
                        mSurface.setPercent(mPercent);
                    }
                    if (errorString != 0) {
                        mFaceEnrollMsg.setText(errorString);
                    }
                }
            });
        }

        @Override
        public void onTimeout() {
            if (!isFinishing() && !isDestroyed()) {
                if (Settings.isFaceUnlockAvailable(FaceEnrollActivity.this)) {
                    startFinishActivity();
                    return;
                }
                if (!mIsActivityPaused) {
                    Intent intent = new Intent();
                    intent.setClass(FaceEnrollActivity.this, FaceTryAgain.class);
                    parseIntent(intent);
                    startActivity(intent);
                }
                finish();
            }
        }

        @Override
        public void onCameraError() {
            if (!isFinishing() && !isDestroyed()) {
                if (!mIsActivityPaused) {
                    Intent intent = new Intent();
                    intent.setClass(FaceEnrollActivity.this, FaceTryAgain.class);
                    parseIntent(intent);
                    startActivity(intent);
                }
                finish();
            }
        }

        @Override
        public void onFaceDetected() {
            mIsFaceDetected = true;
        }
    };
    private final FaceManager.EnrollmentCallback mEnrollmentCallback = new FaceManager.EnrollmentCallback() {
        @Override
        public void onEnrollmentProgress(int i) {
            if (i == 0) {
                runOnUiThread(() -> {
                    mPercent = 100.0f;
                    mSurface.setPercent(mPercent);
                    try {
                        if (mFaceEnrollMsg != null) {
                            mFaceEnrollMsg.setText("");
                        }
                        new Handler(Looper.getMainLooper()).postDelayed(() -> {
                            FaceEnrollActivity faceEnrollActivity = FaceEnrollActivity.this;
                            if (faceEnrollActivity == null || !faceEnrollActivity.isDestroyed()) {
                                startFinishActivity();
                            }
                        }, 2000);
                    } catch (Exception ignored) {
                    }
                });
            }
        }

        @Override
        public void onEnrollmentHelp(int i, final CharSequence charSequence) {
            runOnUiThread(() -> {
                if (!TextUtils.isEmpty(charSequence)) {
                    mFaceEnrollMsg.setText(charSequence);
                }
            });
        }

        @Override
        public void onEnrollmentError(int i, CharSequence charSequence) {
            if (!mIsActivityPaused) {
                Intent intent = new Intent();
                intent.setClass(FaceEnrollActivity.this, FaceTryAgain.class);
                if (i != MG_UNLOCK_FAILED) {
                    parseIntent(intent);
                } else {
                    setResult(-1);
                }
                startActivity(intent);
            }
            finish();
        }
    };

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        initData();
        mHasCameraPermission = false;
        mFaceManager = new ContextShim(this).getSystemService(FaceManager.class);
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != 0) {
            boolean shouldShowRequestPermissionRationale = ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA);
            if (Util.DEBUG) {
                Log.i(TAG, "shouldShowRequestPermissionRationale: " + shouldShowRequestPermissionRationale);
            }
            requestPermissions(new String[]{Manifest.permission.CAMERA}, 0);
            return;
        }
        synchronized (this) {
            mHasCameraPermission = true;
            if (Util.DEBUG) {
                Log.i(TAG, "hasCameraPermission: > M");
            }
        }
    }

    private void initData() {
        mSharedUtil = new SharedUtil(this);
    }

    boolean enrollCalled = false;
    private ServiceConnection faceAuthConn = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            IFaceServiceReceiver authCallback = new IFaceServiceReceiver.Stub() {
                @Override
                public void onEnrollResult(int faceId, int userId, int remaining) throws RemoteException {
                    Log.d(TAG, "onEnrollResult: faceId=" + faceId + ", userId=" + userId + ", remaining=" + remaining);
                    mEnrollmentCallback.onEnrollmentProgress(remaining);
                }

                @Override
                public void onAuthenticated(int faceId, int userId, byte[] token) throws RemoteException {
                    Log.wtf(TAG, "onAuthenticated called in FaceEnrollActivity???");
                    // Not used, safe to skip
                }

                @Override
                public void onError(int error, int vendorCode) throws RemoteException {
                    int clientErrId = FaceManager.calcClientMsgId(error, vendorCode);
                    String message = FaceManager.getErrorString(FaceEnrollActivity.this, error, vendorCode);
                    Log.d(TAG, "onError: " + clientErrId + ", " + error + ", " + vendorCode + ", " + message);
                    mEnrollmentCallback.onEnrollmentError(clientErrId, message);
                }

                @Override
                public void onRemoved(int[] faceIds, int userId) throws RemoteException {
                    Log.wtf(TAG, "onRemoved called in FaceEnrollActivity???");
                    // Not used, safe to skip
                }

                @Override
                public void onEnumerate(int[] faceIds, int userId) throws RemoteException {
                    Log.wtf(TAG, "onEnumerate called in FaceEnrollActivity???");
                    // Not used, safe to skip
                }
            };
            IFaceService authBinder = (IFaceService) iBinder;
            try {
                authBinder.setCallback(authCallback);
            } catch (RemoteException e) {
                Log.e(TAG, "Callback setup error!", e);
            }

            if(!enrollCalled) {
                enrollCalled = true;
                try {
                    authBinder.enroll(
                            new byte[]{0}, // TODO supposed to be mToken but it's always null...
                            75, // ENROLL_TIMEOUT_SEC in FaceService.java
                            new int[]{1}
                    );
                } catch (RemoteException e) {
                    Log.e(TAG, "Enroll error!", e);
                    try {
                        authCallback.onError(BiometricFaceConstants.FACE_ERROR_UNKNOWN, 0);
                    } catch (RemoteException remoteException) {
                        Log.e(TAG, "authCallback.onError error!", e);
                    }
                }
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
        }
    };

    private void init() {
        setContentView(R.layout.face_enroll);
        mSurface = findViewById(R.id.camera_surface);
        mPercent = 0.0f;
        mSurface.setPercent(0.0f);
        mSurface.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (mIsFaceDetected) {
                    mPercent += 5.0f;
                }
                if (mPercent < 60.0f) {
                    mSurface.setPercent(mPercent);
                    mSurface.postDelayed(this, 500);
                }
            }
        }, 500);
        if (mCameraEnrollService == null) {
            mCameraEnrollService = CameraFaceEnrollController.getInstance();
            mCameraEnrollService.setSurfaceHolder(mSurface.getHolder());
        }

        // ND: NEW
        enrollCalled = false;
        bindService(new Intent(this, FaceAuthService.class), faceAuthConn, Context.BIND_AUTO_CREATE);
//        mFaceManager.enroll(Util.getUserId(this), mToken, mEnrollmentCancel, mEnrollmentCallback, new int[]{1});

        mCameraEnrollService.start(mCameraCallback, 15000);
        mFaceEnrollMsg = findViewById(R.id.face_msg);
        findViewById(R.id.enroll_done).setOnClickListener(view -> {
            setResult(-1);
            finish();
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "onDestroy");
        if (mHandler != null) {
            mHandler.post(() -> {
                Log.i(FaceEnrollActivity.TAG, "onDestroy handlerThread.quit()");
                mHandlerThread.quit();
            });
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        mIsActivityPaused = true;
        if (mCameraEnrollService != null) {
            mCameraEnrollService.setSurfaceHolder(null);
            mCameraEnrollService.stop(mCameraCallback);
            mCameraEnrollService = null;
        }
        mEnrollmentCancel.cancel();
        unbindService(faceAuthConn);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mIsActivityPaused = false;
        synchronized (this) {
            if (mHasCameraPermission) {
                if (Util.DEBUG) {
                    Log.i(TAG, "onResume");
                }
                init();
            }
        }
    }

    private void showPermissionRequiredDialog() {
        new AlertDialog.Builder(this, R.style.AppTheme_AlertDialog)
                .setTitle(R.string.perm_required_alert_title)
                .setMessage(R.string.perm_required_alert_msg)
                .setPositiveButton(R.string.perm_required_alert_button_app_info,
                        (dialogInterface, i) -> Util.jumpToAppInfo(this, 2)).setOnCancelListener(dialogInterface -> finish()).show();
    }

    private void startFinishActivity() {
        Intent intent = new Intent();
        intent.setClass(this, FaceFinish.class);
        intent.putExtra(SetupFaceIntroActivity.EXTRA_ENROLL_SUCCESS, true);
        startActivityForResult(intent, 1);
    }

    @Override
    protected void onActivityResult(int i, int i2, Intent intent) {
        super.onActivityResult(i, i2, intent);
        if (i == 1) {
            setResult(i2);
            finish();
        } else if (i == 2) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != 0) {
                if (Util.DEBUG) {
                    Log.i(TAG, "REQUEST_CAMERA finish");
                }
                finish();
                return;
            }
            if (Util.DEBUG) {
                Log.i(TAG, "REQUEST_CAMERA init");
            }
            synchronized (this) {
                if (Util.DEBUG) {
                    Log.i(TAG, "hasCameraPermission: REQUEST_CAMERA");
                }
                mHasCameraPermission = true;
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int i, String[] strArr, int[] iArr) {
        if (i == 0 && SetupFaceIntroActivity.hasAllPermissionsGranted(iArr)) {
            synchronized (this) {
                if (Util.DEBUG) {
                    Log.i(TAG, "hasCameraPermission: onRequestPermissionsResult");
                }
                mHasCameraPermission = true;
            }
        } else if (!ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA)) {
            showPermissionRequiredDialog();
        } else {
            finish();
        }
    }
}
