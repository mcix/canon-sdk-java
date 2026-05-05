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

import com.sun.jna.NativeLong;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;

import java.util.Arrays;
import java.util.List;

/**
 * Extended property descriptor supporting 64-bit property values (up to 2048 entries).
 * <br>
 * Used with {@code EdsGetPropertyDescEx} to retrieve settable values for properties such as
 * {@code kEdsPropID_MovieParamEx}.
 * <br>
 * <i>native declaration : sdk-header\EDSDKTypes.h</i>
 *
 * @since edsdk 13.19.0
 */
public class EdsPropertyDescEx extends Structure {
    /** C type : EdsInt32 */
    public NativeLong form;
    /** C type : EdsInt32 */
    public NativeLong access;
    /** C type : EdsInt32 */
    public NativeLong numElements;
    /** C type : EdsInt64[2048] */
    public long[] propDesc = new long[2048];

    public EdsPropertyDescEx() {
        super();
    }

    @Override
    protected List<String> getFieldOrder() {
        return Arrays.asList("form", "access", "numElements", "propDesc");
    }

    public EdsPropertyDescEx(NativeLong form, NativeLong access, NativeLong numElements, long[] propDesc) {
        super();
        this.form = form;
        this.access = access;
        this.numElements = numElements;
        if (propDesc.length != this.propDesc.length)
            throw new IllegalArgumentException("Wrong array size!");
        this.propDesc = propDesc;
    }

    public EdsPropertyDescEx(Pointer peer) {
        super(peer);
    }

    public static class ByReference extends EdsPropertyDescEx implements Structure.ByReference {
    }

    public static class ByValue extends EdsPropertyDescEx implements Structure.ByValue {
    }
}
