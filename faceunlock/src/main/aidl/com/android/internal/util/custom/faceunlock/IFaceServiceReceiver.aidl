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

/** @hide */
oneway interface IFaceServiceReceiver {
    void onEnrollResult(int faceId, int userId, int remaining);

    void onAuthenticated(int faceId, int userId, in byte[] token);

    void onError(int error, int vendorCode);

    void onRemoved(in int[] faceIds, int userId);

    void onEnumerate(in int[] faceIds, int userId);
}
