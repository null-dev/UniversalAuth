package ax.nd.faceunlock.camera.callables;

import android.hardware.Camera;

import ax.nd.faceunlock.camera.listeners.CameraListener;

public class SetFaceDetectionCallback extends CameraCallable {
    private static final String TAG = SetFaceDetectionCallback.class.getSimpleName();
    @SuppressWarnings("deprecation")
    Camera.FaceDetectionListener mListener;

    @SuppressWarnings("deprecation")
    public SetFaceDetectionCallback(Camera.FaceDetectionListener faceDetectionListener, CameraListener cameraListener) {
        super(cameraListener);
        mListener = faceDetectionListener;
    }

    @SuppressWarnings("deprecation")
    @Override
    public CallableReturn call() {
        Camera camera = getCameraData().mCamera;
        if (camera == null) {
            return new CallableReturn(new Exception("Camera isn't opened"));
        }
        camera.setFaceDetectionListener(mListener);
        if (mListener != null) {
            camera.startFaceDetection();
        } else {
            camera.stopFaceDetection();
        }
        return new CallableReturn(null);
    }

    @Override
    public String getTag() {
        return TAG;
    }
}
