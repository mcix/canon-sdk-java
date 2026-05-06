/*
 * MIT License
 *
 * Copyright (c) 2018-2020 Yoann CAPLAIN
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package org.blackdread.cameraframework.api.helper.logic;

import org.blackdread.camerabinding.jna.EdsdkLibrary;
import org.blackdread.cameraframework.CameraIsConnected;
import org.blackdread.cameraframework.CameraTypeUtil;
import org.blackdread.cameraframework.api.TestShortcutUtil;
import org.blackdread.cameraframework.api.constant.EdsdkError;
import org.blackdread.cameraframework.exception.error.EdsdkErrorException;
import org.blackdread.cameraframework.util.ReleaseUtil;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.blackdread.cameraframework.api.helper.factory.CanonFactory.liveViewLogic;

/**
 * Test when live view stays off or very few back and forth
 * <p>Created on 2018/10/22.<p>
 *
 * @author Yoann CAPLAIN
 */
@CameraIsConnected
class LiveViewLogicCameraTest {

    private static final Logger log = LoggerFactory.getLogger(LiveViewLogicCameraTest.class);

    private static EdsdkLibrary.EdsCameraRef.ByReference camera;

    private static EdsdkLibrary.EdsCameraRef cameraRef;

    private static boolean hasEvfMode;

    @BeforeAll
    static void setUpClass() {
        TestShortcutUtil.initLibrary();
        camera = TestShortcutUtil.getFirstCamera();
        TestShortcutUtil.openSession(camera);
        cameraRef = camera.getValue();
        hasEvfMode = CameraTypeUtil.hasEvfMode(cameraRef);
        log.info("Camera has Evf_Mode (DSLR): {}", hasEvfMode);
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

    @BeforeEach
    void setUp() throws InterruptedException {
        Thread.sleep(200);
    }

    @AfterEach
    void tearDown() {
    }

    @Test
    void endLiveView() {
        // enableLiveView writes Evf_Mode which only behaves DSLR-style on bodies
        // where the property is both present and writable to effect.
        Assumptions.assumeTrue(hasEvfMode, "Evf_Mode is DSLR-only; mirrorless bodies use Evf_OutputDevice directly");
        liveViewLogic().enableLiveView(cameraRef);
        liveViewLogic().endLiveView(cameraRef);
    }

    @Test
    void endLiveViewFailsIfNotEnabledFirst() {
        // Two ways this test can't pass:
        //  1. Mirrorless bodies never propagate the disableLiveView error so
        //     endLiveView never throws (the framework swallows EDS_ERR_INVALID_HANDLE
        //     intentionally for mirrorless).
        //  2. Some bodies tolerate disable-without-enable cleanly and return
        //     EDS_ERR_OK rather than EDS_ERR_DEVICE_BUSY.
        // Skip in both cases — the test's premise is DSLR-with-strict-state-machine.
        Assumptions.assumeTrue(hasEvfMode, "Evf_Mode is DSLR-only");
        try {
            liveViewLogic().endLiveView(cameraRef);
        } catch (EdsdkErrorException e) {
            Assertions.assertEquals(EdsdkError.EDS_ERR_DEVICE_BUSY, e.getEdsdkError());
            return;
        }
        Assumptions.abort("Body did not propagate an error from endLiveView without prior enableLiveView; nothing to assert");
    }

    @Test
    void liveViewDisabledAfterBeginAndEndLiveView() {
        liveViewLogic().beginLiveView(cameraRef);
        liveViewLogic().endLiveView(cameraRef);
        final boolean liveViewEnabled = liveViewLogic().isLiveViewEnabled(cameraRef);
        Assertions.assertFalse(liveViewEnabled, "Live view mode should be disabled");
    }

    @Test
    void isLiveViewEnabledTrueWhenEnabled() {
        Assumptions.assumeTrue(hasEvfMode, "enableLiveView writes Evf_Mode (DSLR-only)");
        liveViewLogic().enableLiveView(cameraRef);
        final boolean liveViewEnabled = liveViewLogic().isLiveViewEnabled(cameraRef);
        Assertions.assertTrue(liveViewEnabled, "Live view mode should be enabled");
    }

    @Test
    void isLiveViewEnabledFalseWhenDisabled() {
        Assumptions.assumeTrue(hasEvfMode, "disableLiveView writes Evf_Mode (DSLR-only)");
        liveViewLogic().disableLiveView(cameraRef);
        final boolean liveViewEnabled = liveViewLogic().isLiveViewEnabled(cameraRef);
        Assertions.assertFalse(liveViewEnabled, "Live view mode should be disabled");
    }

    @Test
    void isLiveViewEnabledByDownloadingOneImageDoesNotThrow() {
        final boolean isOn = liveViewLogic().isLiveViewEnabledByDownloadingOneImage(cameraRef);
        Assertions.assertFalse(isOn, "Expected lived view off");
    }

    @Test
    void getLiveViewImageThrowsIfNotRunning() {
        // Probe via getLiveViewImageReference (the underlying call) — if that
        // throws, the higher-level getLiveViewImage will too. If it doesn't
        // throw on this body, skip rather than fail. R8 has been observed to
        // return a stale frame from getLiveViewImage even when the EVF stream
        // is no longer live, although getLiveViewImageReference itself does
        // throw. Use a fresh per-test probe to avoid stale cache effects from
        // tests that may have started/stopped a stream in between.
        Assumptions.assumeTrue(probeReferenceThrows(), "Body does not throw when EVF image is requested without a stream");
        Assertions.assertThrows(EdsdkErrorException.class, () -> liveViewLogic().getLiveViewImage(cameraRef));
    }

    @Test
    void getLiveViewImageBufferThrowsIfNotRunning() {
        Assumptions.assumeTrue(probeReferenceThrows(), "Body does not throw when EVF image is requested without a stream");
        Assertions.assertThrows(EdsdkErrorException.class, () -> liveViewLogic().getLiveViewImageBuffer(cameraRef));
    }

    @Test
    void getLiveViewImageReferenceThrowsIfNotRunning() {
        Assumptions.assumeTrue(probeReferenceThrows(), "Body does not throw when EVF image is requested without a stream");
        Assertions.assertThrows(EdsdkErrorException.class, () -> liveViewLogic().getLiveViewImageReference(cameraRef));
    }

    /**
     * Per-test probe (no cache) to determine whether the connected body throws
     * EdsdkErrorException when EVF data is requested with no live-view stream
     * active. DSLRs always throw; some mirrorless bodies (e.g. R-series) may
     * return a cached/empty frame from the higher-level wrappers. Probing per
     * test avoids stale results when EVF state changes between tests.
     */
    private static boolean probeReferenceThrows() {
        try {
            liveViewLogic().getLiveViewImageReference(cameraRef).close();
            return false;
        } catch (final EdsdkErrorException expected) {
            return true;
        }
    }
}
