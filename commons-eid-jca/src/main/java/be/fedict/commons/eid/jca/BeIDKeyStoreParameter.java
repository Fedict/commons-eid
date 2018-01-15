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

import java.awt.Component;
import java.security.KeyStore;
import java.security.KeyStore.ProtectionParameter;
import java.util.Locale;

import be.fedict.commons.eid.client.BeIDCard;
import be.fedict.commons.eid.client.spi.Logger;

/**
 * An eID specific {@link KeyStore} parameter. Used to influence how the eID
 * card should be handled. If no {@link BeIDKeyStoreParameter} is used for
 * loading the keystore, a default behavior will be used.
 * <br>
 * Usage:
 * <br>
 * 
 * <pre>
 * import java.security.KeyStore;
 * ...
 * KeyStore keyStore = KeyStore.getInstance("BeID");
 * BeIDKeyStoreParameter keyStoreParameter = new BeIDKeyStoreParameter();
 * keyStoreParameter.set...
 * keyStore.load(keyStoreParameter);
 * </pre>
 * 
 * @author Frank Cornelis
 * @see KeyStore
 * @see BeIDKeyStore
 */
public class BeIDKeyStoreParameter implements KeyStore.LoadStoreParameter {

	private BeIDCard beIDCard;
	private boolean logoff;
	private Component parentComponent;
	private Locale locale;
	private boolean autoRecovery;
	private boolean cardReaderStickiness;
	private Logger logger;

	@Override
	public ProtectionParameter getProtectionParameter() {
		return null;
	}

	/**
	 * Sets the {@link BeIDCard} to be used by the corresponding
	 * {@link KeyStore}.
	 */
	public void setBeIDCard(BeIDCard beIDCard) {
		this.beIDCard = beIDCard;
	}

	public BeIDCard getBeIDCard() {
		return beIDCard;
	}

	/**
	 * Set to <code>true</code> if you want an eID logoff to be issued after
	 * each PIN entry.
	 */
	public void setLogoff(boolean logoff) {
		this.logoff = logoff;
	}

	public boolean getLogoff() {
		return logoff;
	}

	/**
	 * Sets the parent component used to position the default eID dialogs.
	 */
	public void setParentComponent(Component parentComponent) {
		this.parentComponent = parentComponent;
	}

	public Component getParentComponent() {
		return parentComponent;
	}

	/**
	 * Sets the locale used for the default eID dialogs.
	 */
	public void setLocale(Locale locale) {
		this.locale = locale;
	}

	public Locale getLocale() {
		return locale;
	}

	public boolean getAutoRecovery() {
		return autoRecovery;
	}

	/**
	 * Sets whether the private keys retrieved from the key store should feature
	 * auto-recovery. This means that they can survive eID card
	 * removal/re-insert events.
	 */
	public void setAutoRecovery(boolean autoRecovery) {
		this.autoRecovery = autoRecovery;
	}

	public boolean getCardReaderStickiness() {
		return cardReaderStickiness;
	}

	/**
	 * Sets whether the auto recovery should use card reader stickiness. If set
	 * to true, the auto recovery will try to recover using the same card
	 * reader.
	 */
	public void setCardReaderStickiness(boolean cardReaderStickiness) {
		this.cardReaderStickiness = cardReaderStickiness;
	}

	/**
	 * Sets the logger to be used within the BeIDCard sub-system.
	 */
	public void setLogger(Logger logger) {
		this.logger = logger;
	}

	public Logger getLogger() {
		return logger;
	}
}
