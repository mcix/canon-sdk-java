/*
 * MIT License
 *
 * Copyright (c) 2018-2020 Yoann CAPLAIN
 */
package org.blackdread.cameraframework.demo;

import com.sun.jna.Platform;
import org.junit.platform.engine.discovery.DiscoverySelectors;
import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;
import org.junit.platform.launcher.listeners.TestExecutionSummary;

import java.awt.Toolkit;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;

/**
 * Standalone JUnit 5 launcher that bootstraps NSApplication on the main thread
 * before EDSDK.framework is loaded, then runs the {@code @CameraIsConnected}
 * test classes. This sidesteps Surefire's forked-JVM problem on macOS where
 * dlopen on EDSDK.framework hangs because the test JVM has no Cocoa run loop.
 *
 * Run with:
 *   java -cp "$CP" -Dapple.awt.UIElement=true -DcanonCameraConnected=true \
 *        org.blackdread.cameraframework.demo.CameraTestRunner [TestClassName ...]
 *
 * With no args, runs the standard set of camera-required test classes.
 */
public final class CameraTestRunner {

    private static final List<String> DEFAULT_CLASSES = Arrays.asList(
        "org.blackdread.cameraframework.api.helper.logic.CameraLogicCameraTest",
        "org.blackdread.cameraframework.api.helper.logic.PropertyDescCameraTest",
        "org.blackdread.cameraframework.api.helper.logic.PropertyGetLogicCameraTest",
        "org.blackdread.cameraframework.api.helper.logic.PropertySetLogicCameraTest",
        "org.blackdread.cameraframework.api.helper.logic.LiveViewLogicCameraTest",
        "org.blackdread.cameraframework.api.helper.logic.LiveViewLogicOnCameraTest",
        "org.blackdread.cameraframework.api.helper.logic.MirrorlessLiveViewCameraTest",
        "org.blackdread.cameraframework.api.helper.logic.NewEdsdkBindingsCameraTest",
        "org.blackdread.cameraframework.api.helper.logic.ShootLogicCameraTest",
        "org.blackdread.cameraframework.api.helper.logic.ShootLogicWithEventFetcherCameraTest",
        "org.blackdread.cameraframework.api.helper.logic.event.CameraObjectEventLogicCameraTest",
        "org.blackdread.cameraframework.api.command.GetPropertyCommandCameraTest",
        "org.blackdread.cameraframework.api.ShootCameraTest",
        "org.blackdread.cameraframework.api.EventCameraTest",
        "org.blackdread.cameraframework.api.Event2CameraTest",
        "org.blackdread.cameraframework.api.CanonLibraryTest"
    );

    public static void main(final String[] args) {
        if (Platform.isMac()) {
            // Initialise AppKit on the main thread before EDSDK.framework is dlopen-ed.
            Toolkit.getDefaultToolkit();
            System.out.println("[runner] AWT toolkit initialised");
        }

        // canonCameraConnected gates @CameraIsConnected via @EnabledIfSystemProperty.
        if (!"true".equalsIgnoreCase(System.getProperty("canonCameraConnected"))) {
            System.setProperty("canonCameraConnected", "true");
            System.out.println("[runner] Set -DcanonCameraConnected=true");
        }

        final List<String> classNames = args.length == 0 ? DEFAULT_CLASSES : Arrays.asList(args);
        System.out.println("[runner] Running " + classNames.size() + " test class(es)");

        final LauncherDiscoveryRequestBuilder builder = LauncherDiscoveryRequestBuilder.request();
        for (final String name : classNames) {
            try {
                final Class<?> testClass = Class.forName(name);
                builder.selectors(DiscoverySelectors.selectClass(testClass));
            } catch (final ClassNotFoundException e) {
                System.err.println("[runner] Skipping (not on classpath): " + name);
            }
        }
        final LauncherDiscoveryRequest request = builder.build();
        final SummaryGeneratingListener listener = new SummaryGeneratingListener();

        final Launcher launcher = LauncherFactory.create();
        launcher.registerTestExecutionListeners(listener);
        launcher.execute(request);

        final TestExecutionSummary summary = listener.getSummary();
        final PrintWriter out = new PrintWriter(System.out, true);
        summary.printTo(out);

        if (!summary.getFailures().isEmpty()) {
            System.out.println();
            System.out.println("===== Failures =====");
            summary.printFailuresTo(out);
        }

        final int total = (int) summary.getTestsFoundCount();
        final int passed = (int) summary.getTestsSucceededCount();
        final int failed = (int) summary.getTestsFailedCount();
        final int skipped = (int) summary.getTestsSkippedCount();
        final int aborted = (int) summary.getTestsAbortedCount();
        System.out.printf("[runner] Done: total=%d passed=%d failed=%d skipped=%d aborted=%d%n",
            total, passed, failed, skipped, aborted);

        System.exit(failed == 0 ? 0 : 1);
    }

    private CameraTestRunner() {}
}
