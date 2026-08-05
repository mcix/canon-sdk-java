/*
 * MIT License
 *
 * Copyright (c) 2018-2020 Yoann CAPLAIN
 */
package org.blackdread.cameraframework.demo;

import com.sun.jna.Platform;
import org.blackdread.camerabinding.jna.EdsPoint;
import org.blackdread.camerabinding.jna.EdsSize;
import org.blackdread.camerabinding.jna.EdsdkLibrary;
import org.blackdread.camerabinding.jna.EdsdkLibrary.EdsCameraRef;
import org.blackdread.cameraframework.api.TestShortcutUtil;
import org.blackdread.cameraframework.api.constant.EdsAFMode;
import org.blackdread.cameraframework.api.constant.EdsCameraCommand;
import org.blackdread.cameraframework.api.constant.EdsEvfAFMode;
import org.blackdread.cameraframework.api.constant.EdsEvfAf;
import org.blackdread.cameraframework.api.constant.EdsEvfOutputDevice;
import org.blackdread.cameraframework.api.constant.EdsPropertyID;
import org.blackdread.cameraframework.api.constant.EdsdkError;
import org.blackdread.cameraframework.api.helper.factory.CanonFactory;
import org.blackdread.cameraframework.api.helper.logic.LiveViewReference;
import org.blackdread.cameraframework.exception.error.EdsdkErrorException;
import org.blackdread.cameraframework.util.ReleaseUtil;

import java.awt.Toolkit;
import java.util.List;

import static org.blackdread.cameraframework.util.ErrorUtil.toEdsdkError;

/**
 * Probes a connected Canon body to report what autofocus capabilities are
 * actually exposed: AF mode, available EVF AF modes, tracking object,
 * continuous AF mode, AF lock state, EVF coordinate system, and whether
 * EdsSetFramePoint dispatches successfully at center / corners during a
 * live-view session. Useful when wiring focus-by-point UI on a new body.
 */
public final class AutoFocusProbe {

    public static void main(final String[] args) throws Exception {
        if (Platform.isMac()) {
            Toolkit.getDefaultToolkit();
        }

        TestShortcutUtil.initLibrary();
        final EdsdkLibrary.EdsCameraRef.ByReference cameraByRef = TestShortcutUtil.getFirstCamera();
        TestShortcutUtil.openSession(cameraByRef);
        final EdsCameraRef camera = cameraByRef.getValue();
        System.out.println("=== AutoFocusProbe: opened session on first camera ===\n");

        try {
            reportProperty("AFMode (lens AF mode)",
                EdsPropertyID.kEdsPropID_AFMode, camera, v -> EdsAFMode.ofValue(v.intValue()).description());
            reportProperty("Evf_AFMode (live-view AF mode)",
                EdsPropertyID.kEdsPropID_Evf_AFMode, camera, v -> EdsEvfAFMode.ofValue(v.intValue()).description());
            reportEvfAFModeDescriptor(camera);
            reportProperty("AFTrackingObject",
                EdsPropertyID.kEdsPropID_AFTrackingObject, camera, v -> "value=" + v);
            reportProperty("ContinuousAfMode",
                EdsPropertyID.kEdsPropID_ContinuousAfMode, camera, v -> "value=" + v);
            reportProperty("AfLockState",
                EdsPropertyID.kEdsPropID_AfLockState, camera, v -> "value=" + v);
            reportProperty("FocusPosition",
                EdsPropertyID.kEdsPropID_FocusPosition, camera, v -> "value=" + v);

            System.out.println();
            System.out.println("=== Starting live view (PC) for AF-point tests ===");
            CanonFactory.liveViewLogic().beginLiveView(camera, EdsEvfOutputDevice.kEdsEvfOutputDevice_PC);
            Thread.sleep(2000);

            // Coordinate system is on the EVF image, not the camera ref.
            try (LiveViewReference lv = CanonFactory.liveViewLogic().getLiveViewImageReference(camera)) {
                final EdsSize coord = CanonFactory.propertyGetShortcutLogic().getEvfCoordinateSystem(lv.getImageRef().getValue());
                System.out.printf("Evf coordinate system: width=%d height=%d%n", coord.width, coord.height);

                tryFramePoint("center",       coord.width / 2,                coord.height / 2,                false, camera);
                tryFramePoint("top-right",    (int)(coord.width * 0.85),      (int)(coord.height * 0.15),      false, camera);
                tryFramePoint("top-left",     (int)(coord.width * 0.15),      (int)(coord.height * 0.15),      false, camera);
                tryFramePoint("bottom-right", (int)(coord.width * 0.85),      (int)(coord.height * 0.85),      false, camera);
                tryFramePoint("center-locked", coord.width / 2,                coord.height / 2,                true, camera);

                System.out.println();
                System.out.println("=== Triggering AF via DoEvfAf(ON) ===");
                final EdsdkError af = toEdsdkError(CanonFactory.edsdkLibrary().EdsSendCommand(camera,
                    EdsCameraCommand.kEdsCameraCommand_DoEvfAf.value(),
                    EdsEvfAf.kEdsCameraCommand_EvfAf_ON.value()));
                System.out.println("DoEvfAf(ON)  -> " + af);
                Thread.sleep(800);
                final EdsdkError off = toEdsdkError(CanonFactory.edsdkLibrary().EdsSendCommand(camera,
                    EdsCameraCommand.kEdsCameraCommand_DoEvfAf.value(),
                    EdsEvfAf.kEdsCameraCommand_EvfAf_OFF.value()));
                System.out.println("DoEvfAf(OFF) -> " + off);
            } finally {
                CanonFactory.liveViewLogic().endLiveView(camera);
            }
        } finally {
            TestShortcutUtil.closeSession(cameraByRef);
            ReleaseUtil.release(cameraByRef);
            TestShortcutUtil.terminateLibrary();
        }
    }

    private interface Decoder { String describe(Long value); }

    private static void reportProperty(final String label, final EdsPropertyID id,
                                       final EdsCameraRef camera, final Decoder decoder) {
        try {
            final Long raw = CanonFactory.propertyGetLogic().getPropertyData(camera, id);
            System.out.printf("%-35s -> %s (raw=%d)%n", label, decoder.describe(raw), raw);
        } catch (final EdsdkErrorException e) {
            System.out.printf("%-35s -> %s%n", label, e.getEdsdkError());
        } catch (final Exception other) {
            System.out.printf("%-35s -> %s: %s%n", label, other.getClass().getSimpleName(), other.getMessage());
        }
    }

    private static void reportEvfAFModeDescriptor(final EdsCameraRef camera) {
        try {
            final List<EdsEvfAFMode> available = CanonFactory.propertyDescLogic().getPropertyDesc(
                camera, EdsPropertyID.kEdsPropID_Evf_AFMode);
            if (available.isEmpty()) {
                System.out.printf("%-35s -> (no settable values reported)%n", "Available Evf_AFMode values");
            } else {
                System.out.printf("%-35s -> %d entries%n", "Available Evf_AFMode values", available.size());
                for (final EdsEvfAFMode m : available) {
                    System.out.printf("    %s (%d) — %s%n", m.name(), m.value(), m.description());
                }
            }
        } catch (final Exception e) {
            System.out.printf("%-35s -> %s%n", "Available Evf_AFMode values", e.getMessage());
        }
    }

    private static void tryFramePoint(final String label, final int x, final int y,
                                      final boolean lock, final EdsCameraRef camera) {
        final EdsPoint.ByValue p = new EdsPoint.ByValue();
        p.x = x;
        p.y = y;
        try {
            final EdsdkError err = toEdsdkError(CanonFactory.edsdkLibrary().EdsSetFramePoint(camera, p, lock));
            System.out.printf("EdsSetFramePoint %-15s @ (%4d,%4d) lock=%-5s -> %s%n",
                label, x, y, lock, err);
        } catch (final UnsatisfiedLinkError tooOld) {
            System.out.printf("EdsSetFramePoint %-15s — symbol not exported by installed EDSDK (need 13.13.20+)%n", label);
        } catch (final Exception other) {
            System.out.printf("EdsSetFramePoint %-15s @ (%4d,%4d) -> %s: %s%n",
                label, x, y, other.getClass().getSimpleName(), other.getMessage());
        }
    }

    private AutoFocusProbe() {}
}
