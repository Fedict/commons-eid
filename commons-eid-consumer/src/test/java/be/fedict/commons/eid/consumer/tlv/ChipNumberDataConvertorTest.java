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

package be.fedict.commons.eid.consumer.tlv;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ChipNumberDataConvertorTest {

	private static final byte[] ID = { 0x01, (byte)0xa0, 0x39, 0x56};

	@Test
	public void convertsTheIdCorrectly() {
		assertEquals("01A03956", new ChipNumberDataConvertor().convert(ID));
	}

}