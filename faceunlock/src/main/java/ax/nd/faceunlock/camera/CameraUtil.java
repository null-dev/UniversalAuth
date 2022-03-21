package ax.nd.faceunlock.camera;

import android.content.Context;
import android.hardware.Camera;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;

import ax.nd.faceunlock.R;

import java.util.ArrayList;
import java.util.List;

public class CameraUtil {
    @SuppressWarnings("deprecation")
    public static Camera.Size calBestPreviewSize(Camera.Parameters parameters, final int i, final int i2) {
        List<Camera.Size> supportedPreviewSizes = parameters.getSupportedPreviewSizes();
        ArrayList<Camera.Size> arrayList = new ArrayList<>();
        for (Camera.Size size : supportedPreviewSizes) {
            if (size.width > size.height) {
                arrayList.add(size);
            }
        }
        arrayList.sort((size, size2) -> Math.abs((size.width * size.height) - (i * i2)) - Math.abs((size2.width * size2.height) - (i * i2)));
        return arrayList.get(0);
    }

    public static int getFrontFacingCameraId(Context context) {
        int overrideCamId = context.getResources().getInteger(R.integer.override_front_cam_id);
        if (overrideCamId != -1){
            return overrideCamId;
        }
        try {
            CameraManager cameraManager = context.getSystemService(CameraManager.class);
            String cameraId;
            int cameraOrientation;
            CameraCharacteristics characteristics;
            for (int i = 0; i < cameraManager.getCameraIdList().length; i++) {
                cameraId = cameraManager.getCameraIdList()[i];
                characteristics = cameraManager.getCameraCharacteristics(cameraId);
                cameraOrientation = characteristics.get(CameraCharacteristics.LENS_FACING);
                if (cameraOrientation == CameraCharacteristics.LENS_FACING_FRONT) {
                    return Integer.parseInt(cameraId);
                }

            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        return -1;
    }
}
