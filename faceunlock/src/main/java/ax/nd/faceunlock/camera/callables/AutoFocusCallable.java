package ax.nd.faceunlock.camera.callables;

import android.hardware.Camera;

import ax.nd.faceunlock.camera.listeners.CameraListener;
import ax.nd.faceunlock.camera.listeners.FocusResultListener;

public class AutoFocusCallable extends CameraCallable {
    private static final String TAG = AutoFocusCallable.class.getSimpleName();
    private final boolean mEnable;
    private final FocusResultListener mFocusResultListener;

    public AutoFocusCallable(boolean z, FocusResultListener focusResultListener, CameraListener cameraListener) {
        super(cameraListener);
        mEnable = z;
        mFocusResultListener = focusResultListener;
    }

    @SuppressWarnings("deprecation")
    @Override
    public CallableReturn call() {
        Camera camera = getCameraData().mCamera;
        if (camera == null) {
            return new CallableReturn(new Exception("Camera isn't opened"));
        }
        if (mEnable) {
            camera.autoFocus(new AutoFocusCallbackWrapper(mFocusResultListener));
        } else {
            camera.cancelAutoFocus();
        }
        return new CallableReturn(null);
    }

    @Override
    public String getTag() {
        return TAG;
    }

    @SuppressWarnings("deprecation")
    private static class AutoFocusCallbackWrapper implements Camera.AutoFocusCallback {
        private final FocusResultListener mListener;

        private AutoFocusCallbackWrapper(FocusResultListener focusResultListener) {
            mListener = focusResultListener;
        }

        @SuppressWarnings("deprecation")
        @Override
        public void onAutoFocus(final boolean z, Camera camera) {
            CameraCallable.runOnUiThread(() -> {
                if (mListener != null) {
                    mListener.onEventCallback(0, z);
                }
            });
        }
    }
}
