package ax.nd.faceunlock.camera.callables;

import android.hardware.Camera;
import android.os.SystemClock;
import android.util.Log;

import ax.nd.faceunlock.FaceApplication;
import ax.nd.faceunlock.camera.CameraHandlerThread;
import ax.nd.faceunlock.camera.listeners.CallableListener;
import ax.nd.faceunlock.util.Util;

import java.lang.ref.WeakReference;

public abstract class CameraCallable {
    protected final WeakReference<CallableListener> mCameraListener;
    private long mBegin;

    public CameraCallable(CallableListener callableListener) {
        mCameraListener = new WeakReference<>(callableListener);
    }

    protected static void runOnUiThread(Runnable runnable) {
        FaceApplication.getApp().postRunnable(runnable);
    }

    public abstract CallableReturn call();

    public abstract String getTag();

    public CameraHandlerThread.CameraData getCameraData() {
        return ((CameraHandlerThread) Thread.currentThread()).getCameraData();
    }

    @SuppressWarnings("deprecation")
    public Camera.Parameters getCameraParameters() {
        return ((CameraHandlerThread) Thread.currentThread()).getCameraData().mParameters;
    }

    public void run() {
        if (Util.DEBUG) {
            Log.d(getTag(), "Begin");
        }
        mBegin = SystemClock.elapsedRealtime();
        final CallableReturn call = call();
        if (Util.DEBUG) {
            String tag = getTag();
            Log.d(tag, "End (dur:" + (SystemClock.elapsedRealtime() - mBegin) + ")");
        }
        runOnUiThread(() -> callback(call));
    }

    public void callback(CallableReturn callableReturn) {
        long elapsedRealtime = SystemClock.elapsedRealtime() - mBegin;
        CallableListener callableListener = mCameraListener.get();
        if (callableReturn.exception != null) {
            String tag = getTag();
            Log.w(tag, "Exception in result (dur:" + elapsedRealtime + ")", callableReturn.exception);
            if (callableListener != null) {
                callableListener.onError(callableReturn.exception);
                return;
            }
            return;
        }
        String tag2 = getTag();
        Log.d(tag2, "Result success (dur:" + elapsedRealtime + ")");
        if (callableListener != null) {
            callableListener.onComplete(callableReturn.value);
        }
    }
}
