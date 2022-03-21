package ax.nd.faceunlock.camera.callables;

import android.hardware.Camera;

import ax.nd.faceunlock.camera.listeners.CameraListener;

public class WriteParamsCallable extends CameraCallable {
    private static final String TAG = WriteParamsCallable.class.getSimpleName();

    public WriteParamsCallable(CameraListener cameraListener) {
        super(cameraListener);
    }

    @SuppressWarnings("deprecation")
    @Override
    public CallableReturn call() {
        Camera camera = getCameraData().mCamera;
        if (camera == null) {
            return new CallableReturn(new Exception("Camera isn't opened"));
        }
        try {
            camera.setParameters(getCameraParameters());
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
