package ax.nd.faceunlock.camera;

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

import ax.nd.faceunlock.R;
import ax.nd.faceunlock.camera.listeners.ByteBufferCallbackListener;
import ax.nd.faceunlock.camera.listeners.CameraListener;
import ax.nd.faceunlock.camera.listeners.ErrorCallbackListener;
import ax.nd.faceunlock.camera.listeners.ReadParametersListener;
import ax.nd.faceunlock.util.Util;

import java.nio.ByteBuffer;

import static ax.nd.faceunlock.FaceConstants.MG_UNLOCK_FACE_NOT_FOUND;

public class CameraFaceAuthController {
    private static final int CAM_MSG_ERROR = 101;
    private static final int CAM_MSG_STATE_UPDATE = 102;
    private static final int CAM_MSG_OPEN = 103;
    private static final int MATCH_TIME_OUT_NO_FACE_MS = 3000;
    private static final int MATCH_TIME_OUT_WITH_FACE_MS = 4800;
    private static final int MSG_FACE_UNLOCK_COMPARE = 1003;
    private static final int MSG_FACE_UNLOCK_DETECT_AREA = 1004;
    private static final int MSG_TIME_OUT_NO_FACE = 1;
    private static final int MSG_TIME_OUT_WITH_FACE = 2;
    private static final String TAG = CameraFaceAuthController.class.getSimpleName();
    private static HandlerThread mFaceUnlockThread;
    private final Context mContext;
    private final Handler mHandler = new Handler(Looper.getMainLooper(), new HandlerCallback());
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
    private ServiceCallback mCallback;
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
    private boolean mCompareSuccess = false;
    private boolean mComparing = false;
    private Handler mFaceUnlockHandler;
    private final ByteBufferCallbackListener mByteBufferListener = (i, byteBuffer) -> {
        if (!this.mComparing) {
            mComparing = true;
            if (Util.DEBUG) {
                Log.d(CameraFaceAuthController.TAG, "Camera frame arrival");
            }
            Message obtain = Message.obtain(this.mFaceUnlockHandler, MSG_FACE_UNLOCK_COMPARE);
            obtain.obj = byteBuffer;
            this.mFaceUnlockHandler.sendMessage(obtain);
        }
    };
    private ByteBuffer mFrame;
    @SuppressWarnings("deprecation")
    private Camera.Size mPreviewSize;
    private boolean mStop = false;
    private SurfaceTexture mTexture = null;
    private int mCamOpenDelay;

    public CameraFaceAuthController(Context context, ServiceCallback serviceCallback) {
        mContext = context;
        mCallback = serviceCallback;
        mCamID = CameraUtil.getFrontFacingCameraId(context);
        mCamOpenDelay = context.getResources().getInteger(R.integer.cam_open_delay_ms);
    }

    public void start() {
        Log.i(TAG, "start enter");
        mHandler.sendEmptyMessageDelayed(CAM_MSG_OPEN, mCamOpenDelay);
    }

    private void handleCameraOpen() {
        Log.i(TAG, "start enter");
        if (mCamID == -1) {
            Log.d(TAG, "No front camera, stop face unlock");
            return;
        }
        initWorkHandler();
        CameraService.openCamera(mCamID, mErrorCallbackListener, mCameraListener);
        mCameraState = CameraState.CAMERA_OPENED;
        resetTimeout(0);
        mStop = false;
        Log.i(TAG, "start exit");
    }

    private void stopSelf() {
        mHandler.post(CameraFaceAuthController.this::stop);
    }

    public void stop() {
        Log.i(TAG, "stop enter");
        if (mFaceUnlockHandler != null) {
            mFaceUnlockHandler.removeMessages(MSG_FACE_UNLOCK_COMPARE);
            mFaceUnlockHandler.removeMessages(MSG_FACE_UNLOCK_DETECT_AREA);
        }
        if (mHandler != null) {
            mHandler.removeMessages(CAM_MSG_OPEN);
            mHandler.removeMessages(MSG_TIME_OUT_NO_FACE);
            mHandler.removeMessages(MSG_TIME_OUT_WITH_FACE);
        }
        CameraService.clearQueue();
        if (mCameraState == CameraState.CAMERA_PREVIEW_STARTED) {
            CameraService.addCallbackBuffer(null, null);
            mFrame = null;
            mCameraState = CameraState.CAMERA_PREVIEW_STOPPING;
            CameraService.stopPreview(null);
            CameraService.closeCamera(null);
        } else if (mCameraState != CameraState.CAMERA_IDLE) {
            CameraService.closeCamera(null);
        }
        mCallback = null;
        mStop = true;
        if (!mCompareSuccess) {
            mCompareSuccess = false;
        }
        Log.i(TAG, "stop exit");
    }

    public void resetTimeout(int i) {
        mHandler.removeMessages(MSG_TIME_OUT_NO_FACE);
        mHandler.removeMessages(MSG_TIME_OUT_WITH_FACE);
        mHandler.sendEmptyMessageDelayed(MSG_TIME_OUT_NO_FACE, i > 0 ? (long) i : MATCH_TIME_OUT_NO_FACE_MS);
        mHandler.sendEmptyMessageDelayed(MSG_TIME_OUT_WITH_FACE, i > 0 ? (long) i : MATCH_TIME_OUT_WITH_FACE_MS);
    }

    private void initWorkHandler() {
        if (mFaceUnlockThread == null) {
            mFaceUnlockThread = new HandlerThread("Camera Face unlock");
            mFaceUnlockThread.setPriority(Process.THREAD_PRIORITY_BACKGROUND);
            mFaceUnlockThread.start();
        }
        mFaceUnlockHandler = new FaceHandler(mFaceUnlockThread.getLooper());
    }

    @SuppressWarnings("deprecation")
    private void handleCameraStateUpdate() {
        if (!mStop) {
            int i = CameraStateOrdinal.STATE[mCameraState.ordinal()];
            if (i == 1) {
                mCameraState = CameraState.CAMERA_PARAM_READ;
                CameraService.readParameters(mReadParamListener, mCameraListener);
            } else if (i == 2) {
                mCameraState = CameraState.CAMERA_PARAM_SET;
                mPreviewSize = CameraUtil.calBestPreviewSize(mCameraParam, 480, 640);
                int width = mPreviewSize.width;
                int height = mPreviewSize.height;
                mCameraParam.setPreviewSize(width, height);
                mCameraParam.setPreviewFormat(ImageFormat.NV21);
                mFrame = ByteBuffer.allocateDirect(getPreviewBufferSize(width, height));
                CameraService.writeParameters(mCameraListener);
                Log.d(TAG, "preview size " + mPreviewSize.height + " " + mPreviewSize.width);
                mFaceUnlockHandler.sendEmptyMessage(MSG_FACE_UNLOCK_DETECT_AREA);
            } else if (i == 3) {
                mCameraState = CameraState.CAMERA_PREVIEW_STARTED;
                if (mTexture == null) {
                    mTexture = new SurfaceTexture(10);
                }
                CameraService.addCallbackBuffer(mFrame.array(), null);
                CameraService.setPreviewCallback(mByteBufferListener, true, null);
                CameraService.startPreview(mTexture, mCameraListener);
            } else if (i == 4) {
                CameraService.closeCamera(null);
            }
        }
    }

    private int getPreviewBufferSize(int i, int i2) {
        return (((i2 * i) * ImageFormat.getBitsPerPixel(ImageFormat.NV21)) / 8) + 32;
    }

    private enum CameraState {
        CAMERA_IDLE,
        CAMERA_OPENED,
        CAMERA_PARAM_READ,
        CAMERA_PARAM_SET,
        CAMERA_PREVIEW_STARTED,
        CAMERA_PREVIEW_STOPPING
    }

    public interface ServiceCallback {
        int handlePreviewData(byte[] bArr, int i, int i2);

        void onCameraError();

        void onTimeout(boolean z);

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
                Log.i(CameraFaceAuthController.TAG, "handleMessage : " + message);
            }
            if (message.what == MSG_TIME_OUT_NO_FACE || message.what == MSG_TIME_OUT_WITH_FACE) {
                boolean z = false;
                if (Util.DEBUG) {
                    Log.d(CameraFaceAuthController.TAG, "timeout, sendBroadcast faceId stop");
                }
                stopSelf();
                if (mCallback != null) {
                    if (message.what == MSG_TIME_OUT_WITH_FACE) {
                        z = true;
                    }
                    mCallback.onTimeout(z);
                }
            } else if (message.what == CAM_MSG_ERROR) {
                stopSelf();
                if (mCallback != null) {
                    mCallback.onCameraError();
                }
            } else if (message.what == CAM_MSG_STATE_UPDATE) {
                handleCameraStateUpdate();
            } else if (message.what == CAM_MSG_OPEN) {
                handleCameraOpen();
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
            if (Util.DEBUG) {
                Log.i(CameraFaceAuthController.TAG, "FaceHandler handle msg : " + message);
            }
            if (message.what == MSG_FACE_UNLOCK_COMPARE) {
                ByteBuffer byteBuffer = (ByteBuffer) message.obj;
                int i = 0;
                if (mCallback != null) {
                    i = mCallback.handlePreviewData(byteBuffer.array(), mPreviewSize.width, mPreviewSize.height);
                }
                if (i == 0) {
                    mCompareSuccess = true;
                    return;
                }
                if (i != MG_UNLOCK_FACE_NOT_FOUND) {
                    mHandler.removeMessages(MSG_TIME_OUT_NO_FACE);
                }
                if (mFrame != null) {
                    CameraService.addCallbackBuffer(mFrame.array(), null);
                    mComparing = false;
                }
            } else if (message.what == MSG_FACE_UNLOCK_DETECT_AREA && mCallback != null) {
                mCallback.setDetectArea(mPreviewSize);
            }
        }
    }
}
