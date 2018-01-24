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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.net.ssl.KeyManagerFactory;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.Provider;
import java.security.SecureRandom;
import java.security.Signature;
import java.util.HashMap;
import java.util.Map;

/**
 * The JCA security provider. Provides an eID based {@link KeyStore},
 * {@link Signature}, {@link KeyManagerFactory}, and {@link SecureRandom}.
 * <br>
 * Usage:
 * 
 * <pre>
 * import java.security.Security;
 * import BeIDProvider;
 * 
 * ...
 * Security.addProvider(new BeIDProvider());
 * </pre>
 * 
 * @see BeIDKeyStore
 * @see BeIDSignature
 * @see BeIDKeyManagerFactory
 * @see BeIDSecureRandom
 * @author Frank Cornelis
 * 
 */
public class BeIDProvider extends Provider {

	public static final String NAME = "BeIDProvider";
	private static final Log LOG = LogFactory.getLog(BeIDProvider.class);

	public BeIDProvider() {
		super(NAME, 1.0, "BeID Provider");

		putService(new BeIDService(this, "KeyStore", "BeID", BeIDKeyStore.class.getName()));

		Map<String, String> signatureServiceAttributes = new HashMap<>();
		signatureServiceAttributes.put("SupportedKeyClasses", BeIDPrivateKey.class.getName());
		putService(new BeIDService(this, "Signature", "SHA1withRSA", BeIDSignature.class.getName(), signatureServiceAttributes));
		putService(new BeIDService(this, "Signature", "SHA224withRSA", BeIDSignature.class.getName(), signatureServiceAttributes));
		putService(new BeIDService(this, "Signature", "SHA256withRSA", BeIDSignature.class.getName(), signatureServiceAttributes));
		putService(new BeIDService(this, "Signature", "SHA384withRSA", BeIDSignature.class.getName(), signatureServiceAttributes));
		putService(new BeIDService(this, "Signature", "SHA512withRSA", BeIDSignature.class.getName(), signatureServiceAttributes));
		putService(new BeIDService(this, "Signature", "NONEwithRSA", BeIDSignature.class.getName(), signatureServiceAttributes));
		putService(new BeIDService(this, "Signature", "RIPEMD128withRSA", BeIDSignature.class.getName(), signatureServiceAttributes));
		putService(new BeIDService(this, "Signature", "RIPEMD160withRSA", BeIDSignature.class.getName(), signatureServiceAttributes));
		putService(new BeIDService(this, "Signature", "RIPEMD256withRSA", BeIDSignature.class.getName(), signatureServiceAttributes));
		putService(new BeIDService(this, "Signature", "SHA1withRSAandMGF1", BeIDSignature.class.getName(), signatureServiceAttributes));
		putService(new BeIDService(this, "Signature", "SHA256withRSAandMGF1", BeIDSignature.class.getName(), signatureServiceAttributes));

		putService(new BeIDService(this, "KeyManagerFactory", "BeID", BeIDKeyManagerFactory.class.getName()));

		putService(new BeIDService(this, "SecureRandom", "BeID", BeIDSecureRandom.class.getName()));
	}

	/**
	 * Inner class used by {@link BeIDProvider}.
	 * 
	 * @author Frank Cornelis
	 * 
	 */
	private static final class BeIDService extends Service {

		public BeIDService(Provider provider, String type, String algorithm, String className) {
			super(provider, type, algorithm, className, null, null);
		}

		public BeIDService(Provider provider, String type, String algorithm, String className, Map<String, String> attributes) {
			super(provider, type, algorithm, className, null, attributes);
		}

		@Override
		public Object newInstance(Object constructorParameter) throws NoSuchAlgorithmException {
			LOG.debug("newInstance: " + super.getType());
			if (super.getType().equals("Signature")) {
				return new BeIDSignature(getAlgorithm());
			}
			return super.newInstance(constructorParameter);
		}

		@Override
		public boolean supportsParameter(Object parameter) {
			LOG.debug("supportedParameter: " + parameter);
			return super.supportsParameter(parameter);
		}
	}
}
