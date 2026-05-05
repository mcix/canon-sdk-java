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
package org.blackdread.cameraframework;

import com.google.common.collect.Lists;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.LongByReference;
import com.sun.jna.ptr.PointerByReference;
import com.sun.jna.ptr.ShortByReference;
import org.blackdread.camerabinding.jna.*;
import org.blackdread.cameraframework.api.constant.EdsdkError;
import org.junit.jupiter.api.Assertions;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Mock the library, very basic logic to mock, it might behaves differently but general idea is to get something that might help during tests without a camera.
 * If more behavior is required then should use Mockito is the appropriate test class
 * <p>Created on 2018/10/29.</p>
 *
 * @author Yoann CAPLAIN
 * @deprecated it is impossible to mock unless JNA classes are also mocked
 */
public class EdsdkLibraryMock implements EdsdkLibrary {

    private boolean sdkInitialized = false;

    private final Map<EdsBaseRef, Integer> countRetain = new HashMap<>();

    private final List<EdsCameraRef> camerasConnected = Lists.newArrayList(new EdsCameraRef(), new EdsCameraRef());

    private int returnOk() {
        return EdsdkError.EDS_ERR_OK.value();
    }

    private void assertWasInitialized() {
        Assertions.assertTrue(sdkInitialized, "EDSDK not initialized");
    }

    @Override
    public int EdsInitializeSDK() {
        sdkInitialized = true;
        return returnOk();
    }

    @Override
    public int EdsTerminateSDK() {
        sdkInitialized = false;
        return returnOk();
    }

    @Override
    public int EdsRetain(final EdsBaseRef inRef) {
        assertWasInitialized();
        final Integer total = countRetain.compute(inRef, (edsBaseRef, integer) -> {
            if (integer == null)
                return 1;
            else
                return integer + 1;
        });
        return total;
    }

    @Override
    public int EdsRelease(final EdsBaseRef inRef) {
        assertWasInitialized();
        final Integer total = countRetain.compute(inRef, (edsBaseRef, integer) -> {
            if (integer == null)
                return -1;
            else
                return integer - 1;
        });
        return total;
    }

    @Override
    public int EdsGetChildCount(final EdsBaseRef inRef, final IntByReference outCount) {
        return 0;
    }

    @Override
    public int EdsGetChildAtIndex(final EdsBaseRef inRef, final int inIndex, final EdsBaseRef.ByReference outRef) {
        return 0;
    }

    @Override
    public int EdsGetParent(final EdsBaseRef inRef, final EdsBaseRef.ByReference outParentRef) {
        return 0;
    }

    @Override
    public int EdsGetPropertySize(final EdsBaseRef inRef, final int inPropertyID, final int inParam, final IntByReference outDataType, final IntByReference outSize) {
        return 0;
    }

    @Override
    public int EdsGetPropertySize(final EdsBaseRef inRef, final int inPropertyID, final int inParam, final IntBuffer outDataType, final IntByReference outSize) {
        return 0;
    }

    @Override
    public int EdsGetPropertyData(final EdsBaseRef inRef, final int inPropertyID, final int inParam, final int inPropertySize, final Pointer outPropertyData) {
        return 0;
    }

    @Override
    public int EdsGetPropertyData(final EdsBaseRef inRef, final int inPropertyID, final int inParam, final int inPropertySize, final EdsVoid outPropertyData) {
        return 0;
    }

    @Override
    public int EdsSetPropertyData(final EdsBaseRef inRef, final int inPropertyID, final int inParam, final int inPropertySize, final Pointer inPropertyData) {
        return 0;
    }

    @Override
    public int EdsSetPropertyData(final EdsBaseRef inRef, final int inPropertyID, final int inParam, final int inPropertySize, final EdsVoid inPropertyData) {
        return 0;
    }

    @Override
    public int EdsGetPropertyDesc(final EdsBaseRef inRef, final int inPropertyID, final EdsPropertyDesc outPropertyDesc) {
        return 0;
    }

    @Override
    public int EdsGetCameraList(final EdsCameraListRef.ByReference outCameraListRef) {
        return 0;
    }

    @Override
    public int EdsGetDeviceInfo(final EdsCameraRef inCameraRef, final EdsDeviceInfo outDeviceInfo) {
        return 0;
    }

    @Override
    public int EdsOpenSession(final EdsCameraRef inCameraRef) {
        return 0;
    }

    @Override
    public int EdsCloseSession(final EdsCameraRef inCameraRef) {
        return 0;
    }

    @Override
    public int EdsSendCommand(final EdsCameraRef inCameraRef, final int inCommand, final int inParam) {
        return 0;
    }

    @Override
    public int EdsSendStatusCommand(final EdsCameraRef inCameraRef, final int inStatusCommand, final int inParam) {
        return 0;
    }

    @Override
    public int EdsSetCapacity(final EdsCameraRef inCameraRef, final EdsCapacity.ByValue inCapacity) {
        return 0;
    }

    @Override
    public int EdsGetVolumeInfo(final EdsVolumeRef inVolumeRef, final EdsVolumeInfo outVolumeInfo) {
        return 0;
    }

    @Override
    public int EdsFormatVolume(final EdsVolumeRef inVolumeRef) {
        return 0;
    }

    @Override
    public int EdsGetDirectoryItemInfo(final EdsDirectoryItemRef inDirItemRef, final EdsDirectoryItemInfo outDirItemInfo) {
        return 0;
    }

    @Override
    public int EdsDeleteDirectoryItem(final EdsDirectoryItemRef inDirItemRef) {
        return 0;
    }

    @Override
    public int EdsDownload(final EdsDirectoryItemRef inDirItemRef, final long inReadSize, final EdsStreamRef outStream) {
        return 0;
    }

    @Override
    public int EdsDownloadCancel(final EdsDirectoryItemRef inDirItemRef) {
        return 0;
    }

    @Override
    public int EdsDownloadComplete(final EdsDirectoryItemRef inDirItemRef) {
        return 0;
    }

    @Override
    public int EdsDownloadThumbnail(final EdsDirectoryItemRef inDirItemRef, final EdsStreamRef outStream) {
        return 0;
    }

    @Override
    public int EdsGetAttribute(final EdsDirectoryItemRef inDirItemRef, final IntByReference outFileAttribute) {
        return 0;
    }

    @Override
    public int EdsGetAttribute(final EdsDirectoryItemRef inDirItemRef, final IntBuffer outFileAttribute) {
        return 0;
    }

    @Override
    public int EdsSetAttribute(final EdsDirectoryItemRef inDirItemRef, final int inFileAttribute) {
        return 0;
    }

    @Override
    public int EdsCreateFileStream(final ByteBuffer inFileName, final int inCreateDisposition, final int inDesiredAccess, final EdsStreamRef.ByReference outStream) {
        return 0;
    }

    @Override
    public int EdsCreateFileStream(final byte[] inFileName, final int inCreateDisposition, final int inDesiredAccess, final EdsStreamRef.ByReference outStream) {
        return 0;
    }

    @Override
    public int EdsCreateMemoryStream(final long inBufferSize, final EdsStreamRef.ByReference outStream) {
        return 0;
    }

    @Override
    public int EdsCreateFileStreamEx(final short[] inFileName, final int inCreateDisposition, final int inDesiredAccess, final EdsStreamRef.ByReference outStream) {
        return 0;
    }

    @Override
    public int EdsCreateFileStreamEx(final ShortByReference inFileName, final int inCreateDisposition, final int inDesiredAccess, final EdsStreamRef.ByReference outStream) {
        return 0;
    }

    @Override
    public int EdsCreateMemoryStreamFromPointer(final Pointer inUserBuffer, final long inBufferSize, final EdsStreamRef.ByReference outStream) {
        return 0;
    }

    @Override
    public int EdsCreateMemoryStreamFromPointer(final EdsVoid inUserBuffer, final long inBufferSize, final EdsStreamRef.ByReference outStream) {
        return 0;
    }

    @Override
    public int EdsGetPointer(final EdsStreamRef inStream, final PointerByReference outPointer) {
        return 0;
    }

    @Override
    public int EdsRead(final EdsStreamRef inStreamRef, final long inReadSize, final Pointer outBuffer, final LongByReference outReadSize) {
        return 0;
    }

    @Override
    public int EdsRead(final EdsStreamRef inStreamRef, final long inReadSize, final EdsVoid outBuffer, final LongByReference outReadSize) {
        return 0;
    }

    @Override
    public int EdsWrite(final EdsStreamRef inStreamRef, final long inWriteSize, final Pointer inBuffer, final LongByReference outWrittenSize) {
        return 0;
    }

    @Override
    public int EdsWrite(final EdsStreamRef inStreamRef, final long inWriteSize, final EdsVoid inBuffer, final LongByReference outWrittenSize) {
        return 0;
    }

    @Override
    public int EdsSeek(final EdsStreamRef inStreamRef, final long inSeekOffset, final int inSeekOrigin) {
        return 0;
    }

    @Override
    public int EdsGetPosition(final EdsStreamRef inStreamRef, final LongByReference outPosition) {
        return 0;
    }

    @Override
    public int EdsGetLength(final EdsStreamRef inStreamRef, final LongByReference outLength) {
        return 0;
    }

    @Override
    public int EdsCopyData(final EdsStreamRef inStreamRef, final long inWriteSize, final EdsStreamRef outStreamRef) {
        return 0;
    }

    @Override
    public int EdsSetProgressCallback(final EdsBaseRef inRef, final EdsProgressCallback inProgressCallback, final int inProgressOption, final Pointer inContext) {
        return 0;
    }

    @Override
    public int EdsSetProgressCallback(final EdsBaseRef inRef, final EdsProgressCallback inProgressCallback, final int inProgressOption, final EdsVoid inContext) {
        return 0;
    }

    @Override
    public int EdsCreateImageRef(final EdsStreamRef inStreamRef, final EdsImageRef.ByReference outImageRef) {
        return 0;
    }

    @Override
    public int EdsGetImageInfo(final EdsImageRef inImageRef, final int inImageSource, final EdsImageInfo outImageInfo) {
        return 0;
    }

    @Override
    public int EdsGetImage(final EdsImageRef inImageRef, final int inImageSource, final int inImageType, final EdsRect.ByValue inSrcRect, final EdsSize.ByValue inDstSize, final EdsStreamRef outStreamRef) {
        return 0;
    }

    @Override
    public int EdsCreateEvfImageRef(final EdsStreamRef inStreamRef, final EdsEvfImageRef.ByReference outEvfImageRef) {
        return 0;
    }

    @Override
    public int EdsDownloadEvfImage(final EdsCameraRef inCameraRef, final EdsEvfImageRef inEvfImageRef) {
        return 0;
    }

    @Override
    public int EdsSetCameraAddedHandler(final EdsCameraAddedHandler inCameraAddedHandler, final Pointer inContext) {
        return 0;
    }

    @Override
    public int EdsSetCameraAddedHandler(final EdsCameraAddedHandler inCameraAddedHandler, final EdsVoid inContext) {
        return 0;
    }

    @Override
    public int EdsSetPropertyEventHandler(final EdsCameraRef inCameraRef, final int inEvnet, final EdsPropertyEventHandler inPropertyEventHandler, final Pointer inContext) {
        return 0;
    }

    @Override
    public int EdsSetPropertyEventHandler(final EdsCameraRef inCameraRef, final int inEvnet, final EdsPropertyEventHandler inPropertyEventHandler, final EdsVoid inContext) {
        return 0;
    }

    @Override
    public int EdsSetObjectEventHandler(final EdsCameraRef inCameraRef, final int inEvnet, final EdsObjectEventHandler inObjectEventHandler, final Pointer inContext) {
        return 0;
    }

    @Override
    public int EdsSetObjectEventHandler(final EdsCameraRef inCameraRef, final int inEvnet, final EdsObjectEventHandler inObjectEventHandler, final EdsVoid inContext) {
        return 0;
    }

    @Override
    public int EdsSetCameraStateEventHandler(final EdsCameraRef inCameraRef, final int inEvnet, final EdsStateEventHandler inStateEventHandler, final Pointer inContext) {
        return 0;
    }

    @Override
    public int EdsSetCameraStateEventHandler(final EdsCameraRef inCameraRef, final int inEvnet, final EdsStateEventHandler inStateEventHandler, final EdsVoid inContext) {
        return 0;
    }

    @Override
    public int EdsCreateStream(final EdsIStream inStream, final EdsStreamRef.ByReference outStreamRef) {
        return 0;
    }

    @Override
    public int EdsGetEvent() {
        return 0;
    }
}
