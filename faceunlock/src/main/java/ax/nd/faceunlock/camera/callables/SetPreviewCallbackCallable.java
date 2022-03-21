package ax.nd.faceunlock.camera.callables;

import android.hardware.Camera;

import ax.nd.faceunlock.camera.listeners.ByteBufferCallbackListener;
import ax.nd.faceunlock.camera.listeners.CameraListener;

import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;

public class SetPreviewCallbackCallable extends CameraCallable {
    private static final String TAG = SetPreviewCallbackCallable.class.getSimpleName();
    private final WeakReference<ByteBufferCallbackListener> mPreviewCallbackListener;
    @SuppressWarnings("deprecation")
    private final Camera.PreviewCallback mPreviewCallback = (bArr, camera) -> {
        ByteBufferCallbackListener byteBufferCallbackListener = SetPreviewCallbackCallable.this.mPreviewCallbackListener.get();
        if (byteBufferCallbackListener != null) {
            byteBufferCallbackListener.onEventCallback(0, ByteBuffer.wrap(bArr));
        }
    };
    private final boolean mWithBuffer;

    public SetPreviewCallbackCallable(ByteBufferCallbackListener byteBufferCallbackListener, boolean z, CameraListener cameraListener) {
        super(cameraListener);
        mWithBuffer = z;
        mPreviewCallbackListener = new WeakReference<>(byteBufferCallbackListener);
    }

    @SuppressWarnings("deprecation")
    @Override
    public CallableReturn call() {
        Camera camera = getCameraData().mCamera;
        if (camera == null) {
            return new CallableReturn(new Exception("Camera isn't opened"));
        }
        if (mWithBuffer) {
            camera.setPreviewCallbackWithBuffer(mPreviewCallback);
        } else {
            camera.setPreviewCallback(mPreviewCallback);
        }
        return new CallableReturn(null);
    }

    @Override
    public String getTag() {
        return TAG;
    }
}
