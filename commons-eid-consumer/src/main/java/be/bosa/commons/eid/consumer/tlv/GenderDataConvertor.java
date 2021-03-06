/*
 * Commons eID Project.
 * Copyright (C) 2014 - 2018 BOSA.
 *
 * This is free software; you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License version 3.0 as published by
 * the Free Software Foundation.
 *
 * This software is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this software; if not, see https://www.gnu.org/licenses/.
 */

package be.bosa.commons.eid.consumer.tlv;

import be.bosa.commons.eid.consumer.Gender;

/**
 * Data convertor for gender data type.
 * 
 * @author Frank Cornelis
 * 
 */
public class GenderDataConvertor implements DataConvertor<Gender> {

	@Override
	public Gender convert(byte[] value) throws DataConvertorException {
		String genderStr = new String(value);
		if ("M".equals(genderStr)) {
			return Gender.MALE;
		}
		if ("F".equals(genderStr)) {
			return Gender.FEMALE;
		}
		if ("V".equals(genderStr)) {
			return Gender.FEMALE;
		}
		if ("W".equals(genderStr)) {
			return Gender.FEMALE;
		}

		throw new DataConvertorException("unknown gender: " + genderStr);
	}
}
