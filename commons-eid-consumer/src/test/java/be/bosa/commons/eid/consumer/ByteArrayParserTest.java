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

package be.bosa.commons.eid.consumer;

import be.bosa.commons.eid.consumer.tlv.ByteArrayParser;
import org.junit.Test;

import java.math.BigInteger;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ByteArrayParserTest {

	@Test
	public void testByteArrayParser() {
		byte[] cardDataBytes = new BigInteger("534c494e33660013930d2061c018063fd0004801011100020001010f", 16).toByteArray();
		byte[] serialExpected = new byte[16];
		byte[] chipSerialExpected = new byte[12];
		System.arraycopy(cardDataBytes, 0, serialExpected, 0, 16);
		System.arraycopy(cardDataBytes, 4, chipSerialExpected, 0, 12);

		CardData cardData = ByteArrayParser.parse(cardDataBytes, CardData.class);
		assertEquals(cardData.applicationInterfaceVersion, 0);
		assertEquals(cardData.applicationLifeCycle, 15);
		assertEquals(cardData.applicationVersion, 17);
		assertEquals(cardData.componentCode, 208);
		assertEquals(cardData.globalOSVersion, 2);
		assertEquals(cardData.keyExchangeVersion, 1);
		assertEquals(cardData.osNumber, 0);
		assertEquals(cardData.osVersion, 72);
		assertEquals(cardData.pkcs1Support, 1);
		assertArrayEquals(cardData.serialNumber, serialExpected);
		assertEquals(cardData.axaltoReservedNumber, 21324);
		assertEquals(cardData.chipManufacturer, 18766);
		assertArrayEquals(cardData.chipSerialNumber, chipSerialExpected);
		assertEquals(cardData.softmaskNumber, 1);
		assertEquals(cardData.softmaskVersion, 1);
		assertTrue(cardData.isRSASSAPKCS115Supported());
		assertFalse(cardData.isRSASSAPSSSupported());
		assertFalse(cardData.isRSAESPKCS115Supported());
		assertFalse(cardData.isRSAESOAEPSupported());
		assertFalse(cardData.isRSAKEMSupported());
		assertTrue(cardData.isDeactivated());
		assertFalse(cardData.isActivated());
		assertFalse(cardData.isLocked());
	}

}
