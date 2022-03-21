package ax.nd.faceunlock.camera.callables;

import android.hardware.Camera;

import ax.nd.faceunlock.camera.listeners.CameraListener;
import ax.nd.faceunlock.camera.listeners.ReadParametersListener;

@SuppressWarnings("rawtypes")
public class ReadParamsCallable extends CameraCallable {
    private static final String TAG = ReadParamsCallable.class.getSimpleName();
    ReadParametersListener mReadListener;

    public ReadParamsCallable(ReadParametersListener readParametersListener, CameraListener cameraListener) {
        super(cameraListener);
        mReadListener = readParametersListener;
    }

    @SuppressWarnings("deprecation")
    @Override
    public CallableReturn call() {
        Camera camera = getCameraData().mCamera;
        if (camera == null) {
            return new CallableReturn(new Exception("Camera isn't opened"));
        }
        try {
            getCameraData().mParameters = camera.getParameters();
            mReadListener.onEventCallback(0, getCameraData().mParameters);
            return new CallableReturn(null);
        } catch (Exception e) {
            return new CallableReturn(e);
        }
    }

    @Override
    public String getTag() {
        return TAG;
    }
}
