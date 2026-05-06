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
 * support. Mirrorless R-series cameras have {@code kEdsPropID_Evf_Mode} present in
 * the property table but do not actually drive live view through it; the value
 * never flips and writes are silently ignored. Use {@link #hasEvfMode(EdsCameraRef)}
 * with {@code Assumptions.assumeTrue(...)} to gate tests that exercise
 * enableLiveView / disableLiveView / state assertions on Evf_Mode.
 */
public final class CameraTypeUtil {

    private CameraTypeUtil() {}

    /**
     * Probes whether {@code kEdsPropID_Evf_Mode} is functionally writable on this
     * body — not merely readable. Both DSLR and mirrorless bodies usually let you
     * read the property (mirrorless reports a stale 0), but only on a DSLR does
     * writing 1 and reading back yield 1. Using a write-probe avoids the
     * "readable but inert" trap that bit
     * LiveViewLogicCameraTest.endLiveViewFailsIfNotEnabledFirst on the R8.
     *
     * Restores whatever value the camera reported before probing.
     *
     * @return true if writes to Evf_Mode actually take effect (DSLR), false if the
     *         property is missing, write-rejected, or write-silently-ignored
     *         (mirrorless / R-series).
     */
    public static boolean hasEvfMode(final EdsCameraRef camera) {
        final Long original;
        try {
            original = CanonFactory.propertyGetLogic().getPropertyData(camera, EdsPropertyID.kEdsPropID_Evf_Mode);
        } catch (final EdsdkErrorException e) {
            return false;
        } catch (final IllegalArgumentException unsupportedDataType) {
            return false;
        }

        // Pick a target value the camera is not already at, so a successful write
        // produces an observable readback change.
        final long probe = (original != null && original == 1L) ? 0L : 1L;
        try {
            CanonFactory.propertySetLogic().setPropertyData(camera, EdsPropertyID.kEdsPropID_Evf_Mode, probe);
        } catch (final EdsdkErrorException writeFailed) {
            return false;
        } catch (final IllegalArgumentException unsupportedSet) {
            return false;
        }

        final boolean writeStuck;
        try {
            final Long after = CanonFactory.propertyGetLogic().getPropertyData(camera, EdsPropertyID.kEdsPropID_Evf_Mode);
            writeStuck = after != null && after == probe;
        } catch (final EdsdkErrorException ignored) {
            return false;
        }

        // Best-effort restore so the probe doesn't leave the camera in live view.
        if (original != null) {
            try {
                CanonFactory.propertySetLogic().setPropertyData(camera, EdsPropertyID.kEdsPropID_Evf_Mode, (long) original);
            } catch (final EdsdkErrorException ignored) {
                // Ignore — original value couldn't be restored, but probe still answered.
            } catch (final IllegalArgumentException ignored) {
                // Same
            }
        }
        return writeStuck;
    }
}
