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
package org.blackdread.cameraframework.demo;

import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.NativeLong;
import com.sun.jna.Platform;

/**
 * Demonstrates the NativeLong size difference between Windows and macOS.
 *
 * Run this on both platforms to see why EDSDK bindings fail on macOS.
 *
 * Expected output:
 * - Windows (32-bit or 64-bit): NativeLong.SIZE = 4
 * - macOS/Linux (64-bit):       NativeLong.SIZE = 8
 *
 * EDSDK expects 4-byte integers for EdsUInt32/EdsInt32 types on ALL platforms.
 */
public class NativeLongSizeDemo {

    public static void main(String[] args) {
        System.out.println("=== NativeLong Size Demo ===\n");

        // Platform info
        System.out.println("Platform Information:");
        System.out.println("  OS Name:      " + System.getProperty("os.name"));
        System.out.println("  OS Arch:      " + System.getProperty("os.arch"));
        System.out.println("  Java Version: " + System.getProperty("java.version"));
        System.out.println("  JNA Platform: " + Platform.getOSType());
        System.out.println("  Is 64-bit:    " + Platform.is64Bit());
        System.out.println();

        // The key difference
        System.out.println("Type Sizes:");
        System.out.println("  NativeLong.SIZE: " + Native.LONG_SIZE + " bytes");
        System.out.println("  Java int:        4 bytes (always)");
        System.out.println("  Java long:       8 bytes (always)");
        System.out.println();

        // Demonstrate the problem
        System.out.println("=== The Problem ===\n");

        int testValue = 0x00000501; // kEdsPropID_Evf_Mode
        System.out.println("Test value: 0x" + Integer.toHexString(testValue) + " (" + testValue + ")");
        System.out.println();

        // Using NativeLong (current implementation)
        Memory nativeLongMemory = new Memory(Native.LONG_SIZE);
        nativeLongMemory.setNativeLong(0, new NativeLong(testValue));

        System.out.println("Using setNativeLong() - writes " + Native.LONG_SIZE + " bytes:");
        System.out.println("  Memory dump: " + bytesToHex(nativeLongMemory.getByteArray(0, Native.LONG_SIZE)));
        System.out.println("  Read back with getNativeLong(): " + nativeLongMemory.getNativeLong(0));

        // Using int (correct for EDSDK)
        Memory intMemory = new Memory(4);
        intMemory.setInt(0, testValue);

        System.out.println();
        System.out.println("Using setInt() - writes 4 bytes:");
        System.out.println("  Memory dump: " + bytesToHex(intMemory.getByteArray(0, 4)));
        System.out.println("  Read back with getInt(): " + intMemory.getInt(0));

        // Show what happens when sizes mismatch
        System.out.println();
        System.out.println("=== Cross-read Demonstration ===\n");

        if (Native.LONG_SIZE == 8) {
            System.out.println("On this platform (macOS/Linux 64-bit), NativeLong is 8 bytes.");
            System.out.println("EDSDK expects 4-byte values, so there's a mismatch!\n");

            // Write 4 bytes, read 8 - reads garbage in upper bytes
            Memory mem = new Memory(8);
            mem.clear();
            mem.setInt(0, testValue);
            System.out.println("Write 4 bytes with setInt(), then read 8 with getNativeLong():");
            System.out.println("  Written: 0x" + Integer.toHexString(testValue));
            System.out.println("  Read:    0x" + Long.toHexString(mem.getNativeLong(0).longValue()));
            System.out.println("  (Upper 4 bytes are whatever was in memory)");

            System.out.println();

            // Write 8 bytes when only 4 expected - overwrites adjacent memory
            Memory smallMem = new Memory(4);
            System.out.println("If we allocate only 4 bytes but use setNativeLong():");
            System.out.println("  This would write 8 bytes into 4-byte buffer!");
            System.out.println("  (Could cause memory corruption or crash)");

        } else {
            System.out.println("On this platform (Windows), NativeLong is 4 bytes.");
            System.out.println("This matches what EDSDK expects, so no problem here.");
            System.out.println();
            System.out.println("But code using NativeLong will FAIL on macOS/Linux 64-bit!");
        }

        System.out.println();
        System.out.println("=== Conclusion ===\n");

        if (Native.LONG_SIZE == 8) {
            System.out.println("THIS PLATFORM HAS THE BUG!");
            System.out.println("The camera-framework uses NativeLong which is 8 bytes here,");
            System.out.println("but EDSDK expects 4-byte integers.");
            System.out.println();
            System.out.println("Fix: Replace NativeLong with int in EdsdkLibrary.java");
            System.out.println("     Replace getNativeLong()/setNativeLong() with getInt()/setInt()");
        } else {
            System.out.println("This platform works correctly by coincidence.");
            System.out.println("NativeLong happens to be 4 bytes, matching EDSDK's expectation.");
            System.out.println();
            System.out.println("But the code is not portable! Run this on macOS to see the issue.");
        }
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < bytes.length; i++) {
            if (i > 0) sb.append(" ");
            sb.append(String.format("%02X", bytes[i]));
        }
        return sb.toString();
    }
}
