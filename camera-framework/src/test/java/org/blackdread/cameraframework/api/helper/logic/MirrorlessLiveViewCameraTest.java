/*
 * MIT License
 *
 * Copyright (c) 2018-2020 Yoann CAPLAIN
 */
package org.blackdread.cameraframework.api.helper.logic;

import org.blackdread.camerabinding.jna.EdsdkLibrary;
import org.blackdread.camerabinding.jna.EdsdkLibrary.EdsCameraRef;
import org.blackdread.cameraframework.CameraIsConnected;
import org.blackdread.cameraframework.api.TestShortcutUtil;
import org.blackdread.cameraframework.api.constant.EdsEvfOutputDevice;
import org.blackdread.cameraframework.exception.error.EdsdkErrorException;
import org.blackdread.cameraframework.util.ReleaseUtil;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.image.BufferedImage;

import static org.blackdread.cameraframework.api.helper.factory.CanonFactory.liveViewLogic;

/**
 * End-to-end live-view exercise that works on both DSLR and mirrorless bodies.
 * Codifies the path that {@code R8LiveViewDemo} drives manually:
 * beginLiveView(PC) → fetch frames → endLiveView. Catches regressions in the
 * mirrorless-tolerant guards added to LiveViewLogicDefault for R-series bodies.
 */
@CameraIsConnected
class MirrorlessLiveViewCameraTest {

    private static final Logger log = LoggerFactory.getLogger(MirrorlessLiveViewCameraTest.class);

    private static final int FRAME_COUNT = 3;
    private static final long FRAME_WAIT_MS = 200;
    private static final long LIVE_VIEW_WARMUP_MS = 1500;

    private static EdsdkLibrary.EdsCameraRef.ByReference camera;
    private static EdsCameraRef cameraRef;

    @BeforeAll
    static void setUpClass() throws InterruptedException {
        TestShortcutUtil.initLibrary();
        camera = TestShortcutUtil.getFirstCamera();
        TestShortcutUtil.openSession(camera);
        cameraRef = camera.getValue();

        liveViewLogic().beginLiveView(cameraRef, EdsEvfOutputDevice.kEdsEvfOutputDevice_PC);
        // EDSDK needs a moment after Evf_OutputDevice flips before frames are ready.
        Thread.sleep(LIVE_VIEW_WARMUP_MS);
    }

    @AfterAll
    static void tearDownClass() {
        try {
            try {
                liveViewLogic().endLiveView(cameraRef);
            } catch (final EdsdkErrorException e) {
                log.warn("endLiveView during teardown: {}", e.getEdsdkError());
            }
            TestShortcutUtil.closeSession(camera);
        } finally {
            ReleaseUtil.release(camera);
        }
        TestShortcutUtil.terminateLibrary();
    }

    @Test
    void beginLiveViewSucceedsOnAnyBodyType() {
        // Implicit assertion: setUpClass already called beginLiveView. If it threw
        // (e.g. mirrorless body without the swallow guard) the class would have
        // aborted before this test started.
        Assertions.assertNotNull(cameraRef, "Live view should be active after @BeforeAll");
    }

    @Test
    void isLiveViewEnabledReportsTrueWhenActive() {
        // Whether via Evf_Mode (DSLR) or Evf_OutputDevice (mirrorless fallback),
        // isLiveViewEnabled must report true after beginLiveView(... PC).
        Assertions.assertTrue(liveViewLogic().isLiveViewEnabled(cameraRef),
            "isLiveViewEnabled must return true after beginLiveView with PC output");
    }

    @Test
    void capturesMultipleConsecutiveFrames() throws InterruptedException {
        int captured = 0;
        int totalBytes = 0;
        for (int i = 0; i < FRAME_COUNT; i++) {
            final BufferedImage frame = liveViewLogic().getLiveViewImage(cameraRef);
            Assertions.assertNotNull(frame, "Frame " + i + " was null");
            Assertions.assertTrue(frame.getWidth() > 0 && frame.getHeight() > 0,
                "Frame " + i + " has empty dimensions: " + frame.getWidth() + "x" + frame.getHeight());
            captured++;
            // getLiveViewImageBuffer returns the JPEG bytes of the same frame.
            final byte[] buf = liveViewLogic().getLiveViewImageBuffer(cameraRef);
            Assertions.assertTrue(buf != null && buf.length > 0,
                "Buffer for frame " + i + " was empty");
            totalBytes += buf.length;
            Thread.sleep(FRAME_WAIT_MS);
        }
        log.info("Captured {} frames, total {} bytes", captured, totalBytes);
        Assertions.assertEquals(FRAME_COUNT, captured);
        Assertions.assertTrue(totalBytes > 1024,
            "Expected non-trivial JPEG bytes, got " + totalBytes);
    }
}
