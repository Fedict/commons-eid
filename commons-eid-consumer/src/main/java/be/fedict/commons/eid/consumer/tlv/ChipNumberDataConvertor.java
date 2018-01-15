/*
 * Commons eID Project.
 * Copyright (C) 2008-2013 FedICT.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License version
 * 3.0 as published by the Free Software Foundation.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, see 
 * http://www.gnu.org/licenses/.
 */

package be.fedict.commons.eid.consumer.tlv;

/**
 * Convertor for the chip number field.
 * 
 * @author Frank Cornelis
 * 
 */
public class ChipNumberDataConvertor implements DataConvertor<String> {

	@Override
	public String convert(byte[] value) {
		StringBuilder result = new StringBuilder();
		for (byte b : value) {
			result.append(String.format("%02X", b));
		}
		return result.toString();
	}
}
