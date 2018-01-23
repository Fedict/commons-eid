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

package be.bosa.commons.eid.dialogs;

import javax.swing.*;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

/**
 * Util class to manage the i18n messages used within the BeIDCardUI.
 * 
 * @author Frank Cornelis
 * 
 */
public class Messages {

	private static final String RESOURCE_BUNDLE_NAME = Messages.class.getName();

	private static final Map<Locale, Messages> instances;

	static {
		instances = new HashMap<>();
	}

	private ResourceBundle resourceBundle;
	private Locale locale;

	/**
	 * Enumeration of all possible message identifiers.
	 * 
	 * @author Frank Cornelis
	 * 
	 */
	public enum MESSAGE_ID {
		EID_CLIENT("eidClient"),
		LOADING("loading"),
		SECURITY_ERROR("securityError"),
		CARD_ERROR("cardError"),
		GENERIC_ERROR("genericError"),
		DETECTING_CARD("detectingCard"),
		INSERT_CARD_QUESTION("insertCardQuestion"),
		READING_IDENTITY("readingIdentity"),
		TRANSMITTING_IDENTITY("transmittingIdentity"),
		DONE("done"),
		PRIVACY_QUESTION("privacyQuestion"),
		AUTHENTICATING("authenticating"),
		REMOVE_CARD("removeCard"),
		SIGNING("signing"),
		SELECT_FILES("selectFiles"),
		DIGESTING_FILES("digestingFiles"),
		COPY_ALL("copyAll"),
		MAIL("mail"),
		NO_MIDDLEWARE_ERROR("noMiddlewareError"),
		PIN_BLOCKED("pinBlocked"),
		PIN_CHANGED("pinChanged"),
		PIN_UNBLOCKED("pinUnblocked"),
		RETRIES_LEFT("retriesLeft"),
		PIN_INCORRECT("pinIncorrect"),
		CONNECT_READER("connectReader"),
		PIN_PAD("pinPad"),
		CURRENT_PIN("currentPin"),
		NEW_PIN("newPin"),
		OK("ok"),
		CANCEL("cancel"),
		PUK_PAD("pukPad"),
		PIN_PAD_CHANGE("pinPadChange"),
		KIOSK_MODE("kioskMode"),
		ENTER_PIN("enterPin"),
		LABEL_PIN("labelPin"),
		PIN_CHANGE("pinChange"),
		PIN_UNBLOCK("pinUnblock"),
		PIN_PAD_MODIFY_OLD("pinPadModifyOld"),
		PIN_PAD_MODIFY_NEW("pinPadModifyNew"),
		PIN_PAD_MODIFY_NEW_AGAIN("pinPadModifyNewAgain"),
		DIAGNOSTIC_MODE("diagnosticMode"),
		CERTIFICATE_EXPIRED_ERROR("certificateExpiredError"),
		CERTIFICATE_REVOKED_ERROR("certificateRevokedError"),
		IDENTITY_INFO("identityInfo"),
		IDENTITY_IDENTITY("identityIdentity"),
		IDENTITY_ADDRESS("identityAddress"),
		IDENTITY_PHOTO("identityPhoto"),
		DETAILS_BUTTON("detailsButtonText"),
		CANCEL_BUTTON("cancelButtonText"),
		NO_BUTTON("noButtonText"),
		OK_BUTTON("okButtonText"),
		YES_BUTTON("yesButtonText"),
		CERTIFICATE_NOT_TRUSTED("certificateNotTrusted"),
		PIN_REASON("pinReason"),
		USAGE("usage"),
		AUTHORIZATION_ERROR("authorizationError"),
		PROTOCOL_SIGNATURE("protocolSignature"),
		SIGNATURE_CREATION("signatureCreation"),
		SIGN_QUESTION("signQuestion"),
		SIGNATURE_ALGO("signatureAlgo");

		private final String id;

		MESSAGE_ID(String id) {
			this.id = id;
		}

		public String getId() {
			return this.id;
		}
	}

	public static Messages getInstance() {
		return getInstance(Locale.getDefault());
	}

	public static Messages getInstance(Locale locale) {
		return Messages.instances.computeIfAbsent(locale, Messages::new);
	}

	public String getMessage(MESSAGE_ID messageId) {
		return this.resourceBundle.getString(messageId.id);
	}

	public String getMessage(MESSAGE_ID messageId, String variant) {
		return this.resourceBundle.getString(messageId.id + "_" + variant);
	}

	public Locale getLocale() {
		return locale;
	}

	private Messages(Locale locale) {
		this.locale = locale;
		ResourceBundle bundle;
		try {
			bundle = ResourceBundle.getBundle(RESOURCE_BUNDLE_NAME, locale);
		} catch (MissingResourceException mre) {
			bundle = ResourceBundle.getBundle(RESOURCE_BUNDLE_NAME, Locale.ENGLISH);
		}
		this.resourceBundle = bundle;

		UIManager.put("OptionPane.cancelButtonText", this.getMessage(MESSAGE_ID.CANCEL_BUTTON));
		UIManager.put("OptionPane.noButtonText", this.getMessage(MESSAGE_ID.NO_BUTTON));
		UIManager.put("OptionPane.okButtonText", this.getMessage(MESSAGE_ID.OK_BUTTON));
		UIManager.put("OptionPane.yesButtonText", this.getMessage(MESSAGE_ID.YES_BUTTON));
	}

}
