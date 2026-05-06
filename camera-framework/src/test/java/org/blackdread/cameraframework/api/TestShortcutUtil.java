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
package org.blackdread.cameraframework.api;

import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;
import org.blackdread.camerabinding.jna.EdsdkLibrary;
import org.blackdread.cameraframework.api.constant.EdsObjectEvent;
import org.blackdread.cameraframework.api.constant.EdsPropertyEvent;
import org.blackdread.cameraframework.api.constant.EdsStateEvent;
import org.blackdread.cameraframework.api.constant.EdsdkError;
import org.blackdread.cameraframework.api.helper.factory.CanonFactory;
import org.blackdread.cameraframework.util.ReleaseUtil;
import org.junit.jupiter.api.Assertions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.blackdread.cameraframework.api.TestUtil.assertNoError;
import static org.blackdread.cameraframework.api.helper.factory.CanonFactory.edsdkLibrary;
import static org.blackdread.cameraframework.util.ErrorUtil.toEdsdkError;

/**
 * <p>Created on 2018/10/26.</p>
 *
 * @author Yoann CAPLAIN
 */
public final class TestShortcutUtil {

    private static final Logger log = LoggerFactory.getLogger(TestShortcutUtil.class);


    /**
     * EDSDK on macOS (and to a lesser extent on Windows) does not tolerate
     * EdsTerminateSDK followed by another EdsInitializeSDK in the same process —
     * the second init returns EDS_ERR_INTERNAL_ERROR. When test classes run
     * back-to-back in one JVM (e.g. via CameraTestRunner) this caused every test
     * class after the first to fail in setUpClass.
     *
     * Track init state at the JVM level: only call EdsInitializeSDK once, and
     * make terminateLibrary a no-op so the SDK stays alive for the rest of the
     * tests. Real shutdown happens via the framework's shutdown hook
     * (CanonLibraryImpl.registerCanonShutdownHook).
     */
    private static volatile boolean sdkInitialized = false;

    public static synchronized void initLibrary() {
        if (sdkInitialized) {
            return;
        }
        final EdsdkError error = toEdsdkError(edsdkLibrary().EdsInitializeSDK());
        Assertions.assertEquals(EdsdkError.EDS_ERR_OK, error);
        sdkInitialized = true;
    }

    public static synchronized void terminateLibrary() {
        // Intentionally a no-op when running multiple test classes in the same
        // JVM. The CanonLibraryImpl shutdown hook will terminate the SDK at JVM
        // exit. If you need a hard reset, fork a fresh JVM.
    }

    public static synchronized void reloadLibrary() {
        // Same rationale as terminateLibrary — leave the SDK alive across classes.
    }

    /**
     * Do not forget to release the ref on fail
     *
     * @return Camera ref (not connected yet)
     */
    public static EdsdkLibrary.EdsCameraRef.ByReference getFirstCamera() {
        final EdsdkLibrary.EdsCameraListRef.ByReference cameraListRef = new EdsdkLibrary.EdsCameraListRef.ByReference();
        assertNoError(edsdkLibrary().EdsGetCameraList(cameraListRef));
        try {
            final IntByReference outRef = new IntByReference();
            assertNoError(edsdkLibrary().EdsGetChildCount(cameraListRef.getValue(), outRef));

            final long numCams = outRef.getValue() & 0xFFFFFFFFL;
            Assertions.assertTrue(numCams > 0, "No camera connected");

            final EdsdkLibrary.EdsCameraRef.ByReference cameraRef = new EdsdkLibrary.EdsCameraRef.ByReference();

            assertNoError(edsdkLibrary().EdsGetChildAtIndex(cameraListRef.getValue(), (int)(0), cameraRef));
            return cameraRef;
        } finally {
            ReleaseUtil.release(cameraListRef);
        }
    }

    public static void registerCameraAddedHandler(final EdsdkLibrary.EdsCameraAddedHandler handler) {
        CanonFactory.edsdkLibrary().EdsSetCameraAddedHandler(handler, Pointer.NULL);
    }

    public static void registerObjectEventHandler(final EdsdkLibrary.EdsCameraRef cameraRef, final EdsdkLibrary.EdsObjectEventHandler handler) {
        CanonFactory.edsdkLibrary().EdsSetObjectEventHandler(cameraRef, (int)(EdsObjectEvent.kEdsObjectEvent_All.value()), handler, Pointer.NULL);
    }

    public static void registerPropertyEventHandler(final EdsdkLibrary.EdsCameraRef cameraRef, final EdsdkLibrary.EdsPropertyEventHandler handler) {
        CanonFactory.edsdkLibrary().EdsSetPropertyEventHandler(cameraRef, (int)(EdsPropertyEvent.kEdsPropertyEvent_All.value()), handler, Pointer.NULL);
    }

    public static void registerStateEventHandler(final EdsdkLibrary.EdsCameraRef cameraRef, final EdsdkLibrary.EdsStateEventHandler handler) {
        CanonFactory.edsdkLibrary().EdsSetCameraStateEventHandler(cameraRef, (int)(EdsStateEvent.kEdsStateEvent_All.value()), handler, Pointer.NULL);
    }

    public static void openSession(final EdsdkLibrary.EdsCameraRef.ByReference camera) {
        assertNoError(edsdkLibrary().EdsOpenSession(camera.getValue()));
    }

    public static void closeSession(final EdsdkLibrary.EdsCameraRef.ByReference camera) {
        assertNoError(edsdkLibrary().EdsCloseSession(camera.getValue()));
    }

    public static void getEvents() {
        getEvents(50, 25);
    }

    public static void getEvents(final long sleepMillis, final int timeRetry) {
        for (int i = 0; i < timeRetry; i++) {
            CanonFactory.edsdkLibrary().EdsGetEvent();
            try {
                Thread.sleep(sleepMillis);
            } catch (InterruptedException e) {
                Assertions.fail("Interrupted");
            }
        }
    }

    private TestShortcutUtil() {
    }
}
