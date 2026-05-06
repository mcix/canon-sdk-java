/*
 * MIT License
 *
 * Copyright (c) 2018-2020 Yoann CAPLAIN
 */
package org.blackdread.cameraframework.demo;

import com.sun.jna.Platform;
import org.blackdread.camerabinding.jna.EdsdkLibrary;
import org.blackdread.cameraframework.api.TestShortcutUtil;
import org.blackdread.cameraframework.api.constant.EdsEvfOutputDevice;
import org.blackdread.cameraframework.api.helper.factory.CanonFactory;
import org.blackdread.cameraframework.exception.error.EdsdkErrorException;

import javax.imageio.ImageIO;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Standalone live-view exerciser for the Canon R8 (mirrorless).
 *
 * Runs outside of Surefire so it can initialise NSApplication on the main thread
 * before EDSDK.framework is loaded. EDSDK.framework links AppKit/Cocoa and dlopen
 * blocks indefinitely if the loading process has no Cocoa run loop, which is why
 * `mvn test` cannot drive these tests on macOS.
 *
 * Run with:
 *   mvn -pl camera-framework dependency:build-classpath -Dmdep.outputFile=/tmp/cp.txt
 *   java -cp "$(cat /tmp/cp.txt):camera-framework/target/classes:camera-framework/target/test-classes" \
 *        -Dapple.awt.UIElement=true \
 *        -DcanonCameraConnected=true \
 *        org.blackdread.cameraframework.demo.R8LiveViewDemo
 */
public final class R8LiveViewDemo {

    private static final int FRAME_COUNT = 5;
    private static final long FRAME_INTERVAL_MS = 200;
    private static final File OUTPUT_DIR = new File(System.getProperty("user.home"),
        "edsdk-r8-liveview-out");

    public static void main(final String[] args) throws Exception {
        System.out.println("R8 live-view demo starting on " + System.getProperty("os.name"));

        if (Platform.isMac()) {
            // Touch AWT before JNA loads EDSDK so AppKit is brought up on the main thread.
            // Without this, dlopen on EDSDK.framework hangs forever in a non-GUI process.
            Toolkit.getDefaultToolkit();
            System.out.println("AWT toolkit initialised");
        }

        Files.createDirectories(OUTPUT_DIR.toPath());
        System.out.println("Frames will be written to " + OUTPUT_DIR);

        TestShortcutUtil.initLibrary();
        System.out.println("EDSDK initialised");

        final EdsdkLibrary.EdsCameraRef.ByReference cameraByRef = TestShortcutUtil.getFirstCamera();
        TestShortcutUtil.openSession(cameraByRef);
        final EdsdkLibrary.EdsCameraRef cameraRef = cameraByRef.getValue();
        System.out.println("Session opened on first camera");

        try {
            CanonFactory.liveViewLogic().beginLiveView(cameraRef, EdsEvfOutputDevice.kEdsEvfOutputDevice_PC);
            System.out.println("Live view requested (PC)");

            // EDSDK needs a moment after EvfOutputDevice flips before frames are ready.
            Thread.sleep(2000);

            int success = 0;
            int failures = 0;
            for (int i = 0; i < FRAME_COUNT; i++) {
                try {
                    final BufferedImage frame = CanonFactory.liveViewLogic().getLiveViewImage(cameraRef);
                    final File out = new File(OUTPUT_DIR, String.format("frame-%02d.jpg", i));
                    ImageIO.write(frame, "jpg", out);
                    System.out.printf("frame %d: %dx%d -> %s%n",
                        i, frame.getWidth(), frame.getHeight(), out.getName());
                    success++;
                } catch (final EdsdkErrorException e) {
                    System.out.printf("frame %d: %s%n", i, e.getEdsdkError());
                    failures++;
                }
                Thread.sleep(FRAME_INTERVAL_MS);
            }
            System.out.printf("Done. %d frames captured, %d failures.%n", success, failures);

            CanonFactory.liveViewLogic().endLiveView(cameraRef);
            System.out.println("Live view stopped");
        } finally {
            TestShortcutUtil.closeSession(cameraByRef);
            TestShortcutUtil.terminateLibrary();
        }
    }

    private R8LiveViewDemo() {}
}
