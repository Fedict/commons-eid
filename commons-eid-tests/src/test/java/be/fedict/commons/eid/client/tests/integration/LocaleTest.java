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

package be.fedict.commons.eid.client.tests.integration;

import be.fedict.commons.eid.client.BeIDCard;
import be.fedict.commons.eid.client.BeIDCards;
import be.fedict.commons.eid.client.FileType;
import be.fedict.commons.eid.client.impl.BeIDDigest;
import be.fedict.commons.eid.client.spi.BeIDCardsUI;
import be.fedict.commons.eid.dialogs.DefaultBeIDCardsUI;
import org.junit.Test;

import java.util.Locale;

public class LocaleTest {

	@Test
	public void testLocale() throws Exception {
		try (BeIDCards cards = new BeIDCards()) {
			cards.setLocale(Locale.FRENCH);
			try (BeIDCard card = cards.getOneBeIDCard()) {
				card.sign(new byte[]{0x00, 0x00, 0x00, 0x00}, BeIDDigest.PLAIN_TEXT, FileType.NonRepudiationCertificate, false);
			}
		}

		BeIDCardsUI ui = new DefaultBeIDCardsUI();
		ui.setLocale(Locale.GERMAN);
		try (BeIDCards cards = new BeIDCards(ui) ) {
			try (BeIDCard card = cards.getOneBeIDCard()) {
				card.sign(new byte[]{0x00, 0x00, 0x00, 0x00}, BeIDDigest.PLAIN_TEXT, FileType.NonRepudiationCertificate, false);

				card.setLocale(new Locale("nl"));
				card.sign(new byte[]{0x00, 0x00, 0x00, 0x00}, BeIDDigest.PLAIN_TEXT, FileType.NonRepudiationCertificate, false);
			}
		}
	}
}
