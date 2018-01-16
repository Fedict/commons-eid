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

package be.fedict.commons.eid.client.tests.integration;

import be.fedict.commons.eid.client.BeIDCard;
import be.fedict.commons.eid.client.BeIDCards;
import be.fedict.commons.eid.client.BeIDCardsException;
import be.fedict.commons.eid.client.FileType;
import be.fedict.commons.eid.client.event.BeIDCardListener;
import be.fedict.commons.eid.client.impl.BeIDDigest;
import be.fedict.commons.eid.consumer.Address;
import be.fedict.commons.eid.consumer.BeIDIntegrity;
import be.fedict.commons.eid.consumer.Identity;
import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.x509.DigestInfo;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.Test;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Security;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Arrays;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class BeIDCardTest {
	
	protected BeIDCards beIDCards;

	@Test
	public void testReadFiles() throws Exception {
		BeIDCard beIDCard = getBeIDCard();
		beIDCard.addCardListener(new TestBeIDCardListener());

		System.out.println("reading identity file");
		byte[] identityFile = beIDCard.readFile(FileType.Identity);

		System.out.println("reading identity signature file");
		byte[] identitySignatureFile = beIDCard.readFile(FileType.IdentitySignature);

		System.out.println("reading RRN certificate file");
		byte[] rrnCertificateFile = beIDCard.readFile(FileType.RRNCertificate);

		System.out.println("reading Photo file");
		byte[] photoFile = beIDCard.readFile(FileType.Photo);

		CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
		X509Certificate rrnCertificate = (X509Certificate) certificateFactory.generateCertificate(new ByteArrayInputStream(rrnCertificateFile));

		beIDCard.close();

		BeIDIntegrity beIDIntegrity = new BeIDIntegrity();
		Identity identity = beIDIntegrity.getVerifiedIdentity(identityFile, identitySignatureFile, photoFile, rrnCertificate);

		assertNotNull(identity);
		assertNotNull(identity.getNationalNumber());
	}

	@Test
	public void testAddressFileValidation() throws Exception {
		BeIDCard beIDCard = getBeIDCard();
		beIDCard.addCardListener(new TestBeIDCardListener());

		System.out.println("reading address file");
		byte[] addressFile = beIDCard.readFile(FileType.Address);

		System.out.println("reading address signature file");
		byte[] addressSignatureFile = beIDCard.readFile(FileType.AddressSignature);
		System.out.println("reading identity signature file");

		byte[] identitySignatureFile = beIDCard.readFile(FileType.IdentitySignature);

		System.out.println("reading RRN certificate file");
		byte[] rrnCertificateFile = beIDCard.readFile(FileType.RRNCertificate);

		CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
		X509Certificate rrnCertificate = (X509Certificate) certificateFactory.generateCertificate(new ByteArrayInputStream(rrnCertificateFile));

		beIDCard.close();

		BeIDIntegrity beIDIntegrity = new BeIDIntegrity();
		Address address = beIDIntegrity.getVerifiedAddress(addressFile, identitySignatureFile, addressSignatureFile, rrnCertificate);

		assertNotNull(address);
		assertNotNull(address.getMunicipality());
	}

	@Test
	public void testAuthnSignature() throws Exception {
		BeIDCard beIDCard = getBeIDCard();

		byte[] toBeSigned = new byte[10];
		SecureRandom secureRandom = new SecureRandom();
		secureRandom.nextBytes(toBeSigned);

		X509Certificate authnCertificate = beIDCard.getAuthenticationCertificate();

		byte[] signatureValue;
		try {
			signatureValue = beIDCard.signAuthn(toBeSigned, false);
		} finally {
			beIDCard.close();
		}

		BeIDIntegrity beIDIntegrity = new BeIDIntegrity();
		assertTrue(beIDIntegrity.verifyAuthnSignature(toBeSigned, signatureValue, authnCertificate));
	}

	@Test
	public void testRRNCertificate() throws Exception {
		BeIDCard beIDCard = getBeIDCard();

		X509Certificate rrnCertificate = beIDCard.getRRNCertificate();

		assertNotNull(rrnCertificate);
		System.out.println("RRN certificate: " + rrnCertificate);
	}

	@Test
	public void testPSSSignature() throws Exception {
		BeIDCard beIDCard = getBeIDCard();

		byte[] toBeSigned = new byte[10];
		SecureRandom secureRandom = new SecureRandom();
		secureRandom.nextBytes(toBeSigned);

		X509Certificate authnCertificate = beIDCard.getAuthenticationCertificate();

		MessageDigest messageDigest = MessageDigest.getInstance("SHA-1");
		byte[] digestValue = messageDigest.digest(toBeSigned);

		byte[] signatureValue;
		try {
			signatureValue = beIDCard.sign(digestValue, BeIDDigest.SHA_1_PSS, FileType.AuthentificationCertificate, false);
		} finally {
			beIDCard.close();
		}

		Security.addProvider(new BouncyCastleProvider());

		BeIDIntegrity beIDIntegrity = new BeIDIntegrity();
		boolean result = beIDIntegrity.verifySignature("SHA1withRSAandMGF1", signatureValue, authnCertificate.getPublicKey(), toBeSigned);

		assertTrue(result);
	}

	@Test
	public void testPSSSignatureSHA256() throws Exception {
		BeIDCard beIDCard = getBeIDCard();

		byte[] toBeSigned = new byte[10];
		SecureRandom secureRandom = new SecureRandom();
		secureRandom.nextBytes(toBeSigned);

		X509Certificate authnCertificate = beIDCard
				.getAuthenticationCertificate();

		MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
		byte[] digestValue = messageDigest.digest(toBeSigned);

		byte[] signatureValue;
		try {
			signatureValue = beIDCard.sign(digestValue, BeIDDigest.SHA_256_PSS, FileType.AuthentificationCertificate, false);
		} finally {
			beIDCard.close();
		}

		Security.addProvider(new BouncyCastleProvider());

		BeIDIntegrity beIDIntegrity = new BeIDIntegrity();
		boolean result = beIDIntegrity.verifySignature("SHA256withRSAandMGF1", signatureValue, authnCertificate.getPublicKey(), toBeSigned);

		assertTrue(result);
	}

	@Test
	public void testChangePIN() throws Exception {

		try (BeIDCard beIDCard = getBeIDCard()) {
			beIDCard.changePin(false);
		}
	}

	@Test
	public void testUnblockPIN() throws Exception {
		try (BeIDCard beIDCard = getBeIDCard()) {
			beIDCard.unblockPin(false);
		}
	}

	@Test
	public void testNonRepSignature() throws Exception {
		byte[] toBeSigned = new byte[10];
		SecureRandom secureRandom = new SecureRandom();
		secureRandom.nextBytes(toBeSigned);
		MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
		byte[] digestValue = messageDigest.digest(toBeSigned);

		X509Certificate signingCertificate;
		byte[] signatureValue;
		try (BeIDCard beIDCard = getBeIDCard()) {
			signatureValue = beIDCard.sign(digestValue, BeIDDigest.SHA_256, FileType.NonRepudiationCertificate, false);
			assertNotNull(signatureValue);
			signingCertificate = beIDCard.getSigningCertificate();
		}

		boolean result = verifyNonRepSignature(digestValue, signatureValue, signingCertificate);
		assertTrue(result);
	}

	protected BeIDCard getBeIDCard() {
		this.beIDCards = new BeIDCards(new TestLogger());
		BeIDCard beIDCard = null;
		try {
			beIDCard = this.beIDCards.getOneBeIDCard();
			assertNotNull(beIDCard);

			beIDCard.addCardListener(new BeIDCardListener() {
				@Override
				public void notifyReadProgress(FileType fileType, int offset, int estimatedMaxSize) {
					System.out.println("read progress of " + fileType.name() + ":" + offset + " of " + estimatedMaxSize);
				}

				@Override
				public void notifySigningBegin(FileType keyType) {
					System.out.println("signing with " + (keyType == FileType.AuthentificationCertificate ? "authentication" : "non-repudiation") + " key has begun");
				}

				@Override
				public void notifySigningEnd(FileType keyType) {
					System.out.println("signing with " + (keyType == FileType.AuthentificationCertificate ? "authentication" : "non-repudiation") + " key has ended");
				}
			});
		} catch (BeIDCardsException bcex) {
			System.err.println(bcex.getMessage());
		}

		return beIDCard;
	}

	public boolean verifyNonRepSignature(byte[] expectedDigestValue, byte[] signatureValue, X509Certificate certificate) throws NoSuchPaddingException, NoSuchAlgorithmException, IllegalBlockSizeException, BadPaddingException, InvalidKeyException, IOException {
		PublicKey publicKey = certificate.getPublicKey();

		Cipher cipher = Cipher.getInstance("RSA");
		cipher.init(Cipher.DECRYPT_MODE, publicKey);
		byte[] actualSignatureDigestInfoValue = cipher.doFinal(signatureValue);

		ASN1InputStream asnInputStream = new ASN1InputStream(actualSignatureDigestInfoValue);
		DigestInfo actualSignatureDigestInfo = new DigestInfo((ASN1Sequence) asnInputStream.readObject());
		asnInputStream.close();

		byte[] actualDigestValue = actualSignatureDigestInfo.getDigest();
		return Arrays.equals(expectedDigestValue, actualDigestValue);
	}

}
