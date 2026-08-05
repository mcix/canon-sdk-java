/*
 * MIT License
 *
 * Copyright (c) 2018-2020 Yoann CAPLAIN
 */
package org.blackdread.cameraframework.demo;

import com.sun.jna.Platform;
import org.blackdread.camerabinding.jna.EdsFocusInfo;
import org.blackdread.camerabinding.jna.EdsRect;
import org.blackdread.camerabinding.jna.EdsSize;
import org.blackdread.camerabinding.jna.EdsdkLibrary;
import org.blackdread.camerabinding.jna.EdsdkLibrary.EdsCameraRef;
import org.blackdread.camerabinding.jna.EdsdkLibrary.EdsEvfImageRef;
import org.blackdread.cameraframework.api.TestShortcutUtil;
import org.blackdread.cameraframework.api.constant.EdsEvfOutputDevice;
import org.blackdread.cameraframework.api.constant.EdsPropertyID;
import org.blackdread.cameraframework.api.helper.factory.CanonFactory;
import org.blackdread.cameraframework.api.helper.logic.LiveViewReference;
import org.blackdread.cameraframework.exception.error.EdsdkErrorException;
import org.blackdread.cameraframework.util.ReleaseUtil;

import java.awt.Toolkit;

/**
 * Reports which focus-related properties the connected body exposes,
 * and on which ref each one is readable (camera ref vs. EVF image ref
 * during live view).
 */
public final class FocusInfoProbe {

    public static void main(final String[] args) throws Exception {
        if (Platform.isMac()) Toolkit.getDefaultToolkit();

        TestShortcutUtil.initLibrary();
        final EdsdkLibrary.EdsCameraRef.ByReference cameraByRef = TestShortcutUtil.getFirstCamera();
        TestShortcutUtil.openSession(cameraByRef);
        final EdsCameraRef camera = cameraByRef.getValue();
        System.out.println("=== FocusInfoProbe ===\n");

        try {
            // 1. Properties readable directly off the camera ref (no live view).
            System.out.println("--- Off EdsCameraRef (no live view) ---");
            probe(camera, EdsPropertyID.kEdsPropID_FocusInfo,    "FocusInfo (focus-point array)");
            probe(camera, EdsPropertyID.kEdsPropID_FocusPosition,"FocusPosition (lens position)");
            probe(camera, EdsPropertyID.kEdsPropID_AfLockState,  "AfLockState");
            probe(camera, EdsPropertyID.kEdsPropID_AFMode,       "AFMode");
            probe(camera, EdsPropertyID.kEdsPropID_Evf_AFMode,   "Evf_AFMode (EVF AF mode)");

            // 2. Start live view + properties readable off the EvfImageRef.
            System.out.println("\n--- Starting live view ---");
            CanonFactory.liveViewLogic().beginLiveView(camera, EdsEvfOutputDevice.kEdsEvfOutputDevice_PC);
            Thread.sleep(2000);

            try (LiveViewReference lv = CanonFactory.liveViewLogic().getLiveViewImageReference(camera)) {
                final EdsEvfImageRef evf = lv.getImageRef().getValue();
                System.out.println("\n--- Off EdsEvfImageRef (after EdsDownloadEvfImage) ---");
                probe(evf, EdsPropertyID.kEdsPropID_Evf_CoordinateSystem,    "Evf_CoordinateSystem (canvas size for AF coords)");
                probe(evf, EdsPropertyID.kEdsPropID_Evf_VisibleRect,         "Evf_VisibleRect (visible region of EVF)");
                probe(evf, EdsPropertyID.kEdsPropID_Evf_ZoomRect,            "Evf_ZoomRect (AF frame rectangle)");
                probe(evf, EdsPropertyID.kEdsPropID_Evf_ImageClipRect,       "Evf_ImageClipRect (cropped region)");
                probe(evf, EdsPropertyID.kEdsPropID_Evf_AFMode,              "Evf_AFMode");
                probe(evf, EdsPropertyID.kEdsPropID_Evf_FocusAid,            "Evf_FocusAid (focus assist hint)");
                probe(evf, EdsPropertyID.kEdsPropID_FocusInfo,               "FocusInfo (focus-point array, off EVF ref)");
                probe(evf, EdsPropertyID.kEdsPropID_Evf_DepthOfFieldPreview, "Evf_DepthOfFieldPreview");

                // Decode any EdsRect we got from the EVF
                System.out.println("\n--- Decoded values (where read OK) ---");
                tryDecodeSize(evf,  EdsPropertyID.kEdsPropID_Evf_CoordinateSystem, "coord system");
                tryDecodeRect(evf,  EdsPropertyID.kEdsPropID_Evf_VisibleRect,      "visible rect");
                tryDecodeRect(evf,  EdsPropertyID.kEdsPropID_Evf_ZoomRect,         "AF frame (ZoomRect)");
                tryDecodeRect(evf,  EdsPropertyID.kEdsPropID_Evf_ImageClipRect,    "image clip rect");
                tryDecodeFocusInfo(evf);
            }
            CanonFactory.liveViewLogic().endLiveView(camera);
        } finally {
            TestShortcutUtil.closeSession(cameraByRef);
            ReleaseUtil.release(cameraByRef);
            TestShortcutUtil.terminateLibrary();
        }
    }

    private static void probe(final EdsdkLibrary.EdsBaseRef ref, final EdsPropertyID id, final String label) {
        try {
            final org.blackdread.cameraframework.api.helper.logic.PropertyInfo info =
                CanonFactory.propertyLogic().getPropertyTypeAndSize(ref, id);
            System.out.printf("%-50s -> type=%s size=%d%n", label, info.getDataType(), info.getSize());
        } catch (final EdsdkErrorException e) {
            System.out.printf("%-50s -> %s%n", label, e.getEdsdkError());
        } catch (final Exception other) {
            System.out.printf("%-50s -> %s%n", label, other.getMessage());
        }
    }

    private static void tryDecodeSize(final EdsEvfImageRef evf, final EdsPropertyID id, final String label) {
        try {
            final EdsSize s;
            if (id == EdsPropertyID.kEdsPropID_Evf_CoordinateSystem) {
                s = CanonFactory.propertyGetShortcutLogic().getEvfCoordinateSystem(evf);
            } else {
                s = CanonFactory.propertyGetLogic().getPropertyData(evf, id);
            }
            System.out.printf("  %-25s width=%d height=%d%n", label, s.width, s.height);
        } catch (final Exception e) { /* skip */ }
    }

    private static void tryDecodeRect(final EdsEvfImageRef evf, final EdsPropertyID id, final String label) {
        try {
            final EdsRect r = CanonFactory.propertyGetLogic().getPropertyData(evf, id);
            System.out.printf("  %-25s x=%d y=%d w=%d h=%d%n",
                label, r.point.x, r.point.y, r.size.width, r.size.height);
        } catch (final Exception e) { /* skip */ }
    }

    private static void tryDecodeFocusInfo(final EdsEvfImageRef evf) {
        try {
            final EdsFocusInfo info = CanonFactory.propertyGetLogic().getPropertyData(
                evf, EdsPropertyID.kEdsPropID_FocusInfo);
            System.out.printf("  focus info               imageRect=(x=%d,y=%d,w=%d,h=%d) pointCount=%d executeMode=%d%n",
                info.imageRect.point.x, info.imageRect.point.y,
                info.imageRect.size.width, info.imageRect.size.height,
                info.pointNumber, info.executeMode);
            // Print the first few selected/active focus points
            int active = 0;
            for (int i = 0; i < info.pointNumber && active < 5; i++) {
                if (info.focusPoint[i] == null) continue;
                System.out.printf("    point %d  rect=(x=%d,y=%d,w=%d,h=%d)%n", i,
                    info.focusPoint[i].rect.point.x, info.focusPoint[i].rect.point.y,
                    info.focusPoint[i].rect.size.width, info.focusPoint[i].rect.size.height);
                active++;
            }
        } catch (final Exception e) {
            System.out.println("  focus info               (not available: " + e.getMessage() + ")");
        }
    }

    private FocusInfoProbe() {}
}
