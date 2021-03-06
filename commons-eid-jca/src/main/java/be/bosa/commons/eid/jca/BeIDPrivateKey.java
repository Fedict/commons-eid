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

package be.bosa.commons.eid.jca;

import be.bosa.commons.eid.client.BeIDCard;
import be.bosa.commons.eid.client.FileType;
import be.bosa.commons.eid.client.impl.BeIDDigest;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.security.PrivateKey;
import java.security.SignatureException;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;

/**
 * eID based JCA private key. Should not be used directly, but via the
 * {@link BeIDKeyStore}.
 * 
 * @author Frank Cornelis
 * @see BeIDKeyStore
 */
public class BeIDPrivateKey implements PrivateKey {

	private static final Log LOG = LogFactory.getLog(BeIDPrivateKey.class);

	private final FileType certificateFileType;
	private BeIDCard beIDCard;
	private final boolean logoff;
	private final boolean autoRecovery;
	private final BeIDKeyStore beIDKeyStore;
	private final static Map<String, BeIDDigest> beIDDigests;
	private X509Certificate authenticationCertificate;

	static {
		beIDDigests = new HashMap<>();
		beIDDigests.put("SHA-1", BeIDDigest.SHA_1);
		beIDDigests.put("SHA-224", BeIDDigest.SHA_224);
		beIDDigests.put("SHA-256", BeIDDigest.SHA_256);
		beIDDigests.put("SHA-384", BeIDDigest.SHA_384);
		beIDDigests.put("SHA-512", BeIDDigest.SHA_512);
		beIDDigests.put("NONE", BeIDDigest.NONE);
		beIDDigests.put("RIPEMD128", BeIDDigest.RIPEMD_128);
		beIDDigests.put("RIPEMD160", BeIDDigest.RIPEMD_160);
		beIDDigests.put("RIPEMD256", BeIDDigest.RIPEMD_256);
		beIDDigests.put("SHA-1-PSS", BeIDDigest.SHA_1_PSS);
		beIDDigests.put("SHA-256-PSS", BeIDDigest.SHA_256_PSS);
	}

	/**
	 * Main constructor.
	 */
	public BeIDPrivateKey(FileType certificateFileType, BeIDCard beIDCard, boolean logoff, boolean autoRecovery, BeIDKeyStore beIDKeyStore) {
		LOG.debug("constructor: " + certificateFileType);
		this.certificateFileType = certificateFileType;
		this.beIDCard = beIDCard;
		this.logoff = logoff;
		this.autoRecovery = autoRecovery;
		this.beIDKeyStore = beIDKeyStore;
	}

	@Override
	public String getAlgorithm() {
		return "RSA";
	}

	@Override
	public String getFormat() {
		return null;
	}

	@Override
	public byte[] getEncoded() {
		return null;
	}

	byte[] sign(byte[] digestValue, String digestAlgo) throws SignatureException {
		LOG.debug("auto recovery: " + autoRecovery);

		BeIDDigest beIDDigest = beIDDigests.get(digestAlgo);
		if (null == beIDDigest) {
			throw new SignatureException("unsupported algo: " + digestAlgo);
		}

		try {
			if (autoRecovery) {
				/*
				 * We keep a copy of the authentication certificate to make sure
				 * that the automatic recovery only operates against the same
				 * eID card.
				 */
				if (authenticationCertificate == null) {
					try {
						authenticationCertificate = beIDCard.getAuthenticationCertificate();
					} catch (Exception e) {
						// don't fail here
					}
				}
			}
			try {
				return beIDCard.sign(digestValue, beIDDigest, certificateFileType, false);
			} catch (Exception e) {
				if (autoRecovery) {
					LOG.debug("trying to recover...");
					beIDCard = beIDKeyStore.getBeIDCard(true);
					if (null != authenticationCertificate) {
						X509Certificate newAuthenticationCertificate = beIDCard.getAuthenticationCertificate();
						if (!authenticationCertificate.equals(newAuthenticationCertificate)) {
							throw new SignatureException("different eID card");
						}
					}
				}
				return beIDCard.sign(digestValue, beIDDigest, certificateFileType, false);
			} finally {
				if (logoff) {
					beIDCard.logoff();
				}
			}
		} catch (Exception ex) {
			throw new SignatureException(ex);
		}
	}
}
