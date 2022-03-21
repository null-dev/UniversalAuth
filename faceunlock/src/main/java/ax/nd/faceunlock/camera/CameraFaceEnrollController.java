package ax.nd.faceunlock.camera;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.WindowManager;

import ax.nd.faceunlock.FaceApplication;
import ax.nd.faceunlock.camera.listeners.ByteBufferCallbackListener;
import ax.nd.faceunlock.camera.listeners.CameraListener;
import ax.nd.faceunlock.camera.listeners.ErrorCallbackListener;
import ax.nd.faceunlock.camera.listeners.ReadParametersListener;
import ax.nd.faceunlock.util.Util;

import java.nio.ByteBuffer;
import java.util.ArrayList;

@SuppressWarnings("SynchronizeOnNonFinalField")
public class CameraFaceEnrollController {
    private static final int CAM_MSG_ERROR = 101;
    private static final int CAM_MSG_STATE_UPDATE = 102;
    private static final int CAM_MSG_SURFACE_CREATED = 103;
    private static final int MSG_FACE_HANDLE_DATA = 1003;
    private static final int MSG_FACE_UNLOCK_DETECT_AREA = 1004;
    private static final String TAG = CameraFaceEnrollController.class.getSimpleName();
    private static HandlerThread mFaceUnlockThread;
    @SuppressLint("StaticFieldLeak")
    private static CameraFaceEnrollController sInstance;
    private final ArrayList<CameraCallback> mCameraCallbacks = new ArrayList<>();
    private final Context mContext;
    private final Handler mHandler = new Handler(Looper.getMainLooper(), new CameraFaceEnrollController.HandlerCallback());
    private final CameraListener mCameraListener = new CameraListener() {
        @Override
        public void onComplete(Object unused) {
            mHandler.sendEmptyMessage(CAM_MSG_STATE_UPDATE);
        }

        @Override
        public void onError(Exception exc) {
            mHandler.sendEmptyMessage(CAM_MSG_ERROR);
        }
    };
    private final int mCamID;
    protected ErrorCallbackListener mErrorCallbackListener = (i, unused) -> mHandler.sendEmptyMessage(CAM_MSG_ERROR);
    @SuppressWarnings("deprecation")
    private Camera.Parameters mCameraParam;
    private final ReadParametersListener mReadParamListener = new ReadParametersListener() {
        @SuppressWarnings("deprecation")
        @Override
        public void onEventCallback(int i, Object parameters) {
            mCameraParam = (Camera.Parameters) parameters;
        }
    };
    private CameraState mCameraState = CameraState.CAMERA_IDLE;
    private Handler mFaceUnlockHandler;
    private ByteBuffer mFrame;
    private boolean mHandling = false;
    private final ByteBufferCallbackListener mByteBufferListener = (i, byteBuffer) -> {
        if (Util.DEBUG) {
            Log.d(TAG, "Camera frame arrival " + this.mHandling);
        }
        if (!this.mHandling) {
            this.mHandling = true;
            Message obtain = Message.obtain(this.mFaceUnlockHandler, MSG_FACE_HANDLE_DATA);
            obtain.obj = byteBuffer;
            this.mFaceUnlockHandler.sendMessage(obtain);
        }
    };
    private SurfaceHolder mHolder;
    @SuppressWarnings("deprecation")
    private Camera.Size mPreviewSize;
    private boolean mPreviewStarted = false;
    private boolean mStop = false;
    private boolean mSurfaceCreated = false;

    private CameraFaceEnrollController(Context context) {
        mContext = context;
        mCamID = CameraUtil.getFrontFacingCameraId(context);
        initWorkHandler();
    }

    public static CameraFaceEnrollController getInstance() {
        if (sInstance == null) {
            sInstance = new CameraFaceEnrollController(FaceApplication.getApp());
        }
        return sInstance;
    }

    public void setSurfaceHolder(SurfaceHolder surfaceHolder) {
        if (surfaceHolder == null) {
            synchronized (mCameraState) {
                if (mCameraState == CameraState.CAMERA_PREVIEW_STARTED) {
                    CameraService.clearQueue();
                    mCameraState = CameraState.CAMERA_PREVIEW_STOPPING;
                    CameraService.setFaceDetectionCallback(null, null);
                    CameraService.stopPreview(null);
                    CameraService.setPreviewCallback(null, false, null);
                    CameraService.closeCamera(null);
                }
            }
        }
        mHolder = surfaceHolder;
        if (surfaceHolder != null) {
            surfaceHolder.setKeepScreenOn(true);
            mHolder.addCallback(new SurfaceHolder.Callback() {
                @Override
                public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i2, int i3) {
                }

                @Override
                public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
                }

                @Override
                public void surfaceCreated(SurfaceHolder surfaceHolder) {
                    mSurfaceCreated = true;
                    mHandler.sendEmptyMessage(CAM_MSG_SURFACE_CREATED);
                }
            });
            if (mHolder.getSurface() != null) {
                mSurfaceCreated = true;
            }
        }
    }

    public void start(final CameraCallback cameraCallback, int i) {
        Log.i(TAG, "new start : " + cameraCallback);
        synchronized (mCameraCallbacks) {
            if (mCameraCallbacks.contains(cameraCallback)) {
                return;
            }
            mCameraCallbacks.add(cameraCallback);
        }
        synchronized (mCameraState) {
            if (mCameraState == CameraState.CAMERA_IDLE) {
                if (!mHandler.hasMessages(CAM_MSG_STATE_UPDATE)) {
                    mHandler.sendEmptyMessage(CAM_MSG_STATE_UPDATE);
                }
                mHandling = false;
            }
        }
        if (i > 0) {
            mHandler.postDelayed(() -> {
                synchronized (mCameraState) {
                    if (mCameraCallbacks.contains(cameraCallback)) {
                        cameraCallback.onTimeout();
                    }
                }
            }, i);
        }
        mStop = false;
    }

    public void stop(CameraCallback cameraCallback) {
        Log.i(TAG, "stop : " + cameraCallback);
        synchronized (mCameraCallbacks) {
            if (!mCameraCallbacks.contains(cameraCallback)) {
                Log.e(TAG, "callback has been released!");
                return;
            }
            mCameraCallbacks.remove(cameraCallback);
            if (mCameraCallbacks.size() > 0) {
                return;
            }
        }
        mStop = true;
        Handler handler = mFaceUnlockHandler;
        if (handler != null) {
            handler.removeMessages(MSG_FACE_HANDLE_DATA);
            mFaceUnlockHandler.removeMessages(MSG_FACE_UNLOCK_DETECT_AREA);
        }
        synchronized (mCameraState) {
            CameraService.clearQueue();
            if (mCameraState == CameraState.CAMERA_PREVIEW_STARTED) {
                mCameraState = CameraState.CAMERA_PREVIEW_STOPPING;
                CameraService.setFaceDetectionCallback(null, null);
                CameraService.stopPreview(null);
                CameraService.setPreviewCallback(null, false, null);
                CameraService.closeCamera(null);
            } else if (mCameraState != CameraState.CAMERA_IDLE) {
                CameraService.setPreviewCallback(null, false, null);
                CameraService.closeCamera(null);
            }
        }
        mHolder = null;
        mCameraState = CameraState.CAMERA_IDLE;
        mCameraParam = null;
        mPreviewSize = null;
        mHandling = false;
    }

    @SuppressWarnings("deprecation")
    private void handleCameraStateUpdate() {
        if (!mStop) {
            synchronized (mCameraState) {
                switch (CameraStateOrdinal.STATE[mCameraState.ordinal()]) {
                    case 0:
                        if (mCamID != -1) {
                            CameraService.openCamera(mCamID, mErrorCallbackListener, mCameraListener);
                            mCameraState = CameraState.CAMERA_OPENED;
                            break;
                        } else {
                            Log.d(TAG, "No front camera, stop face unlock");
                            mHandler.sendEmptyMessage(CAM_MSG_ERROR);
                            return;
                        }
                    case 1:
                        mCameraState = CameraState.CAMERA_PARAM_READ;
                        CameraService.readParameters(mReadParamListener, mCameraListener);
                        break;
                    case 2:
                        mCameraState = CameraState.CAMERA_PARAM_SET;
                        mPreviewSize = CameraUtil.calBestPreviewSize(mCameraParam, 480, 640);
                        int width = mPreviewSize.width;
                        int height = mPreviewSize.height;
                        mCameraParam.setPreviewSize(width, height);
                        mCameraParam.setPreviewFormat(ImageFormat.NV21);
                        mFrame = ByteBuffer.allocateDirect(getPreviewBufferSize(width, height));
                        CameraService.writeParameters(mCameraListener);
                        Log.d(TAG, "preview size " + mPreviewSize.height + " " + mPreviewSize.width);
                        break;
                    case 3:
                        mCameraState = CameraState.CAMERA_PREVIEW_STARTED;
                        CameraService.addCallbackBuffer(mFrame.array(), null);
                        CameraService.setDisplayOrientationCallback(getCameraAngle(), null);
                        CameraService.setPreviewCallback(mByteBufferListener, true, null);
                        if (mHolder != null) {
                            if (mSurfaceCreated) {
                                CameraService.startPreview(mHolder, mCameraListener);
                                mPreviewStarted = true;
                                break;
                            }
                        } else {
                            SurfaceTexture surfaceTexture = new SurfaceTexture(10);
                            CameraService.startPreview(surfaceTexture, mCameraListener);
                            break;
                        }
                        break;
                    case 4:
                        CameraService.setFaceDetectionCallback((faceArr, camera) -> {
                            if (faceArr.length > 0) {
                                for (CameraCallback mCameraCallback : mCameraCallbacks) {
                                    mCameraCallback.onFaceDetected();
                                }
                            }
                        }, null);
                        break;
                    case 5:
                        CameraService.setPreviewCallback(null, false, null);
                        CameraService.closeCamera(null);
                        break;
                }
            }
        }
    }

    private void initWorkHandler() {
        if (mFaceUnlockThread == null) {
            mFaceUnlockThread = new HandlerThread("Camera Face unlock");
            mFaceUnlockThread.setPriority(Process.THREAD_PRIORITY_BACKGROUND);
            mFaceUnlockThread.start();
        }
        mFaceUnlockHandler = new FaceHandler(mFaceUnlockThread.getLooper());
    }

    private int getPreviewBufferSize(int i, int i2) {
        return (((i2 * i) * ImageFormat.getBitsPerPixel(ImageFormat.NV21)) / 8) + 32;
    }

    @SuppressWarnings("deprecation")
    private int getCameraAngle() {
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        Camera.getCameraInfo(mCamID, cameraInfo);
        int rotation = mContext.getSystemService(WindowManager.class).getDefaultDisplay().getRotation();
        int i = 0;
        if (rotation != 0) {
            if (rotation == 1) {
                i = 90;
            } else if (rotation == 2) {
                i = 180;
            } else if (rotation == 3) {
                i = 270;
            }
        }
        if (cameraInfo.facing == 1) {
            return (360 - ((cameraInfo.orientation + i) % 360)) % 360;
        }
        return ((cameraInfo.orientation - i) + 360) % 360;
    }

    private enum CameraState {
        CAMERA_IDLE,
        CAMERA_OPENED,
        CAMERA_PARAM_READ,
        CAMERA_PARAM_SET,
        CAMERA_PREVIEW_STARTED,
        CAMERA_PREVIEW_STOPPING
    }

    public interface CameraCallback {
        int handleSaveFeature(byte[] bArr, int i, int i2, int i3);

        void handleSaveFeatureResult(int i);

        void onCameraError();

        void onFaceDetected();

        void onTimeout();

        @SuppressWarnings("deprecation")
        void setDetectArea(Camera.Size size);
    }

    static class CameraStateOrdinal {
        static final int[] STATE = new int[CameraState.values().length];

        static {
            STATE[CameraState.CAMERA_OPENED.ordinal()] = 1;
            STATE[CameraState.CAMERA_PARAM_READ.ordinal()] = 2;
            STATE[CameraState.CAMERA_PARAM_SET.ordinal()] = 3;
            try {
                STATE[CameraState.CAMERA_PREVIEW_STOPPING.ordinal()] = 4;
            } catch (NoSuchFieldError ignored) {
            }
        }
    }

    class HandlerCallback implements Handler.Callback {
        @Override
        public boolean handleMessage(Message message) {
            if (Util.DEBUG) {
                Log.i(TAG, "handleMessage : " + message);
            }
            switch (message.what) {
                case CAM_MSG_ERROR:
                    for (CameraCallback mCameraCallback : mCameraCallbacks) {
                        mCameraCallback.onCameraError();
                    }
                    break;
                case CAM_MSG_STATE_UPDATE:
                    handleCameraStateUpdate();
                    break;
                case CAM_MSG_SURFACE_CREATED:
                    if (CameraState.CAMERA_PREVIEW_STARTED == mCameraState && !mPreviewStarted) {
                        CameraService.startPreview(mHolder, mCameraListener);
                    }
                    break;
            }
            return true;
        }
    }

    private class FaceHandler extends Handler {
        public FaceHandler(Looper looper) {
            super(looper);
        }

        @SuppressWarnings("deprecation")
        @Override
        public void handleMessage(Message message) {
            if (!mStop) {
                if (Util.DEBUG) {
                    Log.i(CameraFaceEnrollController.TAG, "FaceHandler handle msg : " + message);
                }
                int i = message.what;
                if (i == MSG_FACE_HANDLE_DATA) {
                    synchronized (mCameraCallbacks) {
                        ByteBuffer byteBuffer = (ByteBuffer) message.obj;
                        int i2 = -1;
                        for (CameraCallback mCameraCallback : mCameraCallbacks) {
                            int handleSaveFeature = mCameraCallback.handleSaveFeature(byteBuffer.array(), mPreviewSize.width, mPreviewSize.height, 0);
                            if (handleSaveFeature != -1) {
                                i2 = handleSaveFeature;
                            }
                        }
                        for (CameraCallback mCameraCallback : mCameraCallbacks) {
                            mCameraCallback.handleSaveFeatureResult(i2);
                        }
                        if (mFrame != null) {
                            CameraService.addCallbackBuffer(mFrame.array(), null);
                            mHandling = false;
                        }
                    }
                } else if (i == MSG_FACE_UNLOCK_DETECT_AREA) {
                    for (CameraCallback mCameraCallback : mCameraCallbacks) {
                        mCameraCallback.setDetectArea(mPreviewSize);
                    }
                }
            }
        }
    }
}
