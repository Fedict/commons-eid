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

package be.fedict.commons.eid.jca;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.KeyManagerFactorySpi;
import javax.net.ssl.ManagerFactoryParameters;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyStore;

/**
 * eID specific {@link KeyManagerFactory}. Can be used for mutual TLS
 * authentication.
 * <br>
 * Usage:
 * <p>
 * <pre>
 * import javax.net.ssl.KeyManagerFactory;
 * import javax.net.ssl.SSLContext;
 * ...
 * KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(&quot;BeID&quot;);
 * SSLContext sslContext = SSLContext.getInstance(&quot;TLS&quot;);
 * sslContext.init(keyManagerFactory.getKeyManagers(), ..., ...);
 * </pre>
 *
 * @author Frank Cornelis
 * @see BeIDX509KeyManager
 * @see BeIDManagerFactoryParameters
 */
public class BeIDKeyManagerFactory extends KeyManagerFactorySpi {

	private static final Log LOG = LogFactory.getLog(BeIDKeyManagerFactory.class);

	private BeIDManagerFactoryParameters beIDSpec;

	@Override
	protected void engineInit(ManagerFactoryParameters spec)
			throws InvalidAlgorithmParameterException {
		LOG.debug("engineInit(spec)");
		if (null == spec) {
			return;
		}
		if (!(spec instanceof BeIDManagerFactoryParameters)) {
			throw new InvalidAlgorithmParameterException();
		}
		beIDSpec = (BeIDManagerFactoryParameters) spec;
	}

	@Override
	protected void engineInit(KeyStore keyStore, char[] password) {
		LOG.debug("engineInit(KeyStore,password)");
	}

	@Override
	protected KeyManager[] engineGetKeyManagers() {
		LOG.debug("engineGetKeyManagers");
		KeyManager beidKeyManager;
		try {
			beidKeyManager = new BeIDX509KeyManager(beIDSpec);
		} catch (GeneralSecurityException | IOException e) {
			throw new IllegalStateException(e);
		}
		return new KeyManager[]{beidKeyManager};
	}
}
