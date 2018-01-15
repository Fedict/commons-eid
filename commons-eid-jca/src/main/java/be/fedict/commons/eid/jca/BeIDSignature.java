/*
 * Commons eID Project.
 * Copyright (C) 2008-2013 FedICT.
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

import java.io.ByteArrayOutputStream;
import java.security.InvalidKeyException;
import java.security.InvalidParameterException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.SignatureSpi;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * eID based JCA {@link Signature} implementation. Supports the following
 * signature algorithms:
 * <ul>
 * <li><code>SHA1withRSA</code></li>
 * <li><code>SHA224withRSA</code></li>
 * <li><code>SHA256withRSA</code></li>
 * <li><code>SHA384withRSA</code></li>
 * <li><code>SHA512withRSA</code></li>
 * <li><code>NONEwithRSA</code>, used for mutual TLS authentication.</li>
 * <li><code>RIPEMD128withRSA</code></li>
 * <li><code>RIPEMD160withRSA</code></li>
 * <li><code>RIPEMD256withRSA</code></li>
 * <li><code>SHA1withRSAandMGF1</code>, supported by future eID cards.</li>
 * <li><code>SHA256withRSAandMGF1</code>, supported by future eID cards.</li>
 * </ul>
 * <br>
 * Some of the more exotic digest algorithms like SHA-224 and RIPEMDxxx will
 * require an additional security provider like BouncyCastle.
 * 
 * @author Frank Cornelis
 * 
 */
public class BeIDSignature extends SignatureSpi {

	private static Log LOG = LogFactory.getLog(BeIDSignature.class);
	private final static Map<String, String> digestAlgos;

	private final MessageDigest messageDigest;
	private BeIDPrivateKey privateKey;
	private Signature verifySignature;
	private final String signatureAlgorithm;
	private final ByteArrayOutputStream precomputedDigestOutputStream;

	static {
		digestAlgos = new HashMap<>();
		digestAlgos.put("SHA1withRSA", "SHA-1");
		digestAlgos.put("SHA224withRSA", "SHA-224");
		digestAlgos.put("SHA256withRSA", "SHA-256");
		digestAlgos.put("SHA384withRSA", "SHA-384");
		digestAlgos.put("SHA512withRSA", "SHA-512");
		digestAlgos.put("NONEwithRSA", null);
		digestAlgos.put("RIPEMD128withRSA", "RIPEMD128");
		digestAlgos.put("RIPEMD160withRSA", "RIPEMD160");
		digestAlgos.put("RIPEMD256withRSA", "RIPEMD256");
		digestAlgos.put("SHA1withRSAandMGF1", "SHA-1");
		digestAlgos.put("SHA256withRSAandMGF1", "SHA-256");
	}

	BeIDSignature(String signatureAlgorithm) throws NoSuchAlgorithmException {
		LOG.debug("constructor: " + signatureAlgorithm);

		this.signatureAlgorithm = signatureAlgorithm;
		if (!digestAlgos.containsKey(signatureAlgorithm)) {
			LOG.error("no such algo: " + signatureAlgorithm);
			throw new NoSuchAlgorithmException(signatureAlgorithm);
		}

		String digestAlgo = digestAlgos.get(signatureAlgorithm);
		if (digestAlgo != null) {
			this.messageDigest = MessageDigest.getInstance(digestAlgo);
			this.precomputedDigestOutputStream = null;
		} else {
			LOG.debug("NONE message digest");
			this.messageDigest = null;
			this.precomputedDigestOutputStream = new ByteArrayOutputStream();
		}
	}

	@Override
	protected void engineInitVerify(PublicKey publicKey) throws InvalidKeyException {
		LOG.debug("engineInitVerify");
		if (null == verifySignature) {
			try {
				verifySignature = Signature.getInstance(signatureAlgorithm);
			} catch (NoSuchAlgorithmException nsaex) {
				throw new InvalidKeyException("no such algo: " + nsaex.getMessage(), nsaex);
			}
		}
		verifySignature.initVerify(publicKey);
	}

	@Override
	protected void engineInitSign(PrivateKey privateKey)
			throws InvalidKeyException {
		LOG.debug("engineInitSign");

		if (!(privateKey instanceof BeIDPrivateKey)) {
			throw new InvalidKeyException();
		}
		this.privateKey = (BeIDPrivateKey) privateKey;

		if (null != messageDigest) {
			messageDigest.reset();
		}
	}

	@Override
	protected void engineUpdate(byte b) throws SignatureException {
		messageDigest.update(b);
		if (null != verifySignature) {
			verifySignature.update(b);
		}
	}

	@Override
	protected void engineUpdate(byte[] b, int off, int len) throws SignatureException {
		if (null != messageDigest) {
			messageDigest.update(b, off, len);
		}
		if (null != precomputedDigestOutputStream) {
			precomputedDigestOutputStream.write(b, off, len);
		}
		if (null != verifySignature) {
			verifySignature.update(b, off, len);
		}
	}

	@Override
	protected byte[] engineSign() throws SignatureException {
		LOG.debug("engineSign");
		byte[] digestValue;
		String digestAlgo;
		if (null != messageDigest) {
			digestValue = messageDigest.digest();
			digestAlgo = messageDigest.getAlgorithm();
			if (signatureAlgorithm.endsWith("andMGF1")) {
				digestAlgo += "-PSS";
			}
		} else if (null != precomputedDigestOutputStream) {
			digestValue = precomputedDigestOutputStream.toByteArray();
			digestAlgo = "NONE";
		} else {
			throw new SignatureException();
		}
		return privateKey.sign(digestValue, digestAlgo);
	}

	@Override
	protected boolean engineVerify(byte[] sigBytes) throws SignatureException {
		LOG.debug("engineVerify");
		if (null == verifySignature) {
			throw new SignatureException("initVerify required");
		}

		return verifySignature.verify(sigBytes);
	}

	@Override
	@Deprecated
	protected void engineSetParameter(String param, Object value) throws InvalidParameterException {
	}

	@Override
	@Deprecated
	protected Object engineGetParameter(String param) throws InvalidParameterException {
		return null;
	}
}
