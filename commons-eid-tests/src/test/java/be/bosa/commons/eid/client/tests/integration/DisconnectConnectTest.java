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

package be.bosa.commons.eid.client.tests.integration;

import be.bosa.commons.eid.client.BeIDCard;
import be.bosa.commons.eid.client.BeIDCards;
import be.bosa.commons.eid.client.FileType;
import be.bosa.commons.eid.consumer.Identity;
import be.bosa.commons.eid.consumer.tlv.TlvParser;
import org.junit.Test;

import javax.smartcardio.CardException;
import javax.swing.*;
import java.io.IOException;

/**
 * See also: https://groups.google.com/forum/#!topic/eid-applet/N1mVFnYJ3VM
 *
 * @author Frank Cornelis
 */
public class DisconnectConnectTest {

	@Test
	public void testDisconnectConnect() throws Exception {
		JOptionPane.showMessageDialog(null, "Connect card");
		try (BeIDCards cards = new BeIDCards(new TestLogger())) {
			try (BeIDCard card = cards.getAllBeIDCards().iterator().next()) {
				Identity identity = readIdentity(card);
				JOptionPane.showMessageDialog(null, "Card read: " + identity.getFirstName());
			}
		}

		JOptionPane.showMessageDialog(null,"Disconnect and reconnect the reader") ;
		try (BeIDCards cards = new BeIDCards()) {
			try (BeIDCard card = cards.getAllBeIDCards().iterator().next()) {
				Identity identity = readIdentity(card);
				JOptionPane.showMessageDialog(null, "Card read: " + identity.getFirstName());
			}
		}
	}

	private Identity readIdentity(BeIDCard card) throws CardException,
			IOException, InterruptedException {
		byte[] idData = card.readFile(FileType.Identity);
		return TlvParser.parse(idData, Identity.class);
	}
}
