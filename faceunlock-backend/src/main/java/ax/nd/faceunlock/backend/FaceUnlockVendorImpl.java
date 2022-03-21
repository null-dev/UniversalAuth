package ax.nd.faceunlock.backend;

import com.megvii.facepp.sdk.Lite;

public class FaceUnlockVendorImpl extends Lite {
    private static FaceUnlockVendorImpl sInstance;

    private FaceUnlockVendorImpl() {
    }
    
    public static FaceUnlockVendorImpl getInstance() {
        if (sInstance == null) {
            sInstance = new FaceUnlockVendorImpl();
        }
        return sInstance;
    }
}
