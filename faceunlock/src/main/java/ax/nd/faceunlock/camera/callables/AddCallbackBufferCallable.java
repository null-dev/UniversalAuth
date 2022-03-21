package ax.nd.faceunlock.camera.callables;

import android.hardware.Camera;

import ax.nd.faceunlock.camera.listeners.CameraListener;

public class AddCallbackBufferCallable extends CameraCallable {
    private static final String TAG = AddCallbackBufferCallable.class.getSimpleName();
    private final byte[] mBuffer;

    public AddCallbackBufferCallable(byte[] bArr, CameraListener cameraListener) {
        super(cameraListener);
        mBuffer = bArr;
    }

    @SuppressWarnings("deprecation")
    @Override
    public CallableReturn call() {
        Camera camera = getCameraData().mCamera;
        if (camera == null) {
            return new CallableReturn(new Exception("Camera isn't opened"));
        }
        camera.addCallbackBuffer(mBuffer);
        return new CallableReturn(null);
    }

    @Override
    public String getTag() {
        return TAG;
    }
}
