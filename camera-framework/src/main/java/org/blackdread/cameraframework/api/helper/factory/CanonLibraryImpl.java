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
package org.blackdread.cameraframework.api.helper.factory;

import com.sun.jna.Native;
import com.sun.jna.Platform;
import org.blackdread.camerabinding.jna.EdsdkLibrary;
import org.blackdread.cameraframework.api.CanonLibrary;
import org.blackdread.cameraframework.api.command.TerminateSdkCommand;
import org.blackdread.cameraframework.util.DllUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.concurrent.ThreadSafe;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * <p>Created on 2018/10/16.<p>
 *
 * @author Yoann CAPLAIN
 * @since 1.0.0
 */
@ThreadSafe
class CanonLibraryImpl implements CanonLibrary {

    private static final Logger log = LoggerFactory.getLogger(CanonLibraryImpl.class);

    private static final String LIBRARY_PATH_PROPERTY = "blackdread.cameraframework.library.path";

    // fields are static but will change to instance fields

    /*
    static {
        final CodeSource codeSource = CanonLibraryImpl.class.getProtectionDomain().getCodeSource();
        final URL sourceLocation = codeSource.getLocation();
        if (sourceLocation != null) {
            try {
                final File tmp = new File(sourceLocation.toURI().getPath());
                jarFile = tmp;
                log.info("Jar file is in {}, folder {}", tmp.getPath(), tmp.getParentFile().getPath());
            } catch (URISyntaxException e) {
                throw new IllegalStateException("Failed to build jar file path");
            }
        }
    }
    //*/

    /*
     * no used yet, might be necessary in getLibPath()
     */
//    private static final File jarFile;
    /*
     * no used yet, might be necessary in getLibPath()
     */
//    private static final File jarDir = jarFile.getParentFile();

    private final Object initLibraryLock = new Object();

    /**
     * Instance of library
     */
    private volatile EdsdkLibrary EDSDK = null;

    private Thread shutdownHookThread = null;

    private ArchLibrary archLibraryToUse = ArchLibrary.AUTO;

    CanonLibraryImpl() {
    }

    @Override
    public EdsdkLibrary edsdkLibrary() {
        initLibrary();
        return EDSDK;
    }

    @Override
    public ArchLibrary getArchLibraryToUse() {
        return archLibraryToUse;
    }

    @Override
    public void setArchLibraryToUse(final ArchLibrary archLibraryToUse) {
        this.archLibraryToUse = archLibraryToUse;
    }

    /**
     * @return path to lib to load (Windows)
     */
    protected Optional<String> getLibPath() {
        final String jnaPath = System.getProperty(LIBRARY_PATH_PROPERTY);
        if (jnaPath != null) {
            // user has specified himself the path, we follow what he gave
            return Optional.of(jnaPath);
        }

        switch (archLibraryToUse) {
            case AUTO:
                // is64Bit() already checks java runtime with "sun.arch.data.model" for 32 or 64
                if (Platform.is64Bit()) {
                    log.info("Dll auto selected to 64 bit");
                    return Optional.of(DllUtil.DEFAULT_LIB_64_PATH);
                }
                throw unsupported32Bit("a 32-bit JVM was detected");
            case FORCE_32:
                throw unsupported32Bit("ArchLibrary.FORCE_32 was requested");
            case FORCE_64:
                log.info("Dll forced to 64 bit");
                return Optional.of(DllUtil.DEFAULT_LIB_64_PATH);
            default:
                throw new IllegalStateException("Enum unknown: " + archLibraryToUse);
        }
    }

    /**
     * 32-bit Windows is not supported by this fork, and must fail loudly.
     * <p>
     * Adding macOS support required switching every JNA callback in
     * {@code EdsdkLibrary} from {@code StdCallCallback} to a plain
     * {@code Callback}: JNA rejects {@code StdCallCallback} outright on macOS
     * ("Invalid calling convention"), and it decides a callback's convention
     * <i>solely</i> from that marker interface — the library-level
     * {@code OPTION_CALLING_CONVENTION} applies to function calls only, never to
     * callbacks.
     * <p>
     * On x64 that is harmless: Windows x64 has a single calling convention, so
     * {@code __stdcall} is ignored. On <b>32-bit</b> Windows, EDSDK declares its
     * handlers {@code __stdcall} ({@code EDSDKTypes.h}: {@code EDSCALLBACK}), and
     * invoking a cdecl trampoline through a {@code __stdcall} pointer drifts the
     * stack pointer inside EDSDK's event dispatch — an intermittent corruption
     * that surfaces far from its cause, on the first camera event.
     * <p>
     * Refusing to load is strictly better than corrupting memory. Restoring 32-bit
     * support means giving the handlers Win32-only {@code StdCallCallback} twins,
     * selected at registration time.
     */
    private static UnsupportedOperationException unsupported32Bit(final String reason) {
        return new UnsupportedOperationException(
                "32-bit Windows is not supported by this build (" + reason + "). "
                        + "EDSDK declares its callbacks __stdcall, but they are bound as cdecl so that "
                        + "macOS works, which would silently corrupt the stack on the first camera event. "
                        + "Run a 64-bit JVM, or use ArchLibrary.FORCE_64.");
    }

    /**
     * @return path to lib to load (macOS)
     */
    protected Optional<String> getLibPathMac() {
        final String jnaPath = System.getProperty(LIBRARY_PATH_PROPERTY);
        if (jnaPath != null) {
            // user has specified himself the path, we follow what he gave
            log.info("Using user-specified library path: {}", jnaPath);
            return Optional.of(jnaPath);
        }

        // Standard framework locations (in order of preference)
        final String[] paths = {
            // Working directory (for development/testing)
            System.getProperty("user.dir") + "/EDSDK.framework/EDSDK",
            "/Library/Frameworks/EDSDK.framework/EDSDK",
            System.getProperty("user.home") + "/Library/Frameworks/EDSDK.framework/EDSDK",
            "/System/Library/Frameworks/EDSDK.framework/EDSDK"
        };

        for (String path : paths) {
            File file = new File(path);
            if (file.exists()) {
                log.info("Found EDSDK framework at: {}", path);
                return Optional.of(path);
            }
            log.debug("EDSDK not found at: {}", path);
        }

        // No path found
        log.warn("EDSDK framework not found in standard locations");
        return Optional.empty();
    }

    /**
     * <p>Method is called at every call of {@link #edsdkLibrary()}</p>
     */
    protected void initLibrary() {
        if (EDSDK == null) {
            synchronized (initLibraryLock) {
                if (EDSDK == null) {
                    if (Platform.isWindows()) {
                        final String libPath = getLibPath()
                            .orElseThrow(() -> new IllegalStateException("Could not init library, lib path not found"));
                        // no options for now
                        EDSDK = Native.loadLibrary(libPath, EdsdkLibrary.class, getOptions());
                        registerCanonShutdownHook();
                        log.info("Library successfully loaded");
                        return;
                    } else if (Platform.isMac()) {
                        final String libPath = getLibPathMac()
                            .orElseThrow(() -> new IllegalStateException("Could not init library, lib path not found"));
                        EDSDK = Native.loadLibrary(libPath, EdsdkLibrary.class, getOptions());
                        registerCanonShutdownHook();
                        log.info("Library successfully loaded on MacOS");
                        return;
                    }
                    throw new IllegalStateException("Not supported OS: " + Platform.getOSType());
                }
            }
        }
    }

    /**
     * Returns JNA loading options with platform-specific configurations.
     * On macOS, overrides the StdCall convention (Windows-only) with standard C convention.
     *
     * @return JNA options map with platform-appropriate calling convention
     */
    private static Map<String, Object> getOptions() {
        Map<String, Object> options = new HashMap<>();

        // On macOS, override StdCallLibrary's Windows-specific calling convention
        // EDSDK on macOS uses standard C calling convention, not StdCall
        if (Platform.isMac()) {
            options.put(com.sun.jna.Library.OPTION_CALLING_CONVENTION,
                com.sun.jna.Function.C_CONVENTION);
        }
        // On Windows, StdCallLibrary's default convention is used automatically

        return options;
    }

    private void registerCanonShutdownHook() {
        final EdsdkLibrary edsdk = EDSDK;
        final Thread shutdownThread = new Thread(() ->
        {
            log.info("Shutdown hook run");
            CanonFactory.commandDispatcher().scheduleCommand(new TerminateSdkCommand());
//            if (edsdk != null)
//                edsdk.EdsTerminateSDK();
        });
        if (shutdownHookThread != null) {
            Runtime.getRuntime().removeShutdownHook(shutdownHookThread);
            shutdownHookThread.interrupt();
        }
        shutdownHookThread = shutdownThread;
        Runtime.getRuntime().addShutdownHook(shutdownThread);
        log.info("Registered shutdown hook of library");
    }
}
