/*
 * Commons eID Project.
 * Copyright (C) 2008-2013 FedICT.
 * Copyright (C) 2014 e-Contract.be BVBA.
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

package be.fedict.commons.eid.jca;

import be.fedict.commons.eid.client.BeIDCard;
import be.fedict.commons.eid.client.BeIDCards;
import be.fedict.commons.eid.client.CancelledException;
import be.fedict.commons.eid.client.FileType;
import be.fedict.commons.eid.client.impl.VoidLogger;
import be.fedict.commons.eid.client.spi.BeIDCardUI;
import be.fedict.commons.eid.client.spi.BeIDCardsUI;
import be.fedict.commons.eid.client.spi.Logger;
import be.fedict.commons.eid.dialogs.DefaultBeIDCardUI;
import be.fedict.commons.eid.dialogs.DefaultBeIDCardsUI;
import be.fedict.commons.eid.dialogs.Messages;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.smartcardio.CardTerminal;
import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.*;
import java.security.KeyStore.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.List;

/**
 * eID based JCA {@link KeyStore}. Used to load eID key material via standard
 * JCA API calls. Once the JCA security provider has been registered you have a
 * new key store available named "BeID". Two key aliases are available:
 * <ul>
 * <li>"Authentication" which gives you access to the eID authentication private
 * key and corresponding certificate chain.</li>
 * <li>"Signature" which gives you access to the eID non-repudiation private key
 * and corresponding certificate chain.</li>
 * </ul>
 * Further the Citizen CA certificate can be accessed via the "CA" alias, the
 * Root CA certificate can be accessed via the "Root" alias, and the national
 * registration certificate can be accessed via the "RRN" alias.
 * <br>
 * Supports the eID specific {@link BeIDKeyStoreParameter} key store parameter.
 * You can also let any {@link JFrame} implement the
 * {@link KeyStore.LoadStoreParameter} interface. If you pass this to
 * {@link KeyStore#load(LoadStoreParameter)} the keystore will use that Swing
 * frame as parent for positioning the dialogs.
 * <br>
 * Usage:
 * <br>
 * 
 * <pre>
 * import java.security.KeyStore;
 * import java.security.cert.X509Certificate;
 * import java.security.PrivateKey;
 * 
 * ...
 * KeyStore keyStore = KeyStore.getInstance("BeID");
 * keyStore.load(null);
 * X509Certificate authnCertificate = (X509Certificate) keyStore
 * 			.getCertificate("Authentication");
 * PrivateKey authnPrivateKey = (PrivateKey) keyStore.getKey(
 * 			"Authentication", null);
 * Certificate[] signCertificateChain = keyStore.getCertificateChain("Signature");
 * </pre>
 * 
 * @author Frank Cornelis
 * @see BeIDKeyStoreParameter
 * @see BeIDProvider
 */
public class BeIDKeyStore extends KeyStoreSpi {

	private static final String ALIAS_AUTHENTICATION = "Authentication";
	private static final String ALIAS_SIGNATURE = "Signature";
	private static final String ALIAS_CA = "CA";
	private static final String ALIAS_ROOT = "Root";
	private static final String ALIAS_RRN = "RRN";
	private static final List<String> ALIASES = Arrays.asList(ALIAS_AUTHENTICATION, ALIAS_SIGNATURE, ALIAS_CA, ALIAS_ROOT, ALIAS_RRN);

	private static final Log LOG = LogFactory.getLog(BeIDKeyStore.class);

	private BeIDKeyStoreParameter keyStoreParameter;
	private BeIDCard beIDCard;

	private List<X509Certificate> authnCertificateChain;
	private List<X509Certificate> signCertificateChain;
	private List<X509Certificate> rrnCertificateChain;
	private X509Certificate citizenCaCertificate;
	private X509Certificate rootCaCertificate;
	private X509Certificate authnCertificate;
	private X509Certificate signCertificate;
	private X509Certificate rrnCertificate;
	private CardTerminal cardTerminal;

	@Override
	public Key engineGetKey(String alias, char[] password) {
		LOG.debug("engineGetKey: " + alias);
		BeIDCard beIDCard = getBeIDCard();
		boolean logoff;
		boolean autoRecovery;

		if (null == keyStoreParameter) {
			logoff = false;
			autoRecovery = false;
		} else {
			logoff = keyStoreParameter.getLogoff();
			autoRecovery = keyStoreParameter.getAutoRecovery();
		}

		if (ALIAS_AUTHENTICATION.equals(alias)) {
			return new BeIDPrivateKey(FileType.AuthentificationCertificate, beIDCard, logoff, autoRecovery, this);
		}

		if (ALIAS_SIGNATURE.equals(alias)) {
			return new BeIDPrivateKey(FileType.NonRepudiationCertificate, beIDCard, logoff, autoRecovery, this);
		}

		return null;
	}

	@Override
	public Certificate[] engineGetCertificateChain(String alias) {
		LOG.debug("engineGetCertificateChain: " + alias);
		BeIDCard beIDCard = getBeIDCard();

		if (ALIAS_SIGNATURE.equals(alias)) {
			try {
				if (signCertificateChain == null) {
					signCertificateChain = beIDCard.getSigningCertificateChain();
					signCertificate = signCertificateChain.get(0);
					citizenCaCertificate = signCertificateChain.get(1);
					rootCaCertificate = signCertificateChain.get(2);
				}
			} catch (Exception ex) {
				LOG.error("error: " + ex.getMessage(), ex);
				return null;
			}
			return signCertificateChain.toArray(new X509Certificate[]{});
		}

		if (ALIAS_AUTHENTICATION.equals(alias)) {
			try {
				if (null == authnCertificateChain) {
					authnCertificateChain = beIDCard.getAuthenticationCertificateChain();
					authnCertificate = authnCertificateChain.get(0);
					citizenCaCertificate = authnCertificateChain.get(1);
					rootCaCertificate = authnCertificateChain.get(2);
				}
			} catch (Exception ex) {
				LOG.error("error: " + ex.getMessage(), ex);
				return null;
			}
			return authnCertificateChain.toArray(new X509Certificate[]{});
		}

		if (ALIAS_RRN.equals(alias)) {
			if (null == rrnCertificateChain) {
				try {
					rrnCertificateChain = beIDCard.getRRNCertificateChain();
				} catch (Exception e) {
					LOG.error("error: " + e.getMessage(), e);
					return null;
				}
				rrnCertificate = rrnCertificateChain.get(0);
				rootCaCertificate = rrnCertificateChain.get(1);
			}
			return rrnCertificateChain.toArray(new X509Certificate[]{});
		}

		return null;
	}

	@Override
	public Certificate engineGetCertificate(String alias) {
		LOG.debug("engineGetCertificate: " + alias);
		BeIDCard beIDCard = getBeIDCard();

		if (ALIAS_SIGNATURE.equals(alias)) {
			try {
				if (null == signCertificate) {
					signCertificate = beIDCard.getSigningCertificate();
				}
			} catch (Exception ex) {
				LOG.warn("error: " + ex.getMessage(), ex);
				return null;
			}
			return signCertificate;
		}

		if (ALIAS_AUTHENTICATION.equals(alias)) {
			try {
				if (null == authnCertificate) {
					authnCertificate = beIDCard
							.getAuthenticationCertificate();
				}
			} catch (Exception ex) {
				LOG.warn("error: " + ex.getMessage(), ex);
				return null;
			}
			return authnCertificate;
		}

		if (ALIAS_CA.equals(alias)) {
			try {
				if (null == citizenCaCertificate) {
					citizenCaCertificate = beIDCard.getCACertificate();
				}
			} catch (Exception e) {
				LOG.warn("error: " + e.getMessage(), e);
				return null;
			}
			return citizenCaCertificate;
		}

		if (ALIAS_ROOT.equals(alias)) {
			try {
				if (null == rootCaCertificate) {
					rootCaCertificate = beIDCard.getRootCACertificate();
				}
			} catch (Exception e) {
				LOG.warn("error: " + e.getMessage(), e);
				return null;
			}
			return rootCaCertificate;
		}

		if (ALIAS_RRN.equals(alias)) {
			try {
				if (null == rrnCertificate) {
					rrnCertificate = beIDCard.getRRNCertificate();
				}
			} catch (Exception e) {
				LOG.warn("error: " + e.getMessage(), e);
				return null;
			}
			return rrnCertificate;
		}
		return null;
	}

	@Override
	public Date engineGetCreationDate(String alias) {
		X509Certificate certificate = (X509Certificate) engineGetCertificate(alias);
		if (null == certificate) {
			return null;
		}
		return certificate.getNotBefore();
	}

	@Override
	public void engineSetKeyEntry(String alias, Key key, char[] password, Certificate[] chain) throws KeyStoreException {
		throw new KeyStoreException();
	}

	@Override
	public void engineSetKeyEntry(String alias, byte[] key, Certificate[] chain) throws KeyStoreException {
		throw new KeyStoreException();
	}

	@Override
	public void engineSetCertificateEntry(String alias, Certificate cert) throws KeyStoreException {
		throw new KeyStoreException();
	}

	@Override
	public void engineDeleteEntry(String alias) throws KeyStoreException {
		throw new KeyStoreException();
	}

	@Override
	public Enumeration<String> engineAliases() {
		LOG.debug("engineAliases");
		return Collections.enumeration(ALIASES);
	}

	@Override
	public boolean engineContainsAlias(String alias) {
		LOG.debug("engineContainsAlias: " + alias);
		return ALIASES.contains(alias);
	}

	@Override
	public int engineSize() {
		return 2;
	}

	@Override
	public boolean engineIsKeyEntry(String alias) {
		LOG.debug("engineIsKeyEntry: " + alias);
		return ALIAS_AUTHENTICATION.equals(alias) || ALIAS_SIGNATURE.equals(alias);
	}

	@Override
	public boolean engineIsCertificateEntry(String alias) {
		LOG.debug("engineIsCertificateEntry: " + alias);
		return ALIAS_ROOT.equals(alias) || ALIAS_CA.equals(alias) || ALIAS_RRN.equals(alias);
	}

	@Override
	public void engineStore(LoadStoreParameter param) throws IOException,
			NoSuchAlgorithmException, CertificateException {
		LOG.debug("engineStore");
		super.engineStore(param);
	}

	@Override
	public Entry engineGetEntry(String alias, ProtectionParameter protParam)
			throws KeyStoreException, NoSuchAlgorithmException,
			UnrecoverableEntryException {
		LOG.debug("engineGetEntry: " + alias);
		if (ALIAS_AUTHENTICATION.equals(alias) || ALIAS_SIGNATURE.equals(alias)) {
			PrivateKey privateKey = (PrivateKey) engineGetKey(alias, null);
			Certificate[] chain = engineGetCertificateChain(alias);
			return new PrivateKeyEntry(privateKey, chain);
		}

		if (ALIAS_CA.equals(alias) || ALIAS_ROOT.equals(alias) || ALIAS_RRN.equals(alias)) {
			Certificate certificate = engineGetCertificate(alias);
			return new TrustedCertificateEntry(certificate);
		}

		return super.engineGetEntry(alias, protParam);
	}

	@Override
	public void engineSetEntry(String alias, Entry entry, ProtectionParameter protParam) throws KeyStoreException {
		LOG.debug("engineSetEntry: " + alias);
		super.engineSetEntry(alias, entry, protParam);
	}

	@Override
	public boolean engineEntryInstanceOf(String alias, Class<? extends Entry> entryClass) {
		LOG.debug("engineEntryInstanceOf: " + alias);
		return super.engineEntryInstanceOf(alias, entryClass);
	}

	@Override
	public String engineGetCertificateAlias(Certificate cert) {
		return null;
	}

	@Override
	public void engineStore(OutputStream stream, char[] password) {
	}

	@Override
	public void engineLoad(InputStream stream, char[] password) {
	}

	@Override
	public void engineLoad(LoadStoreParameter param) throws
			NoSuchAlgorithmException {
		LOG.debug("engineLoad"); /* Allows for a KeyStore to be re-loaded several times. */
		this.beIDCard = null;
		this.authnCertificateChain = null;
		this.signCertificateChain = null;
		this.rrnCertificateChain = null;
		this.authnCertificate = null;
		this.signCertificate = null;
		this.citizenCaCertificate = null;
		this.rootCaCertificate = null;
		this.rrnCertificate = null;
		if (null == param) {
			return;
		}
		if (param instanceof BeIDKeyStoreParameter) {
			this.keyStoreParameter = (BeIDKeyStoreParameter) param;
			return;
		}
		if (param instanceof JFrame) {
			this.keyStoreParameter = new BeIDKeyStoreParameter();
			JFrame frame = (JFrame) param;
			this.keyStoreParameter.setParentComponent(frame);
			return;
		}
		throw new NoSuchAlgorithmException();
	}

	private BeIDCard getBeIDCard() {
		return getBeIDCard(false);
	}

	public BeIDCard getBeIDCard(boolean recover) {
		boolean cardReaderStickiness = keyStoreParameter != null && keyStoreParameter.getCardReaderStickiness();
		if (recover) {
			LOG.debug("recovering from error");
			beIDCard = null;
		}
		if (null != beIDCard) {
			return beIDCard;
		}
		if (null != keyStoreParameter) {
			beIDCard = keyStoreParameter.getBeIDCard();
		}
		if (null != beIDCard) {
			return beIDCard;
		}
		Component parentComponent;
		Locale locale;
		Logger logger;
		if (null != keyStoreParameter) {
			parentComponent = keyStoreParameter.getParentComponent();
			locale = keyStoreParameter.getLocale();
			logger = keyStoreParameter.getLogger();
		} else {
			parentComponent = null;
			locale = null;
			logger = null;
		}
		if (null == locale) {
			locale = Locale.getDefault();
		}
		if (null == logger) {
			logger = new VoidLogger();
		}
		Messages messages = Messages.getInstance(locale);
		BeIDCardsUI ui = new DefaultBeIDCardsUI(parentComponent, messages);
		BeIDCards beIDCards = new BeIDCards(logger, ui);
		beIDCards.setLocale(locale);
		try {
			CardTerminal stickyCardTerminal;
			if (cardReaderStickiness) {
				stickyCardTerminal = cardTerminal;
			} else {
				stickyCardTerminal = null;
			}
			beIDCard = beIDCards.getOneBeIDCard(stickyCardTerminal);
			if (cardReaderStickiness) {
				cardTerminal = beIDCard.getCardTerminal();
				LOG.debug("sticky card reader: " + cardTerminal.getName());
			}
			BeIDCardUI userInterface = new DefaultBeIDCardUI(parentComponent, messages);
			beIDCard.setUI(userInterface);
		} catch (CancelledException cex) {
			throw new SecurityException("user cancelled");
		}
		if (null == beIDCard) {
			throw new SecurityException("missing eID card");
		}
		return beIDCard;
	}
}
