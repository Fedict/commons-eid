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

package be.bosa.commons.eid.client.tests.integration;

import be.bosa.commons.eid.client.BeIDCard;
import be.bosa.commons.eid.client.BeIDCards;
import be.bosa.commons.eid.client.impl.CCID;
import be.bosa.commons.eid.jca.BeIDKeyStoreParameter;
import be.bosa.commons.eid.jca.BeIDPrivateKey;
import be.bosa.commons.eid.jca.BeIDProvider;
import org.apache.commons.codec.binary.Hex;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.swing.*;
import java.io.IOException;
import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.security.Key;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStore.Entry;
import java.security.KeyStore.PrivateKeyEntry;
import java.security.KeyStore.ProtectionParameter;
import java.security.KeyStore.TrustedCertificateEntry;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Security;
import java.security.Signature;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPublicKey;
import java.util.Enumeration;
import java.util.Locale;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class JCATest {

	@BeforeClass
	public static void setupSecurityProviders() {
		Security.addProvider(new BeIDProvider());
		Security.addProvider(new BouncyCastleProvider());
	}

	@Test
	public void testSwingParentLocale() throws GeneralSecurityException, IOException {
		JFrame frame = new JFrame("Test Parent frame");
		frame.setSize(200, 200);
		frame.setLocation(300, 300);
		frame.setVisible(true);

		KeyStore keyStore = KeyStore.getInstance("BeID");
		BeIDKeyStoreParameter keyStoreParameter = new BeIDKeyStoreParameter();
		keyStoreParameter.setLogoff(true);
		keyStoreParameter.setParentComponent(frame);
		keyStoreParameter.setLocale(new Locale("nl"));
		keyStore.load(keyStoreParameter);

		PrivateKey authnPrivateKey = (PrivateKey) keyStore.getKey("Authentication", null);
		Signature signature = Signature.getInstance("SHA256withRSA");
		signature.initSign(authnPrivateKey);

		byte[] toBeSigned = "hello world".getBytes();
		signature.update(toBeSigned);
		signature.sign();
	}

	private static class MyFrame extends JFrame implements KeyStore.LoadStoreParameter {
		public MyFrame() {
			super("Test frame 2");
			setSize(200, 200);
			setLocation(300, 300);
			setVisible(true);
		}

		@Override
		public ProtectionParameter getProtectionParameter() {
			return null;
		}
	}

	@Test
	public void testSwingParent2() throws Exception {
		KeyStore keyStore = KeyStore.getInstance("BeID");
		keyStore.load(new MyFrame());

		PrivateKey authnPrivateKey = (PrivateKey) keyStore.getKey("Authentication", null);
		Signature signature = Signature.getInstance("SHA256withRSA");
		signature.initSign(authnPrivateKey);

		byte[] toBeSigned = "hello world".getBytes();
		signature.update(toBeSigned);
		byte[] signatureValue = signature.sign();

		Certificate[] certificateChain = keyStore.getCertificateChain("Authentication");
		signature.initVerify(certificateChain[0]);
		signature.update(toBeSigned);
		assertTrue(signature.verify(signatureValue));
	}

	@Test
	public void testRecoveryAfterRemoval() throws Exception {
		Security.addProvider(new BeIDProvider());

		KeyStore keyStore = KeyStore.getInstance("BeID");
		keyStore.load(null);

		PrivateKey authnPrivateKey = (PrivateKey) keyStore.getKey("Authentication", null);
		Signature signature = Signature.getInstance("SHA256withRSA");
		signature.initSign(authnPrivateKey);

		byte[] toBeSigned = "hello world".getBytes();
		signature.update(toBeSigned);
		signature.sign();

		JOptionPane.showMessageDialog(null, "Please remove/insert eID card...");

		keyStore.load(null); // reload the keystore.
		authnPrivateKey = (PrivateKey) keyStore.getKey("Authentication", null);
		signature.initSign(authnPrivateKey);
		signature.update(toBeSigned);
		signature.sign();
	}

	/**
	 * Integration test for automatic recovery of a {@link PrivateKey} instance.
	 * <br>
	 * Automatic recovery should work on the same eID card.
	 * <br>
	 * When inserting another eID card however, the automatic recovery should
	 * fail.
	 */
	@Test
	public void testAutoRecovery() throws Exception {
		KeyStore keyStore = KeyStore.getInstance("BeID");
		BeIDKeyStoreParameter keyStoreParameter = new BeIDKeyStoreParameter();
		keyStoreParameter.setAutoRecovery(true);
		keyStoreParameter.setCardReaderStickiness(true);
		keyStore.load(keyStoreParameter);

		PrivateKey authnPrivateKey = (PrivateKey) keyStore.getKey("Authentication", null);
		PublicKey authnPublicKey = keyStore.getCertificate("Authentication").getPublicKey();
		Signature signature = Signature.getInstance("SHA256withRSA");
		signature.initSign(authnPrivateKey);

		byte[] toBeSigned = "hello world".getBytes();
		signature.update(toBeSigned);
		byte[] signatureValue = signature.sign();

		signature.initVerify(authnPublicKey);
		signature.update(toBeSigned);
		assertTrue(signature.verify(signatureValue));

		JOptionPane.showMessageDialog(null, "Please remove/insert eID card...");

		signature.initSign(authnPrivateKey);
		signature.update(toBeSigned);
		signatureValue = signature.sign();

		signature.initVerify(authnPublicKey);
		signature.update(toBeSigned);
		assertTrue(signature.verify(signatureValue));
	}

	@Test
	public void testGetCertificateCaching() throws Exception {
		Security.addProvider(new BeIDProvider());

		KeyStore keyStore = KeyStore.getInstance("BeID");
		keyStore.load(null);

		for (int idx = 0; idx < 100; idx++) {
			assertNotNull(keyStore.getCertificate("Authentication"));
		}
	}

	@Test
	public void testCAAliases() throws Exception {
		Security.addProvider(new BeIDProvider());
		KeyStore keyStore = KeyStore.getInstance("BeID");
		keyStore.load(null);

		X509Certificate citizenCACertificate = (X509Certificate) keyStore.getCertificate("CA");
		X509Certificate rootCACertificate = (X509Certificate) keyStore.getCertificate("Root");
		X509Certificate rrnCertificate = (X509Certificate) keyStore.getCertificate("RRN");

		assertNotNull(citizenCACertificate);
		System.out.println("citizen CA: " + citizenCACertificate.getSubjectX500Principal());

		assertNotNull(rootCACertificate);
		System.out.println("root CA: " + rootCACertificate.getSubjectX500Principal());

		assertNotNull(rrnCertificate);
		assertTrue(rrnCertificate.getSubjectX500Principal().toString().contains("RRN"));
	}

	@Test
	public void testRRNCertificate() throws Exception {
		Security.addProvider(new BeIDProvider());
		KeyStore keyStore = KeyStore.getInstance("BeID");
		keyStore.load(null);

		assertTrue(keyStore.containsAlias("RRN"));
		Entry entry = keyStore.getEntry("RRN", null);
		assertNotNull(entry);
		assertTrue(entry instanceof TrustedCertificateEntry);

		TrustedCertificateEntry trustedCertificateEntry = (TrustedCertificateEntry) entry;
		assertNotNull(trustedCertificateEntry.getTrustedCertificate());
		assertTrue(((X509Certificate) trustedCertificateEntry.getTrustedCertificate()).getSubjectX500Principal().toString().contains("RRN"));
		assertNotNull(keyStore.getCertificate("RRN"));

		Certificate[] certificateChain = keyStore.getCertificateChain("RRN");
		assertNotNull(certificateChain);
		assertEquals(2, certificateChain.length);

		System.out.println("RRN subject: " + ((X509Certificate) certificateChain[0]).getSubjectX500Principal());
		System.out.println("RRN issuer: " + ((X509Certificate) certificateChain[0]).getIssuerX500Principal());
		System.out.println("root subject: " + ((X509Certificate) certificateChain[1]).getSubjectX500Principal());
		System.out.println("root issuer: " + ((X509Certificate) certificateChain[1]).getIssuerX500Principal());
	}

	@Test
	public void testAuthenticationSignatures() throws Exception {
		KeyStore keyStore = KeyStore.getInstance("BeID");
		keyStore.load(null);

		X509Certificate authnCertificate = (X509Certificate) keyStore.getCertificate("Authentication");
		PrivateKey authnPrivateKey = (PrivateKey) keyStore.getKey("Authentication", null);

		verifySignatureAlgorithm("SHA256withRSA", authnPrivateKey, authnCertificate.getPublicKey());
		verifySignatureAlgorithm("SHA224withRSA", authnPrivateKey, authnCertificate.getPublicKey());
		verifySignatureAlgorithm("SHA256withRSA", authnPrivateKey, authnCertificate.getPublicKey());
		verifySignatureAlgorithm("SHA384withRSA", authnPrivateKey, authnCertificate.getPublicKey());
		verifySignatureAlgorithm("SHA512withRSA", authnPrivateKey, authnCertificate.getPublicKey());
		verifySignatureAlgorithm("RIPEMD128withRSA", authnPrivateKey, authnCertificate.getPublicKey());
		verifySignatureAlgorithm("RIPEMD160withRSA", authnPrivateKey, authnCertificate.getPublicKey());
		verifySignatureAlgorithm("RIPEMD256withRSA", authnPrivateKey, authnCertificate.getPublicKey());
	}

	@Test
	public void testNonRepudiationSignature() throws Exception {
		KeyStore keyStore = KeyStore.getInstance("BeID");
		keyStore.load(null);

		PrivateKey signPrivateKey = (PrivateKey) keyStore.getKey("Signature", null);
		Signature signature = Signature.getInstance("SHA256withRSA");
		signature.initSign(signPrivateKey);

		byte[] toBeSigned = "hello world".getBytes();
		signature.update(toBeSigned);
		byte[] signatureValue = signature.sign();
		assertNotNull(signatureValue);

		Certificate[] signCertificateChain = keyStore.getCertificateChain("Signature");
		assertNotNull(signCertificateChain);
	}

	@Test
	public void testNonRepudiationSignaturePPDU() throws Exception {
		CCID.riskPPDU(true);

		KeyStore keyStore = KeyStore.getInstance("BeID");
		keyStore.load(null);

		PrivateKey signPrivateKey = (PrivateKey) keyStore.getKey("Signature", null);
		Signature signature = Signature.getInstance("SHA256withRSA");
		signature.initSign(signPrivateKey);

		byte[] toBeSigned = "hello world".getBytes();
		signature.update(toBeSigned);
		byte[] signatureValue = signature.sign();
		assertNotNull(signatureValue);

		Certificate[] signCertificateChain = keyStore.getCertificateChain("Signature");
		assertNotNull(signCertificateChain);
	}

	@Test
	public void testLocale() throws Exception {
		Security.addProvider(new BeIDProvider());

		KeyStore keyStore = KeyStore.getInstance("BeID");
		BeIDKeyStoreParameter beIDKeyStoreParameter = new BeIDKeyStoreParameter();
		beIDKeyStoreParameter.setLocale(Locale.FRENCH);
		beIDKeyStoreParameter.setLogger(new TestLogger());
		keyStore.load(beIDKeyStoreParameter);

		PrivateKey privateKey = (PrivateKey) keyStore.getKey("Signature", null);
		Signature signature = Signature.getInstance("SHA256withRSA");
		signature.initSign(privateKey);

		byte[] toBeSigned = "hello world".getBytes();
		signature.update(toBeSigned);
		signature.sign();
	}

	@Test
	public void testBeIDSignature() throws Exception {
		Security.addProvider(new BeIDProvider());

		KeyStore keyStore = KeyStore.getInstance("BeID");
		BeIDKeyStoreParameter keyStoreParameter = new BeIDKeyStoreParameter();
		BeIDCard beIDCard = getBeIDCard();
		keyStoreParameter.setBeIDCard(beIDCard);
		keyStoreParameter.setLogoff(true);
		keyStore.load(keyStoreParameter);

		Enumeration<String> aliases = keyStore.aliases();
		while (aliases.hasMoreElements()) {
			String alias = aliases.nextElement();
			System.out.println("alias: " + alias);
		}

		assertEquals(2, keyStore.size());
		assertTrue(keyStore.containsAlias("Signature"));
		assertTrue(keyStore.containsAlias("Authentication"));
		assertNotNull(keyStore.getCreationDate("Signature"));
		assertNotNull(keyStore.getCreationDate("Authentication"));

		assertTrue(keyStore.isKeyEntry("Signature"));
		X509Certificate signCertificate = (X509Certificate) keyStore.getCertificate("Signature");
		assertNotNull(signCertificate);

		assertTrue(keyStore.isKeyEntry("Authentication"));
		X509Certificate authnCertificate = (X509Certificate) keyStore.getCertificate("Authentication");
		assertNotNull(authnCertificate);

		assertNotNull(keyStore.getCertificateChain("Signature"));
		assertNotNull(keyStore.getCertificateChain("Authentication"));

		assertTrue(keyStore.isKeyEntry("Authentication"));
		PrivateKey authnPrivateKey = (PrivateKey) keyStore.getKey("Authentication", null);
		assertNotNull(authnPrivateKey);

		assertTrue(keyStore.isKeyEntry("Signature"));
		PrivateKey signPrivateKey = (PrivateKey) keyStore.getKey("Signature", null);
		assertNotNull(signPrivateKey);

		verifySignatureAlgorithm("SHA256withRSA", authnPrivateKey, authnCertificate.getPublicKey());
		verifySignatureAlgorithm("SHA256withRSA", signPrivateKey, signCertificate.getPublicKey());
		verifySignatureAlgorithm("SHA384withRSA", authnPrivateKey, authnCertificate.getPublicKey());
		verifySignatureAlgorithm("SHA512withRSA", authnPrivateKey, authnCertificate.getPublicKey());

		verifySignatureAlgorithm("SHA256withRSAandMGF1", authnPrivateKey, authnCertificate.getPublicKey());
		verifySignatureAlgorithm("SHA256withRSAandMGF1", authnPrivateKey, authnCertificate.getPublicKey());
	}

	@Test
	public void testPSSPrefix() throws Exception {
		KeyStore keyStore = KeyStore.getInstance("BeID");
		keyStore.load(null);
		PrivateKey authnPrivateKey = (PrivateKey) keyStore.getKey("Authentication", null);
		X509Certificate authnCertificate = (X509Certificate) keyStore.getCertificate("Authentication");
		PublicKey authnPublicKey = authnCertificate.getPublicKey();

		Signature signature = Signature.getInstance("SHA256withRSAandMGF1");
		signature.initSign(authnPrivateKey);

		byte[] toBeSigned = "hello world".getBytes();
		signature.update(toBeSigned);
		byte[] signatureValue = signature.sign();

		signature.initVerify(authnPublicKey);
		signature.update(toBeSigned);
		boolean result = signature.verify(signatureValue);
		assertTrue(result);

		RSAPublicKey rsaPublicKey = (RSAPublicKey) authnPublicKey;
		BigInteger signatureValueBigInteger = new BigInteger(signatureValue);
		BigInteger messageBigInteger = signatureValueBigInteger.modPow(rsaPublicKey.getPublicExponent(), rsaPublicKey.getModulus());
		String paddedMessage = new String(Hex.encodeHex(messageBigInteger.toByteArray()));
		System.out.println("padded message: " + paddedMessage);
		assertTrue(paddedMessage.endsWith("bc"));
	}

	@Test
	public void testPSS256() throws Exception {
		KeyStore keyStore = KeyStore.getInstance("BeID");
		keyStore.load(null);
		PrivateKey authnPrivateKey = (PrivateKey) keyStore.getKey("Authentication", null);
		X509Certificate authnCertificate = (X509Certificate) keyStore.getCertificate("Authentication");
		PublicKey authnPublicKey = authnCertificate.getPublicKey();

		Signature signature = Signature.getInstance("SHA256withRSAandMGF1");
		signature.initSign(authnPrivateKey);

		byte[] toBeSigned = "hello world".getBytes();
		signature.update(toBeSigned);
		byte[] signatureValue = signature.sign();

		signature.initVerify(authnPublicKey);
		signature.update(toBeSigned);
		boolean result = signature.verify(signatureValue);
		assertTrue(result);
	}

	@Test
	public void testSoftwareRSAKeyWrapping() throws Exception {
		KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
		KeyPair keyPair = keyPairGenerator.generateKeyPair();

		KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
		SecretKey secretKey = keyGenerator.generateKey();
		System.out.println("secret key algo: " + secretKey.getAlgorithm());

		Cipher cipher = Cipher.getInstance("RSA");
		cipher.init(Cipher.WRAP_MODE, keyPair.getPublic());
		System.out.println("cipher security provider: " + cipher.getProvider().getName());
		System.out.println("cipher type: " + cipher.getClass().getName());
		byte[] wrappedKey = cipher.wrap(secretKey);

		cipher.init(Cipher.UNWRAP_MODE, keyPair.getPrivate());
		Key resultKey = cipher.unwrap(wrappedKey, "AES", Cipher.SECRET_KEY);

		assertArrayEquals(secretKey.getEncoded(), resultKey.getEncoded());
	}

	@Test
	public void testAutoFindCard() throws Exception {
		KeyStore keyStore = KeyStore.getInstance("BeID");
		BeIDKeyStoreParameter beIDKeyStoreParameter = new BeIDKeyStoreParameter();
		beIDKeyStoreParameter.setLocale(new Locale("fr"));
		keyStore.load(beIDKeyStoreParameter);

		Enumeration<String> aliases = keyStore.aliases();
		assertNotNull(aliases);
		while (aliases.hasMoreElements()) {
			String alias = aliases.nextElement();
			System.out.println("alias: " + alias);
		}

		X509Certificate authnCertificate = (X509Certificate) keyStore.getCertificate("Authentication");
		assertNotNull(authnCertificate);
	}

	@Test
	public void testGetEntry() throws Exception {
		KeyStore keyStore = KeyStore.getInstance("BeID");
		keyStore.load(null);
		PrivateKeyEntry privateKeyEntry = (PrivateKeyEntry) keyStore.getEntry("Authentication", null);
		assertNotNull(privateKeyEntry);
		assertTrue(privateKeyEntry.getPrivateKey() instanceof BeIDPrivateKey);

		TrustedCertificateEntry caEntry = (TrustedCertificateEntry) keyStore.getEntry("CA", null);
		assertNotNull(caEntry);
		System.out.println("CA entry: " + ((X509Certificate) caEntry.getTrustedCertificate()).getSubjectX500Principal());

		TrustedCertificateEntry rootEntry = (TrustedCertificateEntry) keyStore.getEntry("Root", null);
		assertNotNull(rootEntry);
		System.out.println("root entry: " + ((X509Certificate) rootEntry.getTrustedCertificate()).getSubjectX500Principal());
	}

	private void verifySignatureAlgorithm(String signatureAlgorithm, PrivateKey privateKey, PublicKey publicKey) throws GeneralSecurityException {
		Signature signature = Signature.getInstance(signatureAlgorithm);
		signature.initSign(privateKey);
		assertTrue(signature.getProvider() instanceof BeIDProvider);

		byte[] toBeSigned = "hello world".getBytes();
		signature.update(toBeSigned);
		byte[] signatureValue = signature.sign();
		assertNotNull(signatureValue);

		signature.initVerify(publicKey);
		signature.update(toBeSigned);
		boolean beIDResult = signature.verify(signatureValue);
		assertTrue(beIDResult);

		signature = Signature.getInstance(signatureAlgorithm);
		signature.initVerify(publicKey);
		signature.update(toBeSigned);
		boolean result = signature.verify(signatureValue);
		assertTrue(result);

		RSAPublicKey rsaPublicKey = (RSAPublicKey) publicKey;
		BigInteger signatureValueBigInteger = new BigInteger(signatureValue);
		BigInteger messageBigInteger = signatureValueBigInteger.modPow(rsaPublicKey.getPublicExponent(), rsaPublicKey.getModulus());
		System.out.println("Padded DigestInfo: " + new String(Hex.encodeHex(messageBigInteger.toByteArray())));
	}

	private BeIDCard getBeIDCard() throws Exception {
		TestLogger logger = new TestLogger();
		BeIDCards beIDCards = new BeIDCards(logger);
		BeIDCard beIDCard = beIDCards.getOneBeIDCard();
		assertNotNull(beIDCard);
		return beIDCard;
	}
}
