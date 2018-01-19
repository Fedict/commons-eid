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

import org.junit.Test;

import java.util.Locale;
import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class MessagesTest {

	@Test
	public void getMessage() {
		Locale locale = Locale.getDefault();
		Messages messages = Messages.getInstance(locale);
		assertNotNull(messages.getMessage(Messages.MESSAGE_ID.INSERT_CARD_QUESTION));
		System.out.println("done msg: " + messages.getMessage(Messages.MESSAGE_ID.DONE));
	}

	@Test
	public void testFrenchMessages() {
		Locale locale = Locale.FRENCH;
		Messages messages = Messages.getInstance(locale);
		String message = messages.getMessage(Messages.MESSAGE_ID.GENERIC_ERROR);
		System.out.println("message: " + message);
		assertEquals("Erreur générale.", message);
	}

	@Test
	public void testGermanMessages() {
		Locale locale = Locale.GERMAN;
		Messages messages = Messages.getInstance(locale);
		String message = messages.getMessage(Messages.MESSAGE_ID.INSERT_CARD_QUESTION);
		System.out.println("message: " + message);
		System.out.println("connectReader: " + messages.getMessage(Messages.MESSAGE_ID.CONNECT_READER));
	}

	@Test
	public void testUnsupportedLanguage() {
		Locale.setDefault(new Locale("nl"));
		Locale locale = Locale.JAPANESE;
		Messages messages = Messages.getInstance(locale);
		String message = messages.getMessage(Messages.MESSAGE_ID.GENERIC_ERROR);
		System.out.println("message: " + message);
		assertEquals("Algemene fout.", message);
	}

	@Test
	public void testUnsupportedLanguageUnsupportedDefaultLanguage() {
		Locale.setDefault(Locale.CHINESE);
		Locale locale = Locale.JAPANESE;
		Messages messages = Messages.getInstance(locale);
		String message = messages.getMessage(Messages.MESSAGE_ID.GENERIC_ERROR);
		System.out.println("message: " + message);
		assertNotNull(message);
	}

	@Test
	public void testDefaultLocale() {
		Locale defaultLocale = Locale.FRENCH;
		Locale.setDefault(defaultLocale);
		Locale locale = Locale.ENGLISH;
		Messages messages = Messages.getInstance(locale);
		String message = messages.getMessage(Messages.MESSAGE_ID.GENERIC_ERROR);
		System.out.println("message: " + message);
		assertEquals("Generic Error.", message);
	}

	@Test
	public void allStringsAvailable() throws Exception {
		allStringsAvailable("en");
		allStringsAvailable("nl");
		allStringsAvailable("fr");
		allStringsAvailable("de");
	}

	private void allStringsAvailable(String language) throws Exception {
		if (!language.isEmpty()) {
			language = "_" + language;
		}

		Properties properties = new Properties();
		properties.load(MessagesTest.class.getResourceAsStream("Messages" + language + ".properties"));
		for (Messages.MESSAGE_ID messageId : Messages.MESSAGE_ID.values()) {
			assertTrue("Missing message \"" + messageId.getId() + "\" for language \"" + language + "\"", properties.containsKey(messageId.getId()));
		}
	}
}