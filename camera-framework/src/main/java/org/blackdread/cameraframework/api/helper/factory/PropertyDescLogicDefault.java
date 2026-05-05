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

import org.blackdread.camerabinding.jna.EdsdkLibrary.EdsBaseRef;
import org.blackdread.cameraframework.api.constant.*;
import org.blackdread.cameraframework.api.helper.logic.PropertyDescLogic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * <p>Created on 2018/10/28.</p>
 *
 * @author Yoann CAPLAIN
 * @since 1.0.0
 */
public class PropertyDescLogicDefault implements PropertyDescLogic {

    private static final Logger log = LoggerFactory.getLogger(PropertyDescLogicDefault.class);

    protected PropertyDescLogicDefault() {
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends NativeEnum<Integer>> List<T> getPropertyDesc(final EdsBaseRef camera, final EdsPropertyID property) {
        // Validate property up-front so callers still get a clear error for properties
        // that have no enum mapping. Per-value unknown enum values are skipped below.
        rejectUnsupportedProperty(property);

        final List<Integer> propertyDescValues = getPropertyDescValues(camera, property);
        final List<NativeEnum<Integer>> nativeEnums = new ArrayList<>(propertyDescValues.size());

        for (final Integer propertyDescValue : propertyDescValues) {
            try {
                nativeEnums.add(mapDescValue(property, propertyDescValue));
            } catch (final IllegalArgumentException unknownValue) {
                // Newer bodies (R8, etc.) report values not yet in the framework's enum
                // for an otherwise-supported property. Skip rather than abort the list.
                log.debug("Skipping unknown {} value 0x{}", property, Integer.toHexString(propertyDescValue));
            }
        }

        return (List<T>) nativeEnums;
    }

    private static void rejectUnsupportedProperty(final EdsPropertyID property) {
        switch (property) {
            case kEdsPropID_AEMode:
            case kEdsPropID_AEModeSelect:
            case kEdsPropID_ISOSpeed:
            case kEdsPropID_MeteringMode:
            case kEdsPropID_Av:
            case kEdsPropID_Tv:
            case kEdsPropID_ExposureCompensation:
            case kEdsPropID_ImageQuality:
            case kEdsPropID_WhiteBalance:
            case kEdsPropID_PictureStyle:
            case kEdsPropID_DriveMode:
            case kEdsPropID_Evf_WhiteBalance:
            case kEdsPropID_Evf_AFMode:
            case kEdsPropID_DC_Strobe:
                return;
            case kEdsPropID_DC_Zoom:
            case kEdsPropID_ColorTemperature:
            case kEdsPropID_Evf_ColorTemperature:
                throw new IllegalArgumentException("Cannot get desc values of " + property + " as those are not defined in an enum.  Need to use other methods");
            default:
                throw new IllegalArgumentException("Property " + property + " is not supported to get property desc");
        }
    }

    private NativeEnum<Integer> mapDescValue(final EdsPropertyID property, final Integer propertyDescValue) {
        switch (property) {
            case kEdsPropID_AEMode: // added this one as seems more logical but to check (not in documentation 3.9.0)
                // TODO to remove if not possible in fact
                return EdsAEMode.ofValue(propertyDescValue);
            case kEdsPropID_AEModeSelect: // in documentation it is this property but maybe was a typo? need to check later
                // TODO to remove if not possible in fact
                return EdsAEModeSelect.ofValue(propertyDescValue);
            case kEdsPropID_ISOSpeed:
                return EdsISOSpeed.ofValue(propertyDescValue);
            case kEdsPropID_MeteringMode:
                return EdsMeteringMode.ofValue(propertyDescValue);
            case kEdsPropID_Av:
                return EdsAv.ofValue(propertyDescValue);
            case kEdsPropID_Tv:
                return EdsTv.ofValue(propertyDescValue);
            case kEdsPropID_ExposureCompensation:
                return EdsExposureCompensation.ofValue(propertyDescValue);
            case kEdsPropID_ImageQuality:
                return EdsImageQuality.ofValue(propertyDescValue);
            case kEdsPropID_WhiteBalance:
                return EdsWhiteBalance.ofValue(propertyDescValue);
            case kEdsPropID_PictureStyle:
                return EdsPictureStyle.ofValue(propertyDescValue);
            case kEdsPropID_DriveMode:
                return EdsDriveMode.ofValue(propertyDescValue);
            case kEdsPropID_Evf_WhiteBalance:
                return EdsWhiteBalance.ofValue(propertyDescValue);
            case kEdsPropID_Evf_AFMode:
                return EdsEvfAFMode.ofValue(propertyDescValue);
            case kEdsPropID_DC_Strobe:
                return EdsDcStrobe.ofValue(propertyDescValue);
            default:
                throw new IllegalStateException("rejectUnsupportedProperty failed to filter " + property);
        }
    }

}
