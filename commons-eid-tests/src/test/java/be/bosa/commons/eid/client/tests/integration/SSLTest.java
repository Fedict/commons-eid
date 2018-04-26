/*
 * Commons eID Project.
 * Copyright (C) 2008-2013 FedICT.
 * Copyright (C) 2014 e-Contract.be BVBA.
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.net.Socket;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Principal;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.Security;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.RSAKeyGenParameterSpec;
import java.util.Locale;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509KeyManager;
import javax.net.ssl.X509TrustManager;
import javax.swing.*;

import org.bouncycastle.asn1.misc.MiscObjectIdentifiers;
import org.bouncycastle.asn1.misc.NetscapeCertType;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.KeyUsage;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509ExtensionUtils;
import org.bouncycastle.crypto.params.AsymmetricKeyParameter;
import org.bouncycastle.crypto.util.PrivateKeyFactory;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.DefaultDigestAlgorithmIdentifierFinder;
import org.bouncycastle.operator.DefaultSignatureAlgorithmIdentifierFinder;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.bc.BcRSAContentSignerBuilder;
import org.joda.time.DateTime;
import org.junit.BeforeClass;
import org.junit.Test;

import be.bosa.commons.eid.jca.BeIDManagerFactoryParameters;
import be.bosa.commons.eid.jca.BeIDProvider;

public class SSLTest {

	@BeforeClass
	public static void setupSecurityProviders() {
		Security.addProvider(new BeIDProvider());
	}

	@Test
	public void testTestEIDBelgiumBe() throws Exception {
		KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance("BeID");
		keyManagerFactory.init(null);

		SSLContext sslContext = SSLContext.getInstance("TLS");
		sslContext.init(keyManagerFactory.getKeyManagers(), new TrustManager[]{new ClientTestX509TrustManager()}, new SecureRandom());

		SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();
		SSLSocket sslSocket = (SSLSocket) sslSocketFactory.createSocket("test.eid.belgium.be", 443);
		System.out.println("socket created");

		SSLSession sslSession = sslSocket.getSession();
		Certificate[] peerCertificates = sslSession.getPeerCertificates();
		for (Certificate peerCertificate : peerCertificates) {
			System.out.println("peer certificate: " + ((X509Certificate) peerCertificate).getSubjectX500Principal());
		}
	}

	@Test
	public void testMutualSSL() throws Exception {
		KeyPair serverKeyPair = generateKeyPair();
		PrivateKey serverPrivateKey = serverKeyPair.getPrivate();
		DateTime notBefore = new DateTime();
		DateTime notAfter = notBefore.plusDays(1);
		X509Certificate serverCertificate = generateCACertificate(serverKeyPair, "CN=Test", notBefore, notAfter);

		KeyManager keyManager = new ServerTestX509KeyManager(serverPrivateKey, serverCertificate);
		TrustManager trustManager = new ServerTestX509TrustManager();
		SSLContext sslContext = SSLContext.getInstance("TLS");
		sslContext.init(new KeyManager[]{keyManager}, new TrustManager[]{trustManager}, new SecureRandom());

		SSLServerSocketFactory sslServerSocketFactory = sslContext.getServerSocketFactory();
		int serverPort = 8443;
		SSLServerSocket sslServerSocket = (SSLServerSocket) sslServerSocketFactory.createServerSocket(serverPort);
		sslServerSocket.setNeedClientAuth(true);

		TestRunnable testRunnable = new TestRunnable(serverPort);
		Thread thread = new Thread(testRunnable);
		thread.start();

		SSLSocket sslSocket = (SSLSocket) sslServerSocket.accept();
		System.out.println("server accepted");
		InputStream inputStream = sslSocket.getInputStream();
		int result = inputStream.read();
		System.out.println("result: " + result);
		assertEquals(12, result);
		SSLSession sslSession = sslSocket.getSession();
		sslSession.invalidate();
		sslSocket = (SSLSocket) sslServerSocket.accept();
		inputStream = sslSocket.getInputStream();
		result = inputStream.read();
		System.out.println("result: " + result);
		assertEquals(34, result);
	}

	private static class TestRunnable implements Runnable {

		private final int serverPort;

		public TestRunnable(int serverPort) {
			this.serverPort = serverPort;
		}

		@Override
		public void run() {
			try {
				mutualSSLConnection();
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}

		private void mutualSSLConnection() throws Exception {
			Thread.sleep(1000);

			JFrame frame = new JFrame("Mutual SSL test");
			frame.setSize(200, 200);
			frame.setLocation(300, 300);
			frame.setVisible(true);

			KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance("BeID");
			BeIDManagerFactoryParameters spec = new BeIDManagerFactoryParameters();
			spec.setLocale(Locale.FRENCH);
			spec.setParentComponent(frame);
			spec.setAutoRecovery(true);
			spec.setCardReaderStickiness(true);
			keyManagerFactory.init(spec);

			SSLContext sslContext = SSLContext.getInstance("TLS");
			sslContext.init(keyManagerFactory.getKeyManagers(), new TrustManager[]{new ClientTestX509TrustManager()}, new SecureRandom());

			SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();
			SSLSocket sslSocket = (SSLSocket) sslSocketFactory.createSocket("localhost", this.serverPort);
			System.out.println("socket created");

			OutputStream outputStream = sslSocket.getOutputStream();
			outputStream.write(12);
			SSLSession sslSession = sslSocket.getSession();
			sslSession.invalidate();

			JOptionPane.showMessageDialog(null, "Please remove eID card...");
			sslSocket.close();
			sslSocket = (SSLSocket) sslSocketFactory.createSocket("localhost", this.serverPort);
			outputStream = sslSocket.getOutputStream();
			outputStream.write(34);
		}
	}

	private static class ClientTestX509TrustManager implements X509TrustManager {
		@Override
		public void checkClientTrusted(X509Certificate[] chain, String authType) {
			System.out.println("checkClientTrusted");
		}

		@Override
		public void checkServerTrusted(X509Certificate[] chain, String authType) {
			System.out.println("checkServerTrusted: " + authType);
		}

		@Override
		public X509Certificate[] getAcceptedIssuers() {
			System.out.println("getAcceptedIssuers");
			return null;
		}
	}

	private static class ServerTestX509TrustManager implements X509TrustManager {
		@Override
		public void checkClientTrusted(X509Certificate[] chain, String authType) {
			System.out.println("checkClientTrusted");
			System.out.println("subject: " + chain[0].getSubjectX500Principal());
		}

		@Override
		public void checkServerTrusted(X509Certificate[] chain, String authType) {
			System.out.println("checkServerTrusted");
		}

		@Override
		public X509Certificate[] getAcceptedIssuers() {
			System.out.println("getAcceptedIssuers");
			return new X509Certificate[]{};
		}
	}

	private static class ServerTestX509KeyManager implements X509KeyManager {
		private final PrivateKey serverPrivateKey;
		private final X509Certificate serverCertificate;

		public ServerTestX509KeyManager(PrivateKey serverPrivateKey, X509Certificate serverCertificate) {
			this.serverPrivateKey = serverPrivateKey;
			this.serverCertificate = serverCertificate;
		}

		@Override
		public String chooseClientAlias(String[] keyType, Principal[] issuers, Socket socket) {
			System.out.println("chooseClientAlias");
			return null;
		}

		@Override
		public String chooseServerAlias(String keyType, Principal[] issuers, Socket socket) {
			System.out.println("chooseServerAlias: " + keyType);
			if (keyType.equals("RSA")) {
				return "test-server";
			}
			return null;
		}

		@Override
		public X509Certificate[] getCertificateChain(String alias) {
			System.out.println("getCertificateChain: " + alias);
			if (!alias.equals("test-server")) {
				return null;
			}
			return new X509Certificate[]{this.serverCertificate};
		}

		@Override
		public String[] getClientAliases(String keyType, Principal[] issuers) {
			System.out.println("getClientAliases");
			return null;
		}

		@Override
		public PrivateKey getPrivateKey(String alias) {
			System.out.println("getPrivateKey: " + alias);
			if (!alias.equals("test-server")) {
				return null;
			}
			return this.serverPrivateKey;
		}

		@Override
		public String[] getServerAliases(String keyType, Principal[] issuers) {
			System.out.println("getServerAliases");
			return null;
		}
	}

	private static KeyPair generateKeyPair() throws Exception {
		KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
		SecureRandom random = new SecureRandom();
		keyPairGenerator.initialize(new RSAKeyGenParameterSpec(1024, RSAKeyGenParameterSpec.F4), random);
		return keyPairGenerator.generateKeyPair();
	}

	private X509Certificate generateCACertificate(KeyPair keyPair, String subject, DateTime notBefore, DateTime notAfter) {
		System.out.println("generate CA certificate: " + subject);

		X500Name issuer = new X500Name(subject);
		X500Name subjectX500Name = new X500Name(subject);

		SubjectPublicKeyInfo publicKeyInfo = SubjectPublicKeyInfo.getInstance(keyPair.getPublic().getEncoded());

		SecureRandom secureRandom = new SecureRandom();
		byte[] serialValue = new byte[8];
		secureRandom.nextBytes(serialValue);
		BigInteger serial = new BigInteger(serialValue);

		X509v3CertificateBuilder x509v3CertificateBuilder = new X509v3CertificateBuilder(issuer, serial, notBefore.toDate(), notAfter.toDate(), subjectX500Name, publicKeyInfo);

		try {
			JcaX509ExtensionUtils extensionUtils = new JcaX509ExtensionUtils();
			x509v3CertificateBuilder.addExtension(Extension.subjectKeyIdentifier, false,
					extensionUtils.createSubjectKeyIdentifier(keyPair.getPublic())
			);
			x509v3CertificateBuilder.addExtension(Extension.authorityKeyIdentifier, false,
					extensionUtils.createAuthorityKeyIdentifier(keyPair.getPublic())
			);

			x509v3CertificateBuilder.addExtension(MiscObjectIdentifiers.netscapeCertType, false,
					new NetscapeCertType(NetscapeCertType.sslCA | NetscapeCertType.smimeCA | NetscapeCertType.objectSigningCA)
			);

			x509v3CertificateBuilder.addExtension(Extension.keyUsage, true,
					new KeyUsage(KeyUsage.keyCertSign | KeyUsage.cRLSign));

			x509v3CertificateBuilder.addExtension(Extension.basicConstraints, true,
					new BasicConstraints(2147483647)
			);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

		AlgorithmIdentifier sigAlgId = new DefaultSignatureAlgorithmIdentifierFinder().find("SHA256withRSA");
		AlgorithmIdentifier digAlgId = new DefaultDigestAlgorithmIdentifierFinder().find(sigAlgId);
		AsymmetricKeyParameter asymmetricKeyParameter;
		try {
			asymmetricKeyParameter = PrivateKeyFactory.createKey(keyPair.getPrivate().getEncoded());
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		ContentSigner contentSigner;
		try {
			contentSigner = new BcRSAContentSignerBuilder(sigAlgId, digAlgId).build(asymmetricKeyParameter);
		} catch (OperatorCreationException e) {
			throw new RuntimeException(e);
		}
		X509CertificateHolder x509CertificateHolder = x509v3CertificateBuilder.build(contentSigner);

		byte[] encodedCertificate;
		try {
			encodedCertificate = x509CertificateHolder.getEncoded();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		CertificateFactory certificateFactory;
		try {
			certificateFactory = CertificateFactory.getInstance("X.509");
		} catch (CertificateException e) {
			throw new RuntimeException(e);
		}
		X509Certificate certificate;
		try {
			certificate = (X509Certificate) certificateFactory.generateCertificate(new ByteArrayInputStream(encodedCertificate));
		} catch (CertificateException e) {
			throw new RuntimeException(e);
		}
		return certificate;
	}

	@Test
	public void testKeyManagerFactory() throws Exception {
		Security.addProvider(new BeIDProvider());
		KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance("BeID");
		assertNotNull(keyManagerFactory);

		String algo = keyManagerFactory.getAlgorithm();
		System.out.println("key manager factory algo: " + algo);
		assertEquals("BeID", algo);

		KeyManager[] keyManagers = keyManagerFactory.getKeyManagers();
		assertNotNull(keyManagers);
	}
}
