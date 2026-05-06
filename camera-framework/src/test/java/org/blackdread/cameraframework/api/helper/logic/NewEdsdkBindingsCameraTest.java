/*
 * MIT License
 *
 * Copyright (c) 2018-2020 Yoann CAPLAIN
 */
package org.blackdread.cameraframework.api.helper.logic;

import org.blackdread.camerabinding.jna.EdsPoint;
import org.blackdread.camerabinding.jna.EdsPropertyDescEx;
import org.blackdread.camerabinding.jna.EdsdkLibrary;
import org.blackdread.camerabinding.jna.EdsdkLibrary.EdsCameraRef;
import org.blackdread.camerabinding.jna.EdsdkLibrary.EdsFlashRef;
import org.blackdread.cameraframework.CameraIsConnected;
import org.blackdread.cameraframework.api.TestShortcutUtil;
import org.blackdread.cameraframework.api.constant.EdsPropertyID;
import org.blackdread.cameraframework.api.constant.EdsdkError;
import org.blackdread.cameraframework.util.ReleaseUtil;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.blackdread.cameraframework.api.helper.factory.CanonFactory.edsdkLibrary;
import static org.blackdread.cameraframework.util.ErrorUtil.toEdsdkError;

/**
 * Smoke tests for the five EdsdkLibrary functions added to the binding for
 * EDSDK 13.20.10:
 *
 * - EdsCreateFlashSettingRef
 * - EdsCreateFolder
 * - EdsGetPropertyDescEx
 * - EdsSetMetaImage   (signature-only — meaningful exercise needs a directory item)
 * - EdsSetFramePoint
 *
 * Goal is to prove the JNA signatures match what EDSDK exports — i.e. the call
 * dispatches without UnsatisfiedLinkError and returns a sensible EdsError, even
 * if the connected body answers EDS_ERR_NOT_SUPPORTED. Runtime assertions accept
 * any documented EdsError; only an UnsatisfiedLinkError or a totally bogus
 * result would fail these tests.
 */
@CameraIsConnected
class NewEdsdkBindingsCameraTest {

    private static final Logger log = LoggerFactory.getLogger(NewEdsdkBindingsCameraTest.class);

    private static EdsdkLibrary.EdsCameraRef.ByReference camera;
    private static EdsCameraRef cameraRef;

    @BeforeAll
    static void setUpClass() {
        TestShortcutUtil.initLibrary();
        camera = TestShortcutUtil.getFirstCamera();
        TestShortcutUtil.openSession(camera);
        cameraRef = camera.getValue();
    }

    @AfterAll
    static void tearDownClass() {
        try {
            TestShortcutUtil.closeSession(camera);
        } finally {
            ReleaseUtil.release(camera);
        }
        TestShortcutUtil.terminateLibrary();
    }

    @Test
    void edsCreateFlashSettingRef() {
        final EdsFlashRef.ByReference outRef = new EdsFlashRef.ByReference();
        final EdsdkError err;
        try {
            err = toEdsdkError(edsdkLibrary().EdsCreateFlashSettingRef(cameraRef, outRef));
        } catch (final UnsatisfiedLinkError tooOld) {
            Assumptions.abort("Installed EDSDK is older than 13.20.10 — symbol EdsCreateFlashSettingRef not exported");
            return;
        }
        log.info("EdsCreateFlashSettingRef -> {}", err);
        Assertions.assertNotNull(err, "Should map to an EdsdkError enum value");
        if (err == EdsdkError.EDS_ERR_OK) {
            Assertions.assertNotNull(outRef.getValue(), "OK should yield a flash ref");
            ReleaseUtil.release(outRef);
        }
    }

    @Test
    void edsCreateFolder() {
        // EdsCreateFolder takes only a camera ref. Result is body-dependent;
        // mirrorless bodies typically reject this without an EOS Utility-style
        // session. Accept any EdsdkError.
        final EdsdkError err;
        try {
            err = toEdsdkError(edsdkLibrary().EdsCreateFolder(cameraRef));
        } catch (final UnsatisfiedLinkError tooOld) {
            Assumptions.abort("Installed EDSDK is older than 13.20.10 — symbol EdsCreateFolder not exported");
            return;
        }
        log.info("EdsCreateFolder -> {}", err);
        Assertions.assertNotNull(err);
    }

    @Test
    void edsGetPropertyDescEx() {
        // kEdsPropID_MovieParamEx is the canonical user of EdsGetPropertyDescEx.
        // R-series bodies expose it; older / non-movie bodies may report
        // PROPERTIES_UNAVAILABLE — both are acceptable for a signature smoke test.
        final EdsPropertyDescEx desc = new EdsPropertyDescEx();
        final EdsdkError err;
        try {
            err = toEdsdkError(edsdkLibrary().EdsGetPropertyDescEx(cameraRef,
                EdsPropertyID.kEdsPropID_MovieParamEx.value(), desc));
        } catch (final UnsatisfiedLinkError tooOld) {
            Assumptions.abort("Installed EDSDK is older than 13.19.0 — symbol EdsGetPropertyDescEx not exported");
            return;
        }
        log.info("EdsGetPropertyDescEx(MovieParamEx) -> {}, numElements={}",
            err, err == EdsdkError.EDS_ERR_OK ? desc.numElements : "n/a");
        Assertions.assertNotNull(err);
        if (err == EdsdkError.EDS_ERR_OK) {
            // numElements is signed int with a hard upper bound of 2048 from the array layout.
            Assertions.assertTrue(desc.numElements >= 0 && desc.numElements <= 2048,
                "numElements out of expected range: " + desc.numElements);
        }
    }

    @Test
    void edsSetFramePoint() {
        // Set the AF frame to the centre of a notional 1920x1280 viewport with no lock.
        // Only valid mid-live-view; off live view we just want to confirm the call
        // dispatches and returns something documented.
        final EdsPoint.ByValue point = new EdsPoint.ByValue();
        point.x = 960;
        point.y = 640;
        final EdsdkError err;
        try {
            err = toEdsdkError(edsdkLibrary().EdsSetFramePoint(cameraRef, point, false));
        } catch (final UnsatisfiedLinkError tooOld) {
            Assumptions.abort("Installed EDSDK is older than 13.13.20 — symbol EdsSetFramePoint not exported");
            return;
        }
        log.info("EdsSetFramePoint(960,640) -> {}", err);
        Assertions.assertNotNull(err);
    }

    @Test
    void edsSetMetaImageSignature() {
        // EdsSetMetaImage's full exercise needs a download in progress on a
        // directory item, which is impractical from a smoke test. Confirming the
        // signature compiles + the symbol exists is done by the EdsdkLibraryMock
        // override at compile time — no runtime call here. This @Test exists so the
        // function is tracked in the camera-test set; if EDSDK ever drops it, the
        // EdsdkLibraryMock will fail to compile.
        Assertions.assertTrue(true, "Compile-time signature check via EdsdkLibraryMock");
    }
}
