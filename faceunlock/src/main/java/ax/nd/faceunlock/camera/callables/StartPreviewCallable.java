package ax.nd.faceunlock.camera.callables;

import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.util.Log;
import android.view.SurfaceHolder;

import ax.nd.faceunlock.camera.listeners.CallableListener;
import ax.nd.faceunlock.camera.listeners.CameraListener;

import java.io.IOException;

public class StartPreviewCallable extends CameraCallable {
    private static final String TAG = StartPreviewCallable.class.getSimpleName();
    private final SurfaceHolder mSurfaceHolder;
    private final SurfaceTexture mSurfaceTexture;

    public StartPreviewCallable(CameraListener cameraListener) {
        super(cameraListener);
        mSurfaceTexture = null;
        mSurfaceHolder = null;
    }

    public StartPreviewCallable(SurfaceTexture surfaceTexture, CameraListener cameraListener) {
        super(cameraListener);
        mSurfaceTexture = surfaceTexture;
        mSurfaceHolder = null;
    }

    public StartPreviewCallable(SurfaceHolder surfaceHolder, CameraListener cameraListener) {
        super(cameraListener);
        mSurfaceTexture = null;
        mSurfaceHolder = surfaceHolder;
    }

    @SuppressWarnings("deprecation")
    private static void startPreview(Camera camera) {
        camera.startPreview();
    }

    @SuppressWarnings("deprecation")
    @Override
    public CallableReturn call() {
        Camera camera = getCameraData().mCamera;
        if (camera == null) {
            return new CallableReturn(new Exception("Camera isn't opened"));
        }
        try {
            if (mSurfaceTexture != null) {
                camera.setPreviewTexture(mSurfaceTexture);
            } else if (mSurfaceHolder != null) {
                camera.setPreviewDisplay(mSurfaceHolder);
            }
            try {
                startPreview(camera);
                return new CallableReturn(null);
            } catch (RuntimeException e) {
                return new CallableReturn(e);
            }
        } catch (IOException e2) {
            Log.e(TAG, "setPreviewDisplay failed.");
            return new CallableReturn(e2);
        }
    }

    @Override
    public String getTag() {
        return TAG;
    }

    @Override
    public void callback(CallableReturn callableReturn) {
        CallableListener callableListener;
        if (callableReturn.exception != null && (callableListener = mCameraListener.get()) != null) {
            callableListener.onError(callableReturn.exception);
        }
    }
}
