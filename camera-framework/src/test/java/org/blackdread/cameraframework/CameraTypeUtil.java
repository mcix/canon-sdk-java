/*
 * MIT License
 *
 * Copyright (c) 2018-2020 Yoann CAPLAIN
 */
package org.blackdread.cameraframework;

import org.blackdread.camerabinding.jna.EdsdkLibrary.EdsCameraRef;
import org.blackdread.cameraframework.api.constant.EdsPropertyID;
import org.blackdread.cameraframework.api.helper.factory.CanonFactory;
import org.blackdread.cameraframework.exception.error.EdsdkErrorException;

/**
 * Detects camera capabilities so tests can skip features the connected body does not
 * support. Mirrorless R-series cameras lack {@code kEdsPropID_Evf_Mode}; SDK calls
 * touching it return {@code EDS_ERR_INVALID_HANDLE}. Use {@link #hasEvfMode(EdsCameraRef)}
 * with {@code Assumptions.assumeTrue(...)} to gate tests that exercise enableLiveView
 * / disableLiveView / isLiveViewEnabled.
 */
public final class CameraTypeUtil {

    private CameraTypeUtil() {}

    /**
     * @return true if the camera exposes {@code kEdsPropID_Evf_Mode} (DSLR bodies).
     *         Mirrorless bodies (R-series and EOS M ones whose SDK reports no Evf_Mode)
     *         return false here, and DSLR-specific live-view tests should be skipped.
     */
    public static boolean hasEvfMode(final EdsCameraRef camera) {
        try {
            CanonFactory.propertyGetLogic().getPropertyData(camera, EdsPropertyID.kEdsPropID_Evf_Mode);
            return true;
        } catch (final EdsdkErrorException e) {
            return false;
        } catch (final IllegalArgumentException unsupportedDataType) {
            // PropertyGetLogic also throws IllegalArgumentException for unknown data types.
            return false;
        }
    }
}
