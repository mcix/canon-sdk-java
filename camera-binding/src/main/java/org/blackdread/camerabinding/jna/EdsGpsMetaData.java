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
 * GPS meta-data payload (EDSDK 13.20.10).
 * <br>
 * Latitude, longitude and timestamp are arrays of three EdsRational values
 * representing degrees/minutes/seconds (or hours/minutes/seconds for time).
 * <br>
 * <i>native declaration : sdk-header\EDSDKTypes.h (tagEdsGpsMetaData)</i>
 */
public class EdsGpsMetaData extends Structure {
    /** C type : EdsUInt8 */
    public byte latitudeRef;
    /** C type : EdsUInt8 */
    public byte longitudeRef;
    /** C type : EdsUInt8 */
    public byte altitudeRef;
    /** C type : EdsUInt8 */
    public byte status;
    /** C type : EdsRational[3] (degrees, minutes, seconds) */
    public EdsRational[] latitude = new EdsRational[3];
    /** C type : EdsRational[3] (degrees, minutes, seconds) */
    public EdsRational[] longitude = new EdsRational[3];
    /** C type : EdsRational */
    public EdsRational altitude;
    /** C type : EdsRational[3] (hours, minutes, seconds UTC) */
    public EdsRational[] timeStamp = new EdsRational[3];
    /** C type : EdsUInt16 */
    public short dateStampYear;
    /** C type : EdsUInt8 */
    public byte dateStampMonth;
    /** C type : EdsUInt8 */
    public byte dateStampDay;

    public EdsGpsMetaData() {
        super();
    }

    @Override
    protected List<String> getFieldOrder() {
        return Arrays.asList(
            "latitudeRef", "longitudeRef", "altitudeRef", "status",
            "latitude", "longitude", "altitude", "timeStamp",
            "dateStampYear", "dateStampMonth", "dateStampDay"
        );
    }

    public EdsGpsMetaData(Pointer peer) {
        super(peer);
    }

    public static class ByReference extends EdsGpsMetaData implements Structure.ByReference {
    }

    public static class ByValue extends EdsGpsMetaData implements Structure.ByValue {
    }
}
