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

package be.bosa.commons.eid.client.impl;

import be.bosa.commons.eid.client.exception.BeIDException;
import be.bosa.commons.eid.client.spi.Logger;

import javax.smartcardio.Card;
import javax.smartcardio.CardException;
import javax.smartcardio.CardTerminal;
import javax.smartcardio.CommandAPDU;
import javax.smartcardio.ResponseAPDU;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumMap;
import java.util.Locale;

/**
 * CCID I/O according to the USB Smart card CCID 1.1 specifications.
 * FrankM added PPDU support.
 *
 * @author Frank Cornelis
 * @author Frank Marien
 */
public class CCID {

	private static final int GET_FEATURES = 0x42000D48;
	private static final int GET_FEATURES_ON_WINDOWS = 0x31 << 16 | 3400 << 2;
	private static final int MIN_PIN_SIZE = 4;
	private static final int MAX_PIN_SIZE = 12;

	private static final String DUTCH_LANGUAGE = "nl";
	private static final String FRENCH_LANGUAGE = Locale.FRENCH.getLanguage();
	private static final String GERMAN_LANGUAGE = Locale.GERMAN.getLanguage();
	private static final int DUTCH_LANGUAGE_CODE = 0x13;
	private static final int FRENCH_LANGUAGE_CODE = 0x0c;
	private static final int GERMAN_LANGUAGE_CODE = 0x07;
	private static final int ENGLISH_LANGUAGE_CODE = 0x09;

	private final Logger logger;
	private final Card card;
	private final EnumMap<FEATURE, Integer> features;
	private boolean usesPPDU;

	private static boolean riskPpdu; // only for testing

	private static final Collection<String> PPDU_NAMES = Arrays.asList(
			"Digipass 870".toLowerCase(),
			"Digipass 875".toLowerCase(),
			"Digipass 920".toLowerCase()
	);

	public enum FEATURE {
		VERIFY_PIN_START(0x01),
		VERIFY_PIN_FINISH(0x02),
		VERIFY_PIN_DIRECT(0x06),
		MODIFY_PIN_START(0x03),
		MODIFY_PIN_FINISH(0x04),
		MODIFY_PIN_DIRECT(0x07),
		GET_KEY_PRESSED(0x05),
		EID_PIN_PAD_READER(0x80);

		private final byte tag;

		FEATURE(int tag) {
			this.tag = (byte) tag;
		}

		public byte getTag() {
			return this.tag;
		}
	}

	public enum INS {
		VERIFY_PIN(0x20),
		MODIFY_PIN(0x24),
		VERIFY_PUK(0x2C);

		private final int ins;

		INS(int ins) {
			this.ins = ins;
		}

		int getIns() {
			return this.ins;
		}
	}

	public CCID(Card card, CardTerminal cardTerminal, Logger logger) {
		this.card = card;
		this.logger = logger;
		this.features = new EnumMap<>(FEATURE.class);
		this.usesPPDU = false;

		boolean onMSWindows = (System.getProperty("os.name") != null && System.getProperty("os.name").startsWith("Windows"));

		try {
			getFeaturesUsingControlChannel(card, onMSWindows);
		} catch (CardException cexInNormal) {
			this.logger.debug("GET_FEATURES over standard control command failed: " + cexInNormal.getMessage());
		}

		if (features.isEmpty()) {
			if (onMSWindows && isPPDUCardTerminal(cardTerminal.getName())) {
				this.logger.debug("Attempting To get CCID FEATURES using Pseudo-APDU Fallback Strategy");
				try {
					getFeaturesUsingPPDU(card);
				} catch (CardException cexInPseudo) {
					this.logger.error("Pseudo-APDU Fallback strategy failed as well: " + cexInPseudo.getMessage());
				}
			} else {
				this.logger.debug("Not risking PPDU Fallback strategy for CardTerminal [" + cardTerminal.getName() + "] on this platform");
			}
		}
	}

	private void getFeaturesUsingControlChannel(Card card, boolean onMSWindows) throws CardException {
		logger.debug("Getting CCID FEATURES using standard control command");
		byte[] featureBytes = card.transmitControlCommand(onMSWindows ? GET_FEATURES_ON_WINDOWS : GET_FEATURES, new byte[0]);
		logger.debug("CCID FEATURES found using standard control command");

		for (FEATURE feature : FEATURE.values()) {
			Integer featureCode = findFeatureTLV(feature.getTag(), featureBytes);
			features.put(feature, featureCode);
			if (featureCode != null) {
				logger.debug("FEATURE " + feature.name() + " = " + Integer.toHexString(featureCode));
			}
		}
	}

	private Integer findFeatureTLV(byte featureTag, byte[] features) {
		int idx = 0;
		while (idx < features.length) {
			byte tag = features[idx];
			idx += 2;
			if (featureTag == tag) {
				int feature = 0;
				for (int count = 0; count < 3; count++) {
					feature |= features[idx] & 0xff;
					idx++;
					feature <<= 8;
				}
				feature |= features[idx] & 0xff;
				return feature;
			}
			idx += 4;
		}
		return null;
	}

	private void getFeaturesUsingPPDU(Card card) throws CardException {
		ResponseAPDU responseAPDU = card.getBasicChannel().transmit(new CommandAPDU((byte) 0xff, (byte) 0xc2, 0x01, 0x00, new byte[]{}, 32));
		logger.debug("PPDU response: " + Integer.toHexString(responseAPDU.getSW()));

		if (responseAPDU.getSW() == 0x9000) {
			byte[] featureBytes = responseAPDU.getData();
			logger.debug("CCID FEATURES found using Pseudo-APDU Fallback Strategy");

			for (FEATURE feature : FEATURE.values()) {
				Integer featureCode = findFeaturePPDU(feature.getTag(), featureBytes);
				features.put(feature, featureCode);
				if (featureCode != null) {
					logger.debug("FEATURE " + feature.name() + " = " + Integer.toHexString(featureCode));
				}
			}
			usesPPDU = true;
		} else {
			logger.error("CCID Features via PPDU Not Supported");
		}
	}

	private Integer findFeaturePPDU(byte featureTag, byte[] features) {
		for (byte tag : features) {
			if (featureTag == tag)
				return (int) tag;
		}
		return null;
	}

	public boolean usesPPDU() {
		return usesPPDU;
	}

	public boolean hasFeature(FEATURE feature) {
		return getFeature(feature) != null;
	}

	public Integer getFeature(FEATURE feature) {
		return features.get(feature);
	}

	protected byte[] transmitPPDUCommand(int controlCode, byte[] command) throws CardException, BeIDException {
		ResponseAPDU responseAPDU = card.getBasicChannel().transmit(new CommandAPDU((byte) 0xff, (byte) 0xc2, 0x01, controlCode, command));

		if (responseAPDU.getSW() != 0x9000) {
			throw new BeIDException("PPDU Command Failed: ResponseAPDU=" + responseAPDU.getSW());
		}

		return responseAPDU.getData().length == 0 ? responseAPDU.getBytes() : responseAPDU.getData();
	}

	protected byte[] transmitControlCommand(int controlCode, byte[] command) throws BeIDException {
		try {
			if (usesPPDU()) return transmitPPDUCommand(controlCode, command);

			return card.transmitControlCommand(controlCode, command);
		} catch (CardException e) {
			throw new BeIDException("Error transmitting control command", e);
		}
	}

	public void waitForOK() throws BeIDException, InterruptedException {
		// wait for key pressed
		while (true) {
			byte[] keyPressedResult = transmitControlCommand(getFeature(FEATURE.GET_KEY_PRESSED), new byte[0]);
			byte key = keyPressedResult[0];
			switch (key) {
				case 0x00:
					logger.debug("waiting for CCID...");
					Thread.sleep(200);
					break;

				case 0x2b:
					logger.debug("PIN digit");
					break;

				case 0x0a:
					logger.debug("erase PIN digit");
					break;

				case 0x0d:
					logger.debug("user confirmed");
					return;

				case 0x1b:
					logger.debug("user canceled");
					// XXX: need to send the PIN finish ioctl?
					throw new SecurityException("canceled by user");

				case 0x40:
					// happens in case of a reader timeout
					logger.debug("PIN abort");
					return;

				default:
					logger.debug("CCID get key pressed result: " + key
							+ " hex: " + Integer.toHexString(key));
			}
		}
	}

	public byte getLanguageId(Locale locale) {
		String language = locale.getLanguage();

		if (DUTCH_LANGUAGE.equals(language)) {
			return DUTCH_LANGUAGE_CODE;
		}

		if (FRENCH_LANGUAGE.equals(language)) {
			return FRENCH_LANGUAGE_CODE;
		}

		if (GERMAN_LANGUAGE.equals(language)) {
			return GERMAN_LANGUAGE_CODE;
		}

		return ENGLISH_LANGUAGE_CODE;
	}

	public byte[] createPINVerificationDataStructure(Locale locale, INS ins) throws IOException {
		ByteArrayOutputStream verifyCommand = new ByteArrayOutputStream();
		verifyCommand.write(30); // bTimeOut
		verifyCommand.write(30); // bTimeOut2

		/*
		 * bmFormatString.
		 * bit 7: 1 = system units are bytes
		 * bit 6-3: 1 = PIN position in APDU command after Lc, so just after the 0x20 | pinSize.
		 * bit 2: 0 = left justify data
		 * bit 1-0: 1 = BCD
		 */
		verifyCommand.write(0b1000_1001);

		/*
		 * bmPINBlockString
		 * bit 7-4: 4 = PIN length
		 * bit 3-0: 7 = PIN block size (7 times 0xff)
		 */
		verifyCommand.write(0x47);

		/*
		 * bmPINLengthFormat. weird... the values do not make any sense to me.
		 * bit 7-5: 0 = RFU
		 * bit 4: 0 = system units are bits
		 * bit 3-0: 4 = PIN length position in APDU
		 */
		verifyCommand.write(0x04);

		/*
		 * first byte = maximum PIN size in digit
		 * second byte = minimum PIN size in digit.
		 */
		verifyCommand.write(new byte[]{(byte) MAX_PIN_SIZE, (byte) MIN_PIN_SIZE});

		/*
		 * 0x02 = validation key pressed. So the user must press the green
		 * button on his pinpad.
		 */
		verifyCommand.write(0x02);

		/*
		 * 0x01 = message with index in bMsgIndex
		 */
		verifyCommand.write(0x01);

		/*
		 * 0x04 = default sub-language
		 */
		verifyCommand.write(new byte[]{getLanguageId(locale), 0x04});

		/*
		 * 0x00 = PIN insertion prompt
		 */
		verifyCommand.write(0x00);

		/*
		 * bTeoPrologue : only significant for T=1 protocol.
		 */
		verifyCommand.write(new byte[]{0x00, 0x00, 0x00}); // bTeoPrologue

		byte[] verifyApdu = new byte[]{
				0x00, // CLA
				(byte) ins.getIns(), // INS
				0x00, // P1
				0x01, // P2
				0x08, // Lc = 8 bytes in command data
				(byte) 0x20, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
				(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,};
		verifyCommand.write(verifyApdu.length & 0xff); // ulDataLength[0]
		verifyCommand.write(0x00); // ulDataLength[1]
		verifyCommand.write(0x00); // ulDataLength[2]
		verifyCommand.write(0x00); // ulDataLength[3]
		verifyCommand.write(verifyApdu); // abData

		return verifyCommand.toByteArray();
	}

	public byte[] createPINModificationDataStructure(Locale locale, INS ins) throws IOException {
		ByteArrayOutputStream modifyCommand = new ByteArrayOutputStream();
		modifyCommand.write(30); // bTimeOut
		modifyCommand.write(30); // bTimeOut2

		/*
		 * bmFormatString.
		 * bit 7: 1 = system units are bytes
		 * bit 6-3: 1 = PIN position in APDU command after Lc, so just after the 0x20 | pinSize.
		 * bit 2: 0 = left justify data
		 * bit 1-0: 1 = BCD
		 */
		modifyCommand.write(0b1000_1001);

		/*
		 * bmPINBlockString
		 * bit 7-4: 4 = PIN length
		 * bit 3-0: 7 = PIN block size (7 times 0xff)
		 */
		modifyCommand.write(0x47); // bmPINBlockString

		/*
		 * bmPINLengthFormat. weird... the values do not make any sense to me.
		 * bit 7-5: 0 = RFU
		 * bit 4: 0 = system units are bits
		 * bit 3-0: 4 = PIN length position in APDU
		 */
		modifyCommand.write(0x04);

		/*
		 * bInsertionOffsetOld: Insertion position offset in bytes for the current PIN
		 */
		modifyCommand.write(0x00);

		/*
		 * bInsertionOffsetNew: Insertion position offset in bytes for the new PIN
		 */
		modifyCommand.write(0x8);

		/*
		 * first byte = maximum PIN size in digit
		 * second byte = minimum PIN size in digit.
		 */
		modifyCommand.write(new byte[]{(byte) MAX_PIN_SIZE, (byte) MIN_PIN_SIZE});

		/*
		 * bConfirmPIN: Flags governing need for confirmation of new PIN
		 */
		modifyCommand.write(0x03);

		/*
		 * 0x02 = validation key pressed. So the user must press the green button on his pinpad.
		 */
		modifyCommand.write(0x02);

		/*
		 * 0x03 = message with index in bMsgIndex
		 */
		modifyCommand.write(0x03);

		/*
		 * 0x04 = default sub-language
		 */
		modifyCommand.write(new byte[]{getLanguageId(locale), 0x04});

		/*
		 * 0x00 = PIN insertion prompt
		 */
		modifyCommand.write(0x00);

		/*
		 * 0x01 = new PIN prompt
		 */
		modifyCommand.write(0x01);

		/*
		 * 0x02 = new PIN again prompt
		 */
		modifyCommand.write(0x02);

		/*
		 * bTeoPrologue : only significant for T=1 protocol.
		 */
		modifyCommand.write(new byte[]{0x00, 0x00, 0x00}); // bTeoPrologue

		byte[] modifyApdu = new byte[]{
				0x00, // CLA
				(byte) ins.getIns(), // INS
				0x00, // P1
				0x01, // P2
				0x10, // Lc = 16 bytes in command data
				(byte) 0x20, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
				(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
				(byte) 0x20, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
				(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,};
		modifyCommand.write(modifyApdu.length & 0xff); // ulDataLength[0]
		modifyCommand.write(0x00); // ulDataLength[1]
		modifyCommand.write(0x00); // ulDataLength[2]
		modifyCommand.write(0x00); // ulDataLength[3]
		modifyCommand.write(modifyApdu); // abData
		return modifyCommand.toByteArray();
	}

	/**
	 * Forces the use of ppdu.
	 * Only use this for testing!
	 */
	public static void setRiskPPDU(boolean riskPpdu) {
		CCID.riskPpdu = riskPpdu;
	}

	private boolean isPPDUCardTerminal(String name) {
		return riskPpdu || PPDU_NAMES.stream().anyMatch(ppduName -> name.toLowerCase().contains(ppduName));
	}

}
