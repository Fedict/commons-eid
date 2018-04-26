/*
 * Commons eID Project.
 * Copyright (C) 2008-2013 FedICT.
 * Copyright (C) 2009 Frank Cornelis.
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

import java.io.ByteArrayInputStream;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Arrays;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import be.bosa.commons.eid.consumer.tlv.TlvParser;

/**
 * Utility class for various eID related integrity checks.
 *
 * @author Frank Cornelis
 */
public class BeIDIntegrity {

	private final static Log LOG = LogFactory.getLog(BeIDIntegrity.class);

	private final CertificateFactory certificateFactory;

	/**
	 * Default constructor.
	 */
	public BeIDIntegrity() {
		try {
			this.certificateFactory = CertificateFactory.getInstance("X.509");
		} catch (CertificateException cex) {
			throw new RuntimeException("X.509 algo", cex);
		}
	}

	/**
	 * Loads a DER-encoded X509 certificate from a byte array.
	 */
	public X509Certificate loadCertificate(byte[] encodedCertificate) {
		X509Certificate certificate;
		try {
			certificate = (X509Certificate) certificateFactory.generateCertificate(new ByteArrayInputStream(encodedCertificate));
		} catch (CertificateException cex) {
			throw new RuntimeException("X509 decoding error: "
					+ cex.getMessage(), cex);
		}
		return certificate;
	}

	/**
	 * Gives back a parsed identity file after integrity verification.
	 */
	public Identity getVerifiedIdentity(byte[] identityFile, byte[] identitySignatureFile, X509Certificate rrnCertificate)
			throws NoSuchAlgorithmException {
		return getVerifiedIdentity(identityFile, identitySignatureFile, null, rrnCertificate);
	}

	/**
	 * Gives back a parsed identity file after integrity verification including
	 * the eID photo.
	 */
	public Identity getVerifiedIdentity(byte[] identityFile, byte[] identitySignatureFile, byte[] photo, X509Certificate rrnCertificate)
			throws NoSuchAlgorithmException {
		PublicKey publicKey = rrnCertificate.getPublicKey();
		boolean result;
		try {
			result = verifySignature(rrnCertificate.getSigAlgName(),
					identitySignatureFile, publicKey, identityFile);
			if (!result) {
				throw new SecurityException("signature integrity error");
			}
		} catch (Exception ex) {
			throw new SecurityException(
					"identity signature verification error: " + ex.getMessage(),
					ex);
		}

		Identity identity = TlvParser.parse(identityFile, Identity.class);
		if (null != photo) {
			byte[] expectedPhotoDigest = identity.getPhotoDigest();
			byte[] actualPhotoDigest = digest(
					getDigestAlgo(expectedPhotoDigest.length), photo);
			if (!Arrays.equals(expectedPhotoDigest, actualPhotoDigest)) {
				throw new SecurityException("photo digest mismatch");
			}
		}
		return identity;
	}

	/**
	 * Gives back a parsed address file after integrity verification.
	 */
	public Address getVerifiedAddress(byte[] addressFile,
									  byte[] identitySignatureFile,
									  byte[] addressSignatureFile,
									  X509Certificate rrnCertificate) {
		byte[] trimmedAddressFile = trimRight(addressFile);
		PublicKey publicKey = rrnCertificate.getPublicKey();
		try {
			if (!verifySignature(rrnCertificate.getSigAlgName(), addressSignatureFile, publicKey, trimmedAddressFile, identitySignatureFile)) {
				throw new SecurityException("address integrity error");
			}
		} catch (NoSuchAlgorithmException | SignatureException | InvalidKeyException ex) {
			throw new SecurityException("address signature verification error: " + ex.getMessage(), ex);
		}

		return TlvParser.parse(addressFile, Address.class);
	}

	/**
	 * Verifies a SHA256withRSA signature.
	 */
	public boolean verifySignature(byte[] signatureData, PublicKey publicKey, byte[]... data)
			throws InvalidKeyException, NoSuchAlgorithmException, SignatureException {
		return verifySignature("SHA256withRSA", signatureData, publicKey, data);
	}

	/**
	 * Verifies a signature.
	 */
	public boolean verifySignature(String signatureAlgo,
								   byte[] signatureData, PublicKey publicKey,
								   byte[]... data) throws NoSuchAlgorithmException, InvalidKeyException, SignatureException {
		Signature signature = Signature.getInstance(signatureAlgo);
		signature.initVerify(publicKey);

		for (byte[] dataItem : data) {
			signature.update(dataItem);
		}

		return signature.verify(signatureData);
	}

	private byte[] digest(String algoName, byte[] data) throws NoSuchAlgorithmException {
		MessageDigest messageDigest = MessageDigest.getInstance(algoName);
		return messageDigest.digest(data);
	}

	private byte[] trimRight(byte[] addressFile) {
		int idx;
		for (idx = 0; idx < addressFile.length; idx++) {
			if (0 == addressFile[idx]) break;
		}

		byte[] result = new byte[idx];
		System.arraycopy(addressFile, 0, result, 0, idx);
		return result;
	}

	/**
	 * Verifies an authentication signature.
	 */
	public boolean verifyAuthnSignature(byte[] toBeSigned, byte[] signatureValue, X509Certificate authnCertificate) {
		try {
			PublicKey publicKey = authnCertificate.getPublicKey();
			return verifySignature(signatureValue, publicKey, toBeSigned);
		} catch (InvalidKeyException ikex) {
			LOG.warn("invalid key: " + ikex.getMessage(), ikex);
			return false;
		} catch (NoSuchAlgorithmException nsaex) {
			LOG.warn("no such algo: " + nsaex.getMessage(), nsaex);
			return false;
		} catch (SignatureException sigex) {
			LOG.warn("signature error: " + sigex.getMessage(), sigex);
			return false;
		}
	}

	private String getDigestAlgo(int hashSize) throws NoSuchAlgorithmException {
		switch (hashSize) {
			case 20:
				return "SHA-1";
			case 28:
				return "SHA-224";
			case 32:
				return "SHA-256";
			case 48:
				return "SHA-384";
			case 64:
				return "SHA-512";
		}

		throw new NoSuchAlgorithmException("Failed to find guess algorithm for hash size of " + hashSize + " bytes");
	}
}
