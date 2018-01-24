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

import java.util.GregorianCalendar;

/**
 * Data convertor for eID identity validity dates.
 * 
 * @author Frank Cornelis
 * 
 */
public class ValidityDateDataConvertor implements DataConvertor<GregorianCalendar> {

	@Override
	public GregorianCalendar convert(byte[] value) {
		String dateStr = new String(value);
		int day = Integer.parseInt(dateStr.substring(0, 2));
		int month = Integer.parseInt(dateStr.substring(3, 5));
		int year = Integer.parseInt(dateStr.substring(6));

		return new GregorianCalendar(year, month - 1, day);
	}
}
