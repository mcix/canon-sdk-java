/*
 * MIT License
 *
 * Copyright (c) 2018-2020 Yoann CAPLAIN
 */
package org.blackdread.cameraframework.demo;

import com.sun.jna.Platform;
import org.blackdread.camerabinding.jna.EdsdkLibrary;
import org.blackdread.cameraframework.api.TestShortcutUtil;
import org.blackdread.cameraframework.api.constant.EdsPropertyID;
import org.blackdread.cameraframework.api.helper.factory.CanonFactory;
import org.blackdread.cameraframework.api.helper.logic.PropertyInfo;
import org.blackdread.cameraframework.exception.error.EdsdkErrorException;

import java.awt.Toolkit;

/**
 * Probes every {@link EdsPropertyID} on the connected camera, printing the
 * data type and size returned by EdsGetPropertySize. Used to refresh
 * {@code PropertyGetLogicCameraTest#propertyTypeAndSizeExpected} for a new
 * body. Output format is the JUnit {@code arguments(...)} call needed by the
 * test, so the result can be pasted directly.
 */
public final class PropertyTypeProbe {

    public static void main(final String[] args) throws Exception {
        if (Platform.isMac()) {
            Toolkit.getDefaultToolkit();
        }
        TestShortcutUtil.initLibrary();

        final EdsdkLibrary.EdsCameraRef.ByReference cameraByRef = TestShortcutUtil.getFirstCamera();
        TestShortcutUtil.openSession(cameraByRef);
        final EdsdkLibrary.EdsCameraRef cameraRef = cameraByRef.getValue();
        System.out.println("Probing properties on first camera");

        try {
            int ok = 0;
            int unavailable = 0;
            int errored = 0;
            for (final EdsPropertyID id : EdsPropertyID.values()) {
                if (id == EdsPropertyID.kEdsPropID_Unknown) continue;
                try {
                    final PropertyInfo info = CanonFactory.propertyLogic().getPropertyTypeAndSize(cameraRef, id);
                    System.out.printf("            arguments(EdsPropertyID.%s, EdsDataType.%s, %d),%n",
                        id.name(), info.getDataType().name(), info.getSize());
                    ok++;
                } catch (final EdsdkErrorException e) {
                    if (e.getEdsdkError().name().contains("UNAVAILABLE") || e.getEdsdkError().name().contains("NOT_SUPPORTED")) {
                        System.out.printf("//          arguments(EdsPropertyID.%s, ?, ?), // %s%n", id.name(), e.getEdsdkError().name());
                        unavailable++;
                    } else {
                        System.out.printf("//          arguments(EdsPropertyID.%s, ?, ?), // %s%n", id.name(), e.getEdsdkError().name());
                        errored++;
                    }
                }
            }
            System.out.printf("%n=== Total: %d supported, %d unavailable, %d errored%n",
                ok, unavailable, errored);
        } finally {
            TestShortcutUtil.closeSession(cameraByRef);
        }
    }

    private PropertyTypeProbe() {}
}
