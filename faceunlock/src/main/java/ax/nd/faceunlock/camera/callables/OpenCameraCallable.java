package ax.nd.faceunlock.camera.callables;

import android.hardware.Camera;
import android.util.Log;

import ax.nd.faceunlock.camera.CameraHandlerThread;
import ax.nd.faceunlock.camera.listeners.CameraListener;
import ax.nd.faceunlock.camera.listeners.ErrorCallbackListener;
import ax.nd.faceunlock.util.Util;

@SuppressWarnings("rawtypes")
public class OpenCameraCallable extends CameraCallable {
    private static final String TAG = OpenCameraCallable.class.getSimpleName();
    private final int mCameraId;
    private final ErrorCallbackListener mErrorListener;

    public OpenCameraCallable(int i, ErrorCallbackListener errorCallbackListener, CameraListener cameraListener) {
        super(cameraListener);
        mCameraId = i;
        mErrorListener = errorCallbackListener;
    }

    @SuppressWarnings("deprecation")
    private static Camera openCamera(int i) {
        return Camera.open(i);
    }

    @Override
    public CallableReturn call() {
        if (Util.DEBUG) {
            Log.d(TAG, "device: connect device async task: start");
        }
        if (getCameraData().mCamera != null && getCameraData().mCameraId == mCameraId) {
            if (Util.DEBUG) {
                Log.d(TAG, "Camera is already opened");
            }
            setErrorCallback(getCameraData().mCamera);
            return new CallableReturn(null);
        } else if (getCameraData().mCamera != null) {
            return new CallableReturn(new Exception("Other camera is all ready opened"));
        } else {
            try {
                openCamera();
                if (Util.DEBUG) {
                    Log.d(TAG, "device: connect device async task:open camera complete");
                }
                return new CallableReturn(null);
            } catch (Exception e) {
                return new CallableReturn(e);
            }
        }
    }

    @Override
    public String getTag() {
        return TAG;
    }

    private void openCamera() {
        CameraHandlerThread.CameraData cameraData = getCameraData();
        try {
            if (Util.DEBUG) {
                Log.d(TAG, "open camera " + mCameraId);
            }
            if (cameraData.mCameraId != mCameraId) {
                cameraData.mCamera = openCamera(mCameraId);
                cameraData.mCameraId = mCameraId;
            }
            if (Util.DEBUG) {
                Log.d(TAG, "open camera success, id: " + getCameraData().mCameraId);
            }
            setErrorCallback(cameraData.mCamera);
        } catch (RuntimeException e) {
            if (Util.DEBUG) {
                Log.e(TAG, "fail to connect Camera", e);
            }
        }
    }

    @SuppressWarnings("deprecation")
    private void setErrorCallback(Camera camera) {
        if (Util.DEBUG) {
            Log.d(TAG, "set error callback");
        }
        camera.setErrorCallback((i, camera1) -> {
            if (mErrorListener != null) {
                mErrorListener.onEventCallback(i, null);
            }
        });
    }
}
