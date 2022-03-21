package ax.nd.faceunlock.camera;

import android.hardware.Camera;
import android.os.HandlerThread;

public class CameraHandlerThread extends HandlerThread {
    public static final String TAG = CameraHandlerThread.class.getSimpleName();
    private final CameraData mCameraData = new CameraData();

    public CameraHandlerThread() {
        super(TAG, -2);
    }

    public CameraData getCameraData() {
        return this.mCameraData;
    }

    @SuppressWarnings("deprecation")
    public static final class CameraData {
        public Camera mCamera;
        public int mCameraId;
        public Camera.Parameters mParameters;

        private CameraData() {
            this.mCamera = null;
            this.mCameraId = -1;
        }
    }
}
