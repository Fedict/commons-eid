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

package be.bosa.commons.eid.client;

import be.bosa.commons.eid.client.event.BeIDCardListener;
import be.bosa.commons.eid.client.impl.BeIDDigest;
import be.bosa.commons.eid.client.impl.CCID;
import be.bosa.commons.eid.client.impl.LocaleManager;
import be.bosa.commons.eid.client.impl.VoidLogger;
import be.bosa.commons.eid.client.spi.BeIDCardUI;
import be.bosa.commons.eid.client.spi.Logger;
import be.bosa.commons.eid.client.spi.UserCancelledException;

import javax.smartcardio.ATR;
import javax.smartcardio.Card;
import javax.smartcardio.CardChannel;
import javax.smartcardio.CardException;
import javax.smartcardio.CardTerminal;
import javax.smartcardio.CommandAPDU;
import javax.smartcardio.ResponseAPDU;
import java.awt.*;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

/**
 * One BeIDCard instance represents one Belgian Electronic Identity Card,
 * physically present in a connected javax.smartcardio.CardTerminal. It exposes
 * the publicly accessible features of the BELPIC applet on the card's chip:
 * <ul>
 * <li>Reading Certificates and Certificate Chains
 * <li>Signing of digests non-repudiation and authentication purposes
 * <li>Verification and Alteration of the PIN code
 * <li>Reading random bytes from the on-board random generator
 * <li>Creating text message transaction signatures on specialized readers
 * <li>PIN unblocking using PUK codes
 * </ul>
 * <p>
 * BeIDCard instances rely on an instance of BeIDCardUI to support user
 * interaction, such as obtaining PIN and PUK codes for authentication, signing,
 * verifying, changing PIN codes, and for notifying the user of the progress of
 * such operations on a Secure Pinpad Device. A default implementation is
 * available as DefaultBeIDCardUI, and unless replaced by an explicit call to
 * setUI() will automatically be used (when present in the class path).
 * <p>
 * BeIDCard instances automatically detect CCID features in the underlying
 * CardTerminal, and will choose the most secure path where several are
 * available, for example, when needing to acquire PIN codes from the user, and
 * the card is in a CCID-compliant Secure Pinpad Reader the PIN entry features
 * of the reader will be used instead of the corresponding "obtain.." feature
 * from the active BeIDCardUI. In that case, the corresponding "advise.." method
 * of the active BeIDCardUI will be called instead, to advise the user to attend
 * to the SPR.
 * <p>
 * To receive notifications of the progress of lengthy operations such as
 * reading 'files' (certificates, photo,..) or signing (which may be lengthy
 * because of user PIN interaction), register an instance of BeIDCardListener
 * using addCardListener(). This is useful, for example, for providing progress
 * indication to the user.
 * <p>
 * For detailed progress and error/debug logging, provide an instance of
 * be.bosa.commons.eid.spi.Logger to BeIDCard's constructor (the default
 * VoidLogger discards all logging and debug messages). You are advised to
 * provide some form of logging facility, for all but the most trivial
 * applications.
 *
 * @author Frank Cornelis
 * @author Frank Marien
 */
public class BeIDCard implements AutoCloseable {

	private static final String UI_MISSING_LOG_MESSAGE = "No BeIDCardUI set and can't load DefaultBeIDCardUI";
	private static final String UI_DEFAULT_REQUIRES_HEAD = "No BeIDCardUI set and DefaultBeIDCardUI requires a graphical environment";
	private static final String DEFAULT_UI_IMPLEMENTATION = "be.bosa.commons.eid.dialogs.DefaultBeIDCardUI";

	private static final byte[] BELPIC_AID = new byte[]{(byte) 0xA0, 0x00, 0x00, 0x01, 0x77, 0x50, 0x4B, 0x43, 0x53, 0x2D, 0x31, 0x35,};
	private static final byte[] APPLET_AID = new byte[]{(byte) 0xA0, 0x00, 0x00, 0x00, 0x30, 0x29, 0x05, 0x70, 0x00, (byte) 0xAD, 0x13, 0x10, 0x01, 0x01, (byte) 0xFF,};
	private static final int BLOCK_SIZE = 0xff;

	private final CardChannel cardChannel;
	private final List<BeIDCardListener> cardListeners;
	private final CertificateFactory certificateFactory;

	private final Card card;
	private final Logger logger;

	private CCID ccid;
	private BeIDCardUI ui;
	private CardTerminal cardTerminal;
	private Locale locale;

	/**
	 * Instantiate a BeIDCard from an already connected javax.smartcardio.Card,
	 * with a Logger implementation to receive logging output.
	 *
	 * @param card   a javax.smartcardio.Card that you have previously determined
	 *               to be a BeID Card
	 * @param logger the logger instance
	 * @throws IllegalArgumentException when passed a null logger. to disable logging, call
	 *                                  BeIDCard(Card) instead.
	 * @throws RuntimeException         when no CertificateFactory capable of producing X509
	 *                                  Certificates is available.
	 */
	public BeIDCard(Card card, Logger logger) {
		if (logger == null) {
			throw new IllegalArgumentException("logger expected");
		}

		this.card = card;
		this.cardChannel = card.getBasicChannel();
		this.logger = logger;
		this.cardListeners = new LinkedList<>();

		try {
			this.certificateFactory = CertificateFactory.getInstance("X.509");
		} catch (CertificateException e) {
			throw new RuntimeException("X.509 algo", e);
		}
	}

	/**
	 * Instantiate a BeIDCard from an already connected javax.smartcardio.Card
	 * no logging information will be available.
	 *
	 * @param card a javax.smartcardio.Card that you have previously determined
	 *             to be a BeID Card
	 * @throws RuntimeException when no CertificateFactory capable of producing X509
	 *                          Certificates is available.
	 */
	public BeIDCard(Card card) {
		this(card, new VoidLogger());
	}

	/**
	 * Instantiate a BeIDCard from a javax.smartcardio.CardTerminal, with a
	 * Logger implementation to receive logging output.
	 *
	 * @param cardTerminal a javax.smartcardio.CardTerminal that you have previously
	 *                     determined to contain a BeID Card
	 * @param logger       the logger instance
	 * @throws IllegalArgumentException when passed a null logger. to disable logging, call public
	 *                                  BeIDCard(CardTerminal) instead.
	 * @throws RuntimeException         when no CertificateFactory capable of producing X509
	 *                                  Certificates is available.
	 */
	public BeIDCard(CardTerminal cardTerminal, Logger logger) throws CardException {
		this(cardTerminal.connect("T=0"), logger);
	}

	/**
	 * close this BeIDCard, when you are done with it, to release any underlying
	 * resources. All subsequent calls will fail.
	 */
	public void close() {
		logger.debug("closing eID card");
		setCardTerminal(null);

		try {
			card.disconnect(true);
		} catch (CardException e) {
			logger.error("could not disconnect the card: " + e.getMessage());
		}
	}

	/**
	 * Explicitly set the User Interface to be used for consequent operations.
	 * All user interaction is handled through this, and possible SPR features
	 * of CCID-capable CardReaders. This will also modify the Locale setting of
	 * this beIDCard instance to match the UI's Locale, so the language in any
	 * SPR messages displayed will be consistent with the UI's language.
	 *
	 * @param userInterface an instance of BeIDCardUI
	 * @return this BeIDCard instance, to allow method chaining
	 */
	public BeIDCard setUI(BeIDCardUI userInterface) {
		ui = userInterface;
		if (locale == null) {
			setLocale(userInterface.getLocale());
		}
		return this;
	}

	/**
	 * Register a BeIDCardListener to receive updates on any consequent file
	 * reading/signature operations executed by this BeIDCard.
	 *
	 * @param beIDCardListener a beIDCardListener instance
	 * @return this BeIDCard instance, to allow method chaining
	 */
	public BeIDCard addCardListener(BeIDCardListener beIDCardListener) {
		synchronized (cardListeners) {
			cardListeners.add(beIDCardListener);
		}

		return this;
	}

	/**
	 * Unregister a BeIDCardListener to no longer receive updates on any
	 * consequent file reading/signature operations executed by this BeIDCard.
	 *
	 * @param beIDCardListener a beIDCardListener instance
	 * @return this BeIDCard instance, to allow method chaining
	 */
	public BeIDCard removeCardListener(BeIDCardListener beIDCardListener) {
		synchronized (cardListeners) {
			cardListeners.remove(beIDCardListener);
		}

		return this;
	}

	/**
	 * Reads a certain certificate from the card. Which certificate to read is
	 * determined by the FileType param. Applicable FileTypes are
	 * AuthentificationCertificate, NonRepudiationCertificate, CACertificate,
	 * RootCertificate and RRNCertificate.
	 *
	 * @return the certificate requested
	 */
	public X509Certificate getCertificate(FileType fileType) throws CertificateException, CardException, IOException, InterruptedException {
		return generateCertificateOfType(fileType);
	}

	/**
	 * Returns the X509 authentication certificate. This is a convenience method
	 * for <code>getCertificate(FileType.AuthentificationCertificate)</code>
	 *
	 * @return the X509 Authentication Certificate from the card.
	 */
	public X509Certificate getAuthenticationCertificate() throws CardException, IOException, CertificateException, InterruptedException {
		return getCertificate(FileType.AuthentificationCertificate);
	}

	/**
	 * Returns the X509 non-repudiation certificate. This is a convencience
	 * method for
	 * <code>getCertificate(FileType.NonRepudiationCertificate)</code>
	 */
	public X509Certificate getSigningCertificate() throws CardException, IOException, CertificateException, InterruptedException {
		return getCertificate(FileType.NonRepudiationCertificate);
	}

	/**
	 * Returns the citizen CA certificate. This is a convenience method for
	 * <code>getCertificate(FileType.CACertificate)</code>
	 */
	public X509Certificate getCACertificate() throws CardException, IOException, CertificateException, InterruptedException {
		return getCertificate(FileType.CACertificate);
	}

	/**
	 * Returns the Root CA certificate.
	 *
	 * @return the Root CA X509 certificate.
	 */
	public X509Certificate getRootCACertificate() throws CertificateException, CardException, IOException, InterruptedException {
		return getCertificate(FileType.RootCertificate);
	}

	/**
	 * Returns the national registration certificate. This is a convencience
	 * method for <code>getCertificate(FileType.RRNCertificate)</code>
	 */
	public X509Certificate getRRNCertificate() throws CardException, IOException, CertificateException, InterruptedException {
		return getCertificate(FileType.RRNCertificate);
	}

	/**
	 * Returns the entire certificate chain for a given file type. Of course,
	 * only file types corresponding with a certificate are accepted. Which
	 * certificate's chain to return is determined by the FileType param.
	 * Applicable FileTypes are AuthentificationCertificate,
	 * NonRepudiationCertificate, CACertificate, and RRNCertificate.
	 *
	 * @param fileType which certificate's chain to return
	 * @return the certificate's chain up to and including the Belgian Root Cert
	 */
	public List<X509Certificate> getCertificateChain(FileType fileType) throws CertificateException, CardException, IOException, InterruptedException {
		List<X509Certificate> chain = new LinkedList<>();
		chain.add(generateCertificateOfType(fileType));
		if (fileType.chainIncludesCitizenCA()) {
			chain.add(generateCertificateOfType(FileType.CACertificate));
		}
		chain.add(generateCertificateOfType(FileType.RootCertificate));

		return chain;
	}

	/**
	 * Returns the X509 authentication certificate chain. (Authentication -
	 * Citizen CA - Root) This is a convenience method for
	 * <code>getCertificateChain(FileType.AuthentificationCertificate)</code>
	 *
	 * @return the authentication certificate chain
	 */
	public List<X509Certificate> getAuthenticationCertificateChain() throws CardException, IOException, CertificateException, InterruptedException {
		return getCertificateChain(FileType.AuthentificationCertificate);
	}

	/**
	 * Returns the X509 non-repudiation certificate chain. (Non-Repudiation -
	 * Citizen CA - Root) This is a convenience method for
	 * <code>getCertificateChain(FileType.NonRepudiationCertificate)</code>
	 *
	 * @return the non-repudiation certificate chain
	 */
	public List<X509Certificate> getSigningCertificateChain() throws CardException, IOException, CertificateException, InterruptedException {
		return getCertificateChain(FileType.NonRepudiationCertificate);
	}

	/**
	 * Returns the Citizen CA X509 certificate chain. (Citizen CA - Root) This
	 * is a convenience method for
	 * <code>getCertificateChain(FileType.CACertificate)</code>
	 *
	 * @return the citizen ca certificate chain
	 */
	public List<X509Certificate> getCACertificateChain() throws CardException, IOException, CertificateException, InterruptedException {
		return getCertificateChain(FileType.CACertificate);
	}

	/**
	 * Returns the national registry X509 certificate chain. (National Registry
	 * - Root) This is a convenience method for
	 * <code>getCertificateChain(FileType.RRNCertificate)</code>
	 *
	 * @return the national registry certificate chain
	 */
	public List<X509Certificate> getRRNCertificateChain() throws CardException, IOException, CertificateException, InterruptedException {
		return getCertificateChain(FileType.RRNCertificate);
	}

	/**
	 * Sign a given digest value.
	 *
	 * @param digestValue         the digest value to be signed.
	 * @param digestAlgo          the algorithm used to calculate the given digest value.
	 * @param fileType            the certificate's file type.
	 * @param requireSecureReader <code>true</code> if a secure pinpad reader is required.
	 */
	public byte[] sign(byte[] digestValue, BeIDDigest digestAlgo, FileType fileType, boolean requireSecureReader)
			throws CardException, IOException, InterruptedException, UserCancelledException {
		if (!fileType.isCertificateUserCanSignWith()) {
			throw new IllegalArgumentException("Not a certificate that can be used for signing: " + fileType.name());
		}

		if (getCCID().hasFeature(CCID.FEATURE.EID_PIN_PAD_READER)) {
			logger.debug("eID-aware secure PIN pad reader detected");
		}

		if (requireSecureReader
				&& (!getCCID().hasFeature(CCID.FEATURE.VERIFY_PIN_DIRECT))
				&& (getCCID().hasFeature(CCID.FEATURE.VERIFY_PIN_START))) {
			throw new SecurityException("not a secure reader");
		}

		beginExclusive();
		notifySigningBegin(fileType);

		try {
			// select the key
			logger.debug("selecting key...");

			ResponseAPDU responseApdu = transmitCommand(
					BeIDCommandAPDU.SELECT_ALGORITHM_AND_PRIVATE_KEY,
					new byte[]{(byte) 0x04, // length of following data
							(byte) 0x80, digestAlgo.getAlgorithmReference(), // algorithm reference
							(byte) 0x84, fileType.getKeyId(),}); // private key reference

			if (0x9000 != responseApdu.getSW()) {
				throw new ResponseAPDUException("SET (select algorithm and private key) error", responseApdu);
			}

			if (FileType.NonRepudiationCertificate.getKeyId() == fileType.getKeyId()) {
				logger.debug("non-repudiation key detected, immediate PIN verify");
				verifyPin(PINPurpose.NonRepudiationSignature);
			}

			ByteArrayOutputStream digestInfo = new ByteArrayOutputStream();
			digestInfo.write(digestAlgo.getPrefix(digestValue.length));
			digestInfo.write(digestValue);

			logger.debug("computing digital signature...");
			responseApdu = transmitCommand(BeIDCommandAPDU.COMPUTE_DIGITAL_SIGNATURE, digestInfo.toByteArray());
			if (0x9000 == responseApdu.getSW()) {
				/*
				 * OK, we could use the card PIN caching feature.
				 *
				 * Notice that the card PIN caching also works when first doing
				 * an authentication after a non-repudiation signature.
				 */
				return responseApdu.getData();
			}
			if (0x6982 != responseApdu.getSW()) {
				logger.debug("SW: " + Integer.toHexString(responseApdu.getSW()));
				throw new ResponseAPDUException("compute digital signature error", responseApdu);
			}
			/*
			 * 0x6982 = Security status not satisfied, so we do a PIN
			 * verification before retrying.
			 */
			logger.debug("PIN verification required...");
			verifyPin(PINPurpose.fromFileType(fileType));

			logger.debug("computing digital signature (attempt #2 after PIN verification)...");
			responseApdu = transmitCommand(BeIDCommandAPDU.COMPUTE_DIGITAL_SIGNATURE, digestInfo.toByteArray());
			if (0x9000 != responseApdu.getSW()) {
				throw new ResponseAPDUException("compute digital signature error", responseApdu);
			}

			return responseApdu.getData();
		} finally {
			endExclusive();
			notifySigningEnd(fileType);
		}
	}

	/**
	 * Create an authentication signature.
	 *
	 * @param toBeSigned          the data to be signed
	 * @param requireSecureReader whether to require a secure pinpad reader to obtain the
	 *                            citizen's PIN if false, the current BeIDCardUI will be used in
	 *                            the absence of a secure pinpad reader. If true, an exception
	 *                            will be thrown unless a SPR is available
	 * @return a SHA-256 digest of the input data signed by the citizen's authentication key
	 */
	public byte[] signAuthn(byte[] toBeSigned, boolean requireSecureReader)
			throws NoSuchAlgorithmException, CardException, IOException, InterruptedException, UserCancelledException {
		MessageDigest messageDigest = BeIDDigest.SHA_256.getMessageDigestInstance();
		byte[] digest = messageDigest.digest(toBeSigned);
		return sign(digest, BeIDDigest.SHA_256, FileType.AuthentificationCertificate, requireSecureReader);
	}

	/**
	 * Verifying PIN Code (without other actions, for testing PIN), using the
	 * most secure method available. Note that this still has the side effect of
	 * loading a successfully tests PIN into the PIN cache, so that unless the
	 * card is removed, a subsequent authentication attempt will not request the
	 * PIN, but proceed with the PIN given here.
	 */
	public void verifyPin() throws IOException, CardException, InterruptedException, UserCancelledException {
		verifyPin(PINPurpose.PINTest);
	}

	/**
	 * Change PIN code. This method will attempt to change PIN using the most
	 * secure method available. if requiresSecureReader is true, this will throw
	 * a SecurityException if no SPR is available, otherwise, this will default
	 * to changing the PIN via the UI
	 */
	public void changePin(boolean requireSecureReader) throws CardException, InterruptedException, IOException {
		if (requireSecureReader
				&& (!getCCID().hasFeature(CCID.FEATURE.MODIFY_PIN_DIRECT))
				&& (!getCCID().hasFeature(CCID.FEATURE.MODIFY_PIN_START))) {
			throw new SecurityException("not a secure reader");
		}

		int retriesLeft = -1;
		ResponseAPDU responseApdu;
		do {
			if (getCCID().hasFeature(CCID.FEATURE.MODIFY_PIN_START)) {
				logger.debug("using modify pin start/finish...");
				responseApdu = changePINViaCCIDStartFinish(retriesLeft);
			} else if (getCCID().hasFeature(CCID.FEATURE.MODIFY_PIN_DIRECT)) {
				logger.debug("could use direct PIN modify here...");
				responseApdu = changePINViaCCIDDirect(retriesLeft);
			} else {
				responseApdu = changePINViaUI(retriesLeft);
			}

			if (0x9000 != responseApdu.getSW()) {
				logger.debug("CHANGE PIN error");
				logger.debug("SW: " + Integer.toHexString(responseApdu.getSW()));
				if (0x6983 == responseApdu.getSW()) {
					getUI().advisePINBlocked();
					throw new ResponseAPDUException("eID card blocked!", responseApdu);
				}
				if (0x63 != responseApdu.getSW1()) {
					logger.debug("PIN change error. Card blocked?");
					throw new ResponseAPDUException("PIN Change Error", responseApdu);
				}
				retriesLeft = responseApdu.getSW2() & 0xf;
				logger.debug("retries left: " + retriesLeft);
			}
		} while (0x9000 != responseApdu.getSW());

		getUI().advisePINChanged();
	}

	/**
	 * Returns random data generated by the eID card itself.
	 *
	 * @param size the size of the requested random data.
	 * @return size bytes of random data
	 */
	public byte[] getChallenge(int size) throws CardException {
		ResponseAPDU responseApdu = transmitCommand(BeIDCommandAPDU.GET_CHALLENGE, new byte[]{}, 0, 0, size);
		if (0x9000 != responseApdu.getSW()) {
			logger.debug("get challenge failure: " + Integer.toHexString(responseApdu.getSW()));
			throw new ResponseAPDUException("get challenge failure: " + Integer.toHexString(responseApdu.getSW()), responseApdu);
		}
		if (size != responseApdu.getData().length) {
			throw new RuntimeException("challenge size incorrect: " + responseApdu.getData().length);
		}

		return responseApdu.getData();
	}

	/**
	 * Create a text message transaction signature. The BOSA eID aware secure
	 * pinpad readers can visualize such type of text message transactions on
	 * their hardware display.
	 *
	 * @param transactionMessage the transaction message to be signed.
	 */
	public byte[] signTransactionMessage(String transactionMessage, boolean requireSecureReader)
			throws CardException, IOException, InterruptedException, UserCancelledException {
		if (getCCID().hasFeature(CCID.FEATURE.EID_PIN_PAD_READER)) {
			getUI().adviseSecureReaderOperation();
		}

		byte[] signature;
		try {
			signature = sign(
					transactionMessage.getBytes(), BeIDDigest.PLAIN_TEXT,
					FileType.AuthentificationCertificate, requireSecureReader
			);
		} finally {
			if (getCCID().hasFeature(CCID.FEATURE.EID_PIN_PAD_READER)) {
				getUI().adviseSecureReaderOperationEnd();
			}
		}
		return signature;
	}

	/**
	 * Discard the citizen's PIN code from the PIN cache. Any subsequent
	 * Authentication signatures will require PIN entry. (non-repudation
	 * signatures are automatically protected)
	 *
	 * @return this BeIDCard instance, to allow method chaining
	 */
	public BeIDCard logoff() throws CardException {
		CommandAPDU logoffApdu = new CommandAPDU(0x80, 0xE6, 0x00, 0x00);
		logger.debug("logoff...");
		ResponseAPDU responseApdu = transmit(logoffApdu);
		if (0x9000 != responseApdu.getSW()) {
			throw new RuntimeException("logoff failed");
		}

		return this;
	}

	/**
	 * Unblocking PIN using PUKs. This will choose the most secure method
	 * available to unblock a blocked PIN. If requireSecureReader is true, will
	 * throw SecurityException if an SPR is not available
	 */
	public void unblockPin(boolean requireSecureReader) throws Exception {
		if (requireSecureReader && (!getCCID().hasFeature(CCID.FEATURE.VERIFY_PIN_DIRECT))) {
			throw new SecurityException("not a secure reader");
		}

		ResponseAPDU responseApdu;
		int retriesLeft = -1;
		do {
			if (getCCID().hasFeature(CCID.FEATURE.VERIFY_PIN_DIRECT)) {
				logger.debug("could use direct PIN verify here...");
				responseApdu = unblockPINViaCCIDVerifyPINDirectOfPUK(retriesLeft);
			} else {
				responseApdu = unblockPINViaUI(retriesLeft);
			}

			if (0x9000 != responseApdu.getSW()) {
				logger.debug("PIN unblock error");
				logger.debug("SW: " + Integer.toHexString(responseApdu.getSW()));
				if (0x6983 == responseApdu.getSW()) {
					getUI().advisePINBlocked();
					throw new ResponseAPDUException("eID card blocked!", responseApdu);
				}
				if (0x63 != responseApdu.getSW1()) {
					logger.debug("PIN unblock error.");
					throw new ResponseAPDUException("PIN unblock error", responseApdu);
				}
				retriesLeft = responseApdu.getSW2() & 0xf;
				logger.debug("retries left: " + retriesLeft);
			}
		} while (0x9000 != responseApdu.getSW());

		getUI().advisePINUnblocked();
	}

	/**
	 * getATR returns the ATR of the eID Card. If this BeIDCard instance was
	 * constructed using the CardReader constructor, this is the only way to get
	 * to the ATR.
	 */
	public ATR getATR() {
		return card.getATR();
	}

	/**
	 * @return the current Locale used in CCID SPR operations and UI
	 */
	public Locale getLocale() {
		if (locale != null) {
			return locale;
		}
		return LocaleManager.getLocale();
	}

	/**
	 * set the Locale to use for subsequent UI and CCID operations. this will
	 * modify the Locale of any explicitly set UI, as well. BeIDCard instances,
	 * while using the global Locale settings made in BeIDCards and/or
	 * BeIDCardManager by default, may have their own individual Locale settings
	 * that may override those global settings.
	 *
	 * @return this BeIDCard instance, to allow method chaining
	 */
	public BeIDCard setLocale(Locale newLocale) {
		locale = newLocale;
		if (locale != null && ui != null) {
			ui.setLocale(locale);
		}
		return this;
	}

	// ===========================================================================================================
	// low-level card operations
	// not recommended for general use.
	// if you find yourself having to call these, we'd very much like to hear
	// about it.
	// ===========================================================================================================

	/**
	 * Select the BELPIC applet on the chip. Since the BELPIC applet is supposed
	 * to be all alone on the chip, shouldn't be necessary.
	 *
	 * @return this BeIDCard instance, to allow method chaining
	 */
	public BeIDCard selectApplet() throws CardException {
		ResponseAPDU responseApdu;

		responseApdu = transmitCommand(BeIDCommandAPDU.SELECT_APPLET_0, BELPIC_AID);
		if (0x9000 != responseApdu.getSW()) {
			logger.error("error selecting BELPIC");
			logger.debug("status word: "
					+ Integer.toHexString(responseApdu.getSW()));
			/*
			 * Try to select the Applet.
			 */
			try {
				responseApdu = transmitCommand(BeIDCommandAPDU.SELECT_APPLET_1, APPLET_AID);
			} catch (CardException e) {
				logger.error("error selecting Applet");
				return this;
			}
			if (0x9000 != responseApdu.getSW()) {
				logger.error("could not select applet");
			} else {
				logger.debug("BELPIC JavaCard applet selected by APPLET_AID");
			}
		} else {
			logger.debug("BELPIC JavaCard applet selected by BELPIC_AID");
		}

		return this;
	}

	/**
	 * Begin an exclusive transaction with the card. Once this returns, only the
	 * calling thread will be able to access the card, until it calls
	 * endExclusive(). Other threads will receive a CardException. Use this when
	 * you need to make several calls to the card that depend on each other. for
	 * example, SELECT FILE and READ BINARY, or SELECT ALGORITHM and COMPUTE
	 * SIGNATURE, to avoid other threads/processes from interleaving commands
	 * that would break your transactional logic.
	 * <p>
	 * Called automatically by the higher-level methods in this class. If you
	 * end up calling this directly, this is either something wrong with your
	 * code, or with this class. Please let us know. You should really only have
	 * to be calling this when using some of the other low-level methods
	 * (transmitCommand, etc..) *never* in combination with the high-level
	 * methods.
	 *
	 * @return this BeIDCard Instance, to allow method chaining.
	 */
	public BeIDCard beginExclusive() throws CardException {
		logger.debug("---begin exclusive---");
		card.beginExclusive();
		return this;
	}

	/**
	 * Release an exclusive transaction with the card, started by
	 * beginExclusive().
	 *
	 * @return this BeIDCard Instance, to allow method chaining.
	 */
	public BeIDCard endExclusive() throws CardException {
		logger.debug("---end exclusive---");
		card.endExclusive();
		return this;
	}

	/**
	 * Read bytes from a previously selected "File" on the card. should be
	 * preceded by a call to selectFile so the card knows what you want to read.
	 * Consider using one of the higher-level methods, or readFile().
	 *
	 * @param fileType         the file to read (to allow for notification)
	 * @param estimatedMaxSize the estimated total size of the file to read (to allow for
	 *                         notification)
	 * @return the data from the file
	 */
	public byte[] readBinary(FileType fileType, int estimatedMaxSize) throws CardException, IOException, InterruptedException {
		int offset = 0;
		logger.debug("read binary");
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		byte[] data;
		do {
			if (Thread.currentThread().isInterrupted()) {
				logger.debug("interrupted in readBinary");
				throw new InterruptedException();
			}

			notifyReadProgress(fileType, offset, estimatedMaxSize);
			ResponseAPDU responseApdu = transmitCommand(
					BeIDCommandAPDU.READ_BINARY, offset >> 8, offset & 0xFF,
					BLOCK_SIZE);
			int sw = responseApdu.getSW();
			if (0x6B00 == sw) {
				/*
				 * Wrong parameters (offset outside the EF) End of file reached.
				 * Can happen in case the file size is a multiple of 0xff bytes.
				 */
				break;
			}

			if (0x9000 != sw) {
				throw new IOException("BeIDCommandAPDU response error: " + responseApdu.getSW(),
						new ResponseAPDUException(responseApdu));
			}

			data = responseApdu.getData();
			baos.write(data);
			offset += data.length;
		} while (BLOCK_SIZE == data.length);

		notifyReadProgress(fileType, offset, offset);
		return baos.toByteArray();
	}

	/**
	 * Selects a file to read on the card
	 *
	 * @param fileId the file to read
	 * @return this BeIDCard Instance, to allow method chaining.
	 */
	public BeIDCard selectFile(byte[] fileId) throws CardException, FileNotFoundException {
		logger.debug("selecting file");

		ResponseAPDU responseApdu = transmitCommand(BeIDCommandAPDU.SELECT_FILE, fileId);
		if (0x9000 != responseApdu.getSW()) {
			throw new FileNotFoundException("wrong status word after selecting file: " + Integer.toHexString(responseApdu.getSW()));
		}

		try {
			// SCARD_E_SHARING_VIOLATION fix
			Thread.sleep(20);
		} catch (InterruptedException e) {
			throw new RuntimeException("sleep error: " + e.getMessage());
		}

		return this;
	}

	/**
	 * Reads a file and converts it to a certificagte.
	 */
	private X509Certificate generateCertificateOfType(FileType fileType) throws CertificateException, CardException, IOException, InterruptedException {
		return (X509Certificate) certificateFactory.generateCertificate(new ByteArrayInputStream(readFile(fileType)));
	}

	/**
	 * Reads a file from the card.
	 *
	 * @param fileType the file to read
	 * @return the data from the file
	 */
	public byte[] readFile(FileType fileType) throws CardException, IOException, InterruptedException {
		beginExclusive();

		try {
			selectFile(fileType.getFileId());
			return readBinary(fileType, fileType.getEstimatedMaxSize());
		} finally {
			endExclusive();
		}
	}

	/**
	 * test for CCID Features in the card reader this BeIDCard is inserted into
	 *
	 * @param feature the feature to test for (CCID.FEATURE)
	 * @return true if the given feature is available, false if not
	 */
	public boolean cardTerminalHasCCIDFeature(CCID.FEATURE feature) {
		return getCCID().hasFeature(feature);
	}

	// ===========================================================================================================
	// low-level card transmit commands
	// not recommended for general use.
	// if you find yourself having to call these, we'd very much like to hear
	// about it.
	// ===========================================================================================================

	private byte[] transmitCCIDControl(boolean usePPDU, CCID.FEATURE feature) throws CardException {
		return transmitControlCommand(getCCID().getFeature(feature), new byte[0]);
	}

	private byte[] transmitCCIDControl(boolean usePPDU, CCID.FEATURE feature, byte[] command) throws CardException {
		return usePPDU
				? transmitPPDUCommand(feature.getTag(), command)
				: transmitControlCommand(getCCID().getFeature(feature), command);
	}

	private byte[] transmitControlCommand(int controlCode, byte[] command) throws CardException {
		return card.transmitControlCommand(controlCode, command);
	}

	private byte[] transmitPPDUCommand(int controlCode, byte[] command) throws CardException {
		ResponseAPDU responseAPDU = transmitCommand(BeIDCommandAPDU.PPDU, controlCode, command);
		if (responseAPDU.getSW() != 0x9000)
			throw new CardException("PPDU Command Failed: ResponseAPDU="
					+ responseAPDU.getSW());
		if (responseAPDU.getNr() == 0)
			return responseAPDU.getBytes();
		return responseAPDU.getData();
	}

	private ResponseAPDU transmitCommand(BeIDCommandAPDU apdu, int le) throws CardException {
		return transmit(new CommandAPDU(apdu.getCla(), apdu.getIns(), apdu.getP1(), apdu.getP2(), le));
	}

	private ResponseAPDU transmitCommand(BeIDCommandAPDU apdu, int p2, byte[] data) throws CardException {
		return transmit(new CommandAPDU(apdu.getCla(), apdu.getIns(),
				apdu.getP1(), p2, data));
	}

	private ResponseAPDU transmitCommand(BeIDCommandAPDU apdu, int p1, int p2, int le) throws CardException {
		return transmit(new CommandAPDU(apdu.getCla(), apdu.getIns(), p1, p2, le));
	}

	private ResponseAPDU transmitCommand(BeIDCommandAPDU apdu, byte[] data) throws CardException {
		return transmit(new CommandAPDU(apdu.getCla(), apdu.getIns(), apdu.getP1(), apdu.getP2(), data));
	}

	private ResponseAPDU transmitCommand(BeIDCommandAPDU apdu, byte[] data, int dataOffset, int dataLength, int ne) throws CardException {
		return transmit(new CommandAPDU(apdu.getCla(), apdu.getIns(), apdu.getP1(), apdu.getP2(), data, dataOffset, dataLength, ne));
	}

	private ResponseAPDU transmit(CommandAPDU commandApdu) throws CardException {
		ResponseAPDU responseApdu = cardChannel.transmit(commandApdu);
		if (0x6c == responseApdu.getSW1()) {
			/*
			 * A minimum delay of 10 msec between the answer ?????????6C
			 * xx????????? and the next BeIDCommandAPDU is mandatory for eID
			 * v1.0 and v1.1 cards.
			 */
			logger.debug("sleeping...");
			try {
				Thread.sleep(10);
			} catch (InterruptedException e) {
				throw new RuntimeException("cannot sleep");
			}
			responseApdu = cardChannel.transmit(commandApdu);
		}

		return responseApdu;
	}

	// ===========================================================================================================
	// notifications of listeners
	// ===========================================================================================================

	private void notifyReadProgress(FileType fileType, int offset, int estimatedMaxOffset) {
		if (offset > estimatedMaxOffset) {
			estimatedMaxOffset = offset;
		}

		synchronized (cardListeners) {
			for (BeIDCardListener listener : cardListeners) {
				try {
					listener.notifyReadProgress(fileType, offset, estimatedMaxOffset);
				} catch (Exception ex) {
					logger.debug("Exception Thrown In BeIDCardListener.notifyReadProgress():" + ex.getMessage());
				}
			}
		}
	}

	private void notifySigningBegin(FileType keyType) {
		synchronized (cardListeners) {
			for (BeIDCardListener listener : cardListeners) {
				try {
					listener.notifySigningBegin(keyType);
				} catch (Exception ex) {
					logger.debug("Exception Thrown In BeIDCardListener.notifySigningBegin():" + ex.getMessage());
				}
			}
		}
	}

	private void notifySigningEnd(FileType keyType) {
		synchronized (cardListeners) {
			for (BeIDCardListener listener : cardListeners) {
				try {
					listener.notifySigningEnd(keyType);
				} catch (Exception ex) {
					logger.debug("Exception Thrown In BeIDCardListener.notifySigningBegin():" + ex.getMessage());
				}
			}
		}
	}

	// ===========================================================================================================
	// various PIN-related implementations
	// ===========================================================================================================

	/*
	 * Verify PIN code for purpose "purpose" This method will attempt to verify
	 * PIN using the most secure method available. If that method turns out to
	 * be the UI, will pass purpose to the UI.
	 */

	private void verifyPin(PINPurpose purpose) throws IOException, CardException, InterruptedException, UserCancelledException {
		ResponseAPDU responseApdu;
		int retriesLeft = -1;
		do {
			if (getCCID().hasFeature(CCID.FEATURE.VERIFY_PIN_DIRECT)) {
				responseApdu = verifyPINViaCCIDDirect(retriesLeft, purpose);
			} else if (getCCID().hasFeature(CCID.FEATURE.VERIFY_PIN_START)) {
				responseApdu = verifyPINViaCCIDStartFinish(retriesLeft, purpose);
			} else {
				responseApdu = verifyPINViaUI(retriesLeft, purpose);
			}

			if (0x9000 != responseApdu.getSW()) {
				logger.debug("VERIFY_PIN error");
				logger.debug("SW: "
						+ Integer.toHexString(responseApdu.getSW()));
				if (0x6983 == responseApdu.getSW()) {
					getUI().advisePINBlocked();
					throw new ResponseAPDUException("eID card blocked!",
							responseApdu);
				}
				if (0x63 != responseApdu.getSW1()) {
					logger.debug("PIN verification error.");
					throw new ResponseAPDUException("PIN Verification Error",
							responseApdu);
				}
				retriesLeft = responseApdu.getSW2() & 0xf;
				logger.debug("retries left: " + retriesLeft);
			}
		} while (0x9000 != responseApdu.getSW());
	}

	/*
	 * Verify PIN code using CCID Direct PIN Verify sequence.
	 */
	private ResponseAPDU verifyPINViaCCIDDirect(int retriesLeft, PINPurpose purpose) throws IOException, CardException {
		logger.debug("direct PIN verification...");
		getUI().advisePINPadPINEntry(retriesLeft, purpose);
		byte[] result;
		try {
			result = transmitCCIDControl(
					getCCID().usesPPDU(),
					CCID.FEATURE.VERIFY_PIN_DIRECT,
					getCCID().createPINVerificationDataStructure(
							getLocale(), CCID.INS.VERIFY_PIN));
		} finally {
			getUI().advisePINPadOperationEnd();
		}
		ResponseAPDU responseApdu = new ResponseAPDU(result);
		if (0x6401 == responseApdu.getSW()) {
			logger.debug("canceled by user");
			throw new SecurityException("canceled by user", new ResponseAPDUException(responseApdu));
		} else if (0x6400 == responseApdu.getSW()) {
			logger.debug("PIN pad timeout");
		}

		return responseApdu;
	}

	/*
	 * Verify PIN code using CCID Start/Finish sequence.
	 */
	private ResponseAPDU verifyPINViaCCIDStartFinish(int retriesLeft, PINPurpose purpose)
			throws IOException, CardException, InterruptedException {
		logger.debug("CCID verify PIN start/end sequence...");

		getUI().advisePINPadPINEntry(retriesLeft, purpose);

		try {
			transmitCCIDControl(getCCID().usesPPDU(), CCID.FEATURE.VERIFY_PIN_START,
					getCCID().createPINVerificationDataStructure(getLocale(), CCID.INS.VERIFY_PIN));
			getCCID().waitForOK();
		} finally {
			getUI().advisePINPadOperationEnd();
		}

		return new ResponseAPDU(transmitCCIDControl(getCCID().usesPPDU(), CCID.FEATURE.VERIFY_PIN_FINISH));
	}

	private boolean isWindows8Or10() {
		String osName = System.getProperty("os.name");
		return osName.contains("Windows 8") || osName.contains("Windows 10");
	}

	/*
	 * Verify PIN code by obtaining it from the current UI
	 */
	private ResponseAPDU verifyPINViaUI(int retriesLeft, PINPurpose purpose) throws CardException,
			UserCancelledException {
		boolean windows8 = isWindows8Or10();
		if (windows8) {
			endExclusive();
		}
		char[] pin = getUI().obtainPIN(retriesLeft, purpose);
		if (windows8) {
			beginExclusive();
		}
		byte[] verifyData = new byte[]{(byte) (0x20 | pin.length),
				(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
				(byte) 0xFF, (byte) 0xFF, (byte) 0xFF,};
		for (int idx = 0; idx < pin.length; idx += 2) {
			char digit1 = pin[idx];
			char digit2;
			if (idx + 1 < pin.length) {
				digit2 = pin[idx + 1];
			} else {
				digit2 = '0' + 0xf;
			}
			byte value = (byte) (byte) ((digit1 - '0' << 4) + (digit2 - '0'));
			verifyData[idx / 2 + 1] = value;
		}
		Arrays.fill(pin, (char) 0); // minimize exposure

		logger.debug("verifying PIN...");
		try {
			return transmitCommand(BeIDCommandAPDU.VERIFY_PIN, verifyData);
		} finally {
			Arrays.fill(verifyData, (byte) 0); // minimize exposure
		}
	}

	/*
	 * Modify PIN code using CCID Direct PIN Modify sequence.
	 */
	private ResponseAPDU changePINViaCCIDDirect(int retriesLeft) throws IOException, CardException {
		logger.debug("direct PIN modification...");
		getUI().advisePINPadChangePIN(retriesLeft);
		byte[] result;

		try {
			result = transmitCCIDControl(getCCID().usesPPDU(), CCID.FEATURE.MODIFY_PIN_DIRECT,
					getCCID().createPINModificationDataStructure(getLocale(), CCID.INS.MODIFY_PIN));
		} finally {
			getUI().advisePINPadOperationEnd();
		}

		ResponseAPDU responseApdu = new ResponseAPDU(result);
		switch (responseApdu.getSW()) {
			case 0x6402:
				logger.debug("PINs differ");
				break;
			case 0x6401:
				logger.debug("canceled by user");
				throw new SecurityException("canceled by user", new ResponseAPDUException(responseApdu));
			case 0x6400:
				logger.debug("PIN pad timeout");
				break;
		}

		return responseApdu;
	}

	/*
	 * Modify PIN code using CCID Modify PIN Start sequence
	 */
	private ResponseAPDU changePINViaCCIDStartFinish(int retriesLeft)
			throws IOException, CardException, InterruptedException {
		transmitCCIDControl(
				getCCID().usesPPDU(), CCID.FEATURE.MODIFY_PIN_START,
				getCCID().createPINModificationDataStructure(getLocale(), CCID.INS.MODIFY_PIN)
		);

		try {
			logger.debug("enter old PIN...");
			getUI().advisePINPadOldPINEntry(retriesLeft);
			getCCID().waitForOK();
			getUI().advisePINPadOperationEnd();

			logger.debug("enter new PIN...");
			getUI().advisePINPadNewPINEntry(retriesLeft);
			getCCID().waitForOK();
			getUI().advisePINPadOperationEnd();

			logger.debug("enter new PIN again...");
			getUI().advisePINPadNewPINEntryAgain(retriesLeft);
			getCCID().waitForOK();
		} finally {
			getUI().advisePINPadOperationEnd();
		}

		return new ResponseAPDU(transmitCCIDControl(getCCID().usesPPDU(), CCID.FEATURE.MODIFY_PIN_FINISH));
	}

	/*
	 * Modify PIN via the UI
	 */
	private ResponseAPDU changePINViaUI(int retriesLeft) throws CardException {
		char[][] pins = getUI().obtainOldAndNewPIN(retriesLeft);
		char[] oldPin = pins[0];
		char[] newPin = pins[1];

		byte[] changePinData = new byte[]{(byte) (0x20 | oldPin.length),
				(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
				(byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
				(byte) (0x20 | newPin.length), (byte) 0xFF, (byte) 0xFF,
				(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
				(byte) 0xFF,};

		for (int idx = 0; idx < oldPin.length; idx += 2) {
			char digit1 = oldPin[idx];
			char digit2;
			if (idx + 1 < oldPin.length) {
				digit2 = oldPin[idx + 1];
			} else {
				digit2 = '0' + 0xf;
			}
			byte value = (byte) (byte) ((digit1 - '0' << 4) + (digit2 - '0'));
			changePinData[idx / 2 + 1] = value;
		}
		Arrays.fill(oldPin, (char) 0); // minimize exposure

		for (int idx = 0; idx < newPin.length; idx += 2) {
			char digit1 = newPin[idx];
			char digit2;
			if (idx + 1 < newPin.length) {
				digit2 = newPin[idx + 1];
			} else {
				digit2 = '0' + 0xf;
			}
			byte value = (byte) (byte) ((digit1 - '0' << 4) + (digit2 - '0'));
			changePinData[(idx / 2 + 1) + 8] = value;
		}
		Arrays.fill(newPin, (char) 0); // minimize exposure

		try {
			return transmitCommand(BeIDCommandAPDU.CHANGE_PIN,
					changePinData);
		} finally {
			Arrays.fill(changePinData, (byte) 0);
		}
	}

	/*
	 * Unblock PIN using CCID Verify PIN Direct sequence on the PUK
	 */
	private ResponseAPDU unblockPINViaCCIDVerifyPINDirectOfPUK(int retriesLeft) throws IOException, CardException {
		logger.debug("direct PUK verification...");
		getUI().advisePINPadPUKEntry(retriesLeft);
		byte[] result;
		try {
			result = transmitCCIDControl(
					getCCID().usesPPDU(),
					CCID.FEATURE.VERIFY_PIN_DIRECT,
					getCCID().createPINVerificationDataStructure(getLocale(), CCID.INS.VERIFY_PUK)
			);
		} finally {
			getUI().advisePINPadOperationEnd();
		}

		ResponseAPDU responseApdu = new ResponseAPDU(result);
		if (0x6401 == responseApdu.getSW()) {
			logger.debug("canceled by user");
			throw new SecurityException("canceled by user", new ResponseAPDUException(responseApdu));
		} else if (0x6400 == responseApdu.getSW()) {
			logger.debug("PIN pad timeout");
		}
		return responseApdu;
	}

	/*
	 * Unblock the PIN by obtaining PUK codes from the UI and calling RESET_PIN
	 * on the card.
	 */
	private ResponseAPDU unblockPINViaUI(int retriesLeft)
			throws CardException {
		char[][] puks = getUI().obtainPUKCodes(retriesLeft);
		char[] puk1 = puks[0];
		char[] puk2 = puks[1];

		char[] fullPuk = new char[puk1.length + puk2.length];
		System.arraycopy(puk2, 0, fullPuk, 0, puk2.length);
		Arrays.fill(puk2, (char) 0);
		System.arraycopy(puk1, 0, fullPuk, puk2.length, puk1.length);
		Arrays.fill(puk1, (char) 0);

		byte[] unblockPinData = new byte[]{
				(byte) (0x20 | ((byte) (puk1.length + puk2.length))),
				(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
				(byte) 0xFF, (byte) 0xFF, (byte) 0xFF,};

		for (int idx = 0; idx < fullPuk.length; idx += 2) {
			char digit1 = fullPuk[idx];
			char digit2 = fullPuk[idx + 1];
			byte value = (byte) (byte) ((digit1 - '0' << 4) + (digit2 - '0'));
			unblockPinData[idx / 2 + 1] = value;
		}
		Arrays.fill(fullPuk, (char) 0); // minimize exposure

		try {
			return transmitCommand(BeIDCommandAPDU.RESET_PIN, unblockPinData);
		} finally {
			Arrays.fill(unblockPinData, (byte) 0);
		}
	}

	// ----------------------------------------------------------------------------------------------------------------------------------

	private CCID getCCID() {
		if (this.ccid == null) {
			this.ccid = new CCID(this.card, this.cardTerminal, logger);
		}
		return ccid;
	}

	private BeIDCardUI getUI() {
		if (this.ui == null) {
			if (GraphicsEnvironment.isHeadless()) {
				logger.error(UI_DEFAULT_REQUIRES_HEAD);
				throw new UnsupportedOperationException(UI_DEFAULT_REQUIRES_HEAD);
			}

			try {
				ClassLoader classLoader = BeIDCard.class.getClassLoader();
				Class<?> uiClass = classLoader.loadClass(DEFAULT_UI_IMPLEMENTATION);
				this.ui = (BeIDCardUI) uiClass.newInstance();
				if (locale != null) {
					ui.setLocale(locale);
				}
			} catch (Exception e) {
				logger.error(UI_MISSING_LOG_MESSAGE);
				throw new UnsupportedOperationException(UI_MISSING_LOG_MESSAGE, e);
			}
		}

		return ui;
	}

	/**
	 * Return the CardTerminal that held this BeIdCard when it was detected Will
	 * return null if the physical Card that we represent was removed.
	 *
	 * @return the cardTerminal this BeIDCard was in when detected, or null
	 */
	public CardTerminal getCardTerminal() {
		return cardTerminal;
	}

	public void setCardTerminal(CardTerminal cardTerminal) {
		this.cardTerminal = cardTerminal;
	}

	/*
	 * BeIDCommandAPDU encapsulates values sent in CommandAPDU's, to make these
	 * more readable in BeIDCard.
	 */
	private enum BeIDCommandAPDU {
		SELECT_APPLET_0(0x00, 0xA4, 0x04, 0x0C), // TODO these are the same?
		SELECT_APPLET_1(0x00, 0xA4, 0x04, 0x0C), // TODO see above
		SELECT_FILE(0x00, 0xA4, 0x08, 0x0C),
		READ_BINARY(0x00, 0xB0),
		VERIFY_PIN(0x00, 0x20, 0x00, 0x01),
		CHANGE_PIN(0x00, 0x24, 0x00, 0x01), // 0x0024=change reference change
		SELECT_ALGORITHM_AND_PRIVATE_KEY(0x00, 0x22, 0x41, 0xB6), // ISO 7816-8 SET COMMAND (select algorithm and key for signature)
		COMPUTE_DIGITAL_SIGNATURE(0x00, 0x2A, 0x9E, 0x9A), // ISO 7816-8 COMPUTE DIGITAL SIGNATURE COMMAND
		RESET_PIN(0x00, 0x2C, 0x00, 0x01),
		GET_CHALLENGE(0x00, 0x84, 0x00, 0x00),
		GET_CARD_DATA(0x80, 0xE4, 0x00, 0x00),
		PPDU(0xFF, 0xC2, 0x01);

		private final int cla;
		private final int ins;
		private final int p1;
		private final int p2;

		BeIDCommandAPDU(int cla, int ins, int p1, int p2) {
			this.cla = cla;
			this.ins = ins;
			this.p1 = p1;
			this.p2 = p2;
		}

		BeIDCommandAPDU(int cla, int ins, int p1) {
			this.cla = cla;
			this.ins = ins;
			this.p1 = p1;
			this.p2 = -1;
		}

		BeIDCommandAPDU(int cla, int ins) {
			this.cla = cla;
			this.ins = ins;
			this.p1 = -1;
			this.p2 = -1;
		}

		public int getCla() {
			return cla;
		}

		public int getIns() {
			return ins;
		}

		public int getP1() {
			return p1;
		}

		public int getP2() {
			return p2;
		}
	}
}
