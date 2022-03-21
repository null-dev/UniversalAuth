/**
 * Copyright (C) 2021 The PixelExperience Project
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

package com.android.internal.util.custom.faceunlock;

import com.android.internal.util.custom.faceunlock.IFaceServiceReceiver;

/** @hide */
interface IFaceService {
    oneway void authenticate(long operationId);

    oneway void cancel();

    oneway void enroll(in byte[] cryptoToken, int timeout, in int[] disabledFeatures);

    int enumerate();

    long generateChallenge(int timeout);

    int getAuthenticatorId();

    boolean getFeature(int feature, int faceId);

    int getFeatureCount();

    oneway void remove(int biometricId);

    oneway void resetLockout(in byte[] cryptoToken);

    int revokeChallenge();

    oneway void setCallback(in IFaceServiceReceiver receiver);

    oneway void setFeature(int feature, boolean enable, in byte[] token, int faceId);
}