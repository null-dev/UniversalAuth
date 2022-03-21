/*
 * Copyright (C) 2018 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ax.nd.faceunlock.stub.face;

import static ax.nd.faceunlock.stub.biometrics.BiometricFaceConstants.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED;
import static ax.nd.faceunlock.stub.biometrics.BiometricFaceConstants.FACE_ERROR_CANCELED;
import static ax.nd.faceunlock.stub.biometrics.BiometricFaceConstants.FACE_ERROR_HW_NOT_PRESENT;
import static ax.nd.faceunlock.stub.biometrics.BiometricFaceConstants.FACE_ERROR_HW_UNAVAILABLE;
import static ax.nd.faceunlock.stub.biometrics.BiometricFaceConstants.FACE_ERROR_LOCKOUT;
import static ax.nd.faceunlock.stub.biometrics.BiometricFaceConstants.FACE_ERROR_LOCKOUT_PERMANENT;
import static ax.nd.faceunlock.stub.biometrics.BiometricFaceConstants.FACE_ERROR_NOT_ENROLLED;
import static ax.nd.faceunlock.stub.biometrics.BiometricFaceConstants.FACE_ERROR_NO_SPACE;
import static ax.nd.faceunlock.stub.biometrics.BiometricFaceConstants.FACE_ERROR_TIMEOUT;
import static ax.nd.faceunlock.stub.biometrics.BiometricFaceConstants.FACE_ERROR_UNABLE_TO_PROCESS;
import static ax.nd.faceunlock.stub.biometrics.BiometricFaceConstants.FACE_ERROR_USER_CANCELED;
import static ax.nd.faceunlock.stub.biometrics.BiometricFaceConstants.FACE_ERROR_VENDOR;

import android.content.Context;
import android.os.Binder;
import android.os.CancellationSignal;
import android.os.IBinder;
import android.util.Log;
import android.view.Surface;

import ax.nd.faceunlock.framework_stub.R;
import ax.nd.faceunlock.stub.biometrics.BiometricConstants;
import ax.nd.faceunlock.stub.biometrics.BiometricFaceConstants;


public class FaceManager {
    private static final String TAG = "FaceManager";
    private final Context mContext;
    private IBinder mToken = new Binder();
    private EnrollmentCallback mEnrollmentCallback;

    public FaceManager(Context context) {
        mContext = context;
    }

    /**
     * Determine if there is a face enrolled.
     *
     * @return true if a face is enrolled, false otherwise
     * @hide
     */
    public boolean hasEnrolledTemplates() {
        return false;
    }

    /**
     * Defaults to {@link FaceManager#enroll(int, byte[], CancellationSignal, EnrollmentCallback,
     * int[], Surface)} with {@code previewSurface} set to null.
     *
     * @see FaceManager#enroll(int, byte[], CancellationSignal, EnrollmentCallback, int[], Surface)
     * @hide
     */
    public void enroll(int userId, byte[] hardwareAuthToken, CancellationSignal cancel,
                       EnrollmentCallback callback, int[] disabledFeatures) {
        // enroll(hardwareAuthToken, 75 sec, new int[]{1})
        enroll(userId, hardwareAuthToken, cancel, callback, disabledFeatures,
                null /* previewSurface */, false /* debugConsent */);
    }

    /**
     * Request face authentication enrollment. This call operates the face authentication hardware
     * and starts capturing images. Progress will be indicated by callbacks to the
     * {@link EnrollmentCallback} object. It terminates when
     * {@link EnrollmentCallback#onEnrollmentError(int, CharSequence)} or
     * {@link EnrollmentCallback#onEnrollmentProgress(int) is called with remaining == 0, at
     * which point the object is no longer valid. The operation can be canceled by using the
     * provided cancel object.
     *
     * @param hardwareAuthToken a unique token provided by a recent creation or
     *                          verification of device credentials (e.g. pin, pattern or password).
     * @param cancel            an object that can be used to cancel enrollment
     * @param userId            the user to whom this face will belong to
     * @param callback          an object to receive enrollment events
     * @param previewSurface    optional camera preview surface for a single-camera device.
     *                          Must be null if not used.
     * @param debugConsent      a feature flag that the user has consented to debug.
     * @hide
     */
    public void enroll(int userId, byte[] hardwareAuthToken, CancellationSignal cancel,
                       EnrollmentCallback callback, int[] disabledFeatures, Surface previewSurface,
                       boolean debugConsent) {
    }

    // Based off of sendErrorResult()
    public static int calcClientMsgId(int errMsgId, int vendorCode) {
        return errMsgId == FACE_ERROR_VENDOR
                ? (vendorCode + BiometricFaceConstants.FACE_ERROR_VENDOR_BASE) : errMsgId;
    }

    public static String getErrorString(Context context, int errMsg, int vendorCode) {
        switch (errMsg) {
            case FACE_ERROR_HW_UNAVAILABLE:
                return context.getString(
                        R.string.face_error_hw_not_available);
            case FACE_ERROR_UNABLE_TO_PROCESS:
                return context.getString(
                        R.string.face_error_unable_to_process);
            case FACE_ERROR_TIMEOUT:
                return context.getString(R.string.face_error_timeout);
            case FACE_ERROR_NO_SPACE:
                return context.getString(R.string.face_error_no_space);
            case FACE_ERROR_CANCELED:
                return context.getString(R.string.face_error_canceled);
            case FACE_ERROR_LOCKOUT:
                return context.getString(R.string.face_error_lockout);
            case FACE_ERROR_LOCKOUT_PERMANENT:
                return context.getString(
                        R.string.face_error_lockout_permanent);
            case FACE_ERROR_USER_CANCELED:
                return context.getString(R.string.face_error_user_canceled);
            case FACE_ERROR_NOT_ENROLLED:
                return context.getString(R.string.face_error_not_enrolled);
            case FACE_ERROR_HW_NOT_PRESENT:
                return context.getString(R.string.face_error_hw_not_present);
            case BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED:
                return context.getString(
                        R.string.face_error_security_update_required);
            case FACE_ERROR_VENDOR: {
                String[] msgArray = context.getResources().getStringArray(
                        R.array.face_error_vendor);
                if (vendorCode < msgArray.length) {
                    return msgArray[vendorCode];
                }
            }
        }
        Log.w(TAG, "Invalid error message: " + errMsg + ", " + vendorCode);
        return "";
    }


    /**
     * Callback structure provided to {@link FaceManager#enroll(long,
     * EnrollmentCallback, CancellationSignal, int). Users of {@link #FaceAuthenticationManager()}
     * must provide an implementation of this to {@link FaceManager#enroll(long,
     * CancellationSignal, int, EnrollmentCallback) for listening to face enrollment events.
     *
     * @hide
     */
    public abstract static class EnrollmentCallback {
        /**
         * Called when an unrecoverable error has been encountered and the operation is complete.
         * No further callbacks will be made on this object.
         *
         * @param errMsgId  An integer identifying the error message
         * @param errString A human-readable error string that can be shown in UI
         */
        public void onEnrollmentError(int errMsgId, CharSequence errString) {
        }
        /**
         * Called when a recoverable error has been encountered during enrollment. The help
         * string is provided to give the user guidance for what went wrong, such as
         * "Image too dark, uncover light source" or what they need to do next, such as
         * "Rotate face up / down."
         *
         * @param helpMsgId  An integer identifying the error message
         * @param helpString A human-readable string that can be shown in UI
         */
        public void onEnrollmentHelp(int helpMsgId, CharSequence helpString) {
        }
        /**
         * Called each time a single frame is captured during enrollment.
         *
         * <p>For older, non-AIDL implementations, only {@code helpCode} and {@code helpMessage} are
         * supported. Sensible default values will be provided for all other arguments.
         *
         * @param helpCode    An integer identifying the capture status for this frame.
         * @param helpMessage A human-readable help string that can be shown in UI.
         * @param cell        The cell captured during this frame of enrollment, if any.
         * @param stage       An integer representing the current stage of enrollment.
         * @param pan         The horizontal pan of the detected face. Values in the range [-1, 1]
         *                    indicate a good capture.
         * @param tilt        The vertical tilt of the detected face. Values in the range [-1, 1]
         *                    indicate a good capture.
         * @param distance    The distance of the detected face from the device. Values in
         *                    the range [-1, 1] indicate a good capture.
         */
        public void onEnrollmentFrame(
                int helpCode,
                CharSequence helpMessage,
                FaceEnrollCell cell,
                @FaceEnrollStages.FaceEnrollStage int stage,
                float pan,
                float tilt,
                float distance) {
            onEnrollmentHelp(helpCode, helpMessage);
        }
        /**
         * Called as each enrollment step progresses. Enrollment is considered complete when
         * remaining reaches 0. This function will not be called if enrollment fails. See
         * {@link EnrollmentCallback#onEnrollmentError(int, CharSequence)}
         *
         * @param remaining The number of remaining steps
         */
        public void onEnrollmentProgress(int remaining) {
        }
    }
}
