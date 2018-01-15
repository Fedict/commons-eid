package be.fedict.commons.eid.consumer.tlv;

import org.junit.Test;

import static org.junit.Assert.*;

public class ChipNumberDataConvertorTest {

	private static final byte[] ID = { 0x01, (byte)0xa0, 0x39, 0x56};

	@Test
	public void convertsTheIdCorrectly() {
		assertEquals("01A03956", new ChipNumberDataConvertor().convert(ID));
	}

}