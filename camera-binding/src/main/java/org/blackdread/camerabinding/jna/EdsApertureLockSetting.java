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
package org.blackdread.camerabinding.jna;

import com.sun.jna.Pointer;
import com.sun.jna.Structure;

import java.util.Arrays;
import java.util.List;

/**
 * Payload for kEdsPropID_ApertureLockSetting (EDSDK 13.20.10).
 * <br>
 * <i>native declaration : sdk-header\EDSDKTypes.h (tagEdsApertureLockSetting)</i>
 */
public class EdsApertureLockSetting extends Structure {
    /** C type : EdsUInt32 */
    public int apertureLockStatus;
    /** C type : EdsUInt32 */
    public int avValue;

    public EdsApertureLockSetting() {
        super();
    }

    @Override
    protected List<String> getFieldOrder() {
        return Arrays.asList("apertureLockStatus", "avValue");
    }

    public EdsApertureLockSetting(int apertureLockStatus, int avValue) {
        super();
        this.apertureLockStatus = apertureLockStatus;
        this.avValue = avValue;
    }

    public EdsApertureLockSetting(Pointer peer) {
        super(peer);
    }

    public static class ByReference extends EdsApertureLockSetting implements Structure.ByReference {
    }

    public static class ByValue extends EdsApertureLockSetting implements Structure.ByValue {
    }
}
