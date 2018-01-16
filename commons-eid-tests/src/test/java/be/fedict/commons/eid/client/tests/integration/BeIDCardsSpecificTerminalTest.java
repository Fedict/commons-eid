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

package be.fedict.commons.eid.client.tests.integration;

import be.fedict.commons.eid.client.BeIDCard;
import be.fedict.commons.eid.client.BeIDCards;
import be.fedict.commons.eid.client.CancelledException;
import be.fedict.commons.eid.client.FileType;
import be.fedict.commons.eid.consumer.Identity;
import be.fedict.commons.eid.consumer.tlv.TlvParser;
import org.junit.Test;

import javax.smartcardio.CardTerminal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class BeIDCardsSpecificTerminalTest {
	
	@Test
	public void waitInsertAndRemove() throws Exception {
		System.out.println("creating beIDCards Instance");
		BeIDCards beIDCards = new BeIDCards(new TestLogger());
		assertNotNull(beIDCards);

		System.out.println("asking beIDCards Instance for One BeIDCard");
		try {
			BeIDCard beIDCard = beIDCards.getOneBeIDCard();
			assertNotNull(beIDCard);

			System.out.println("imprinting upon a particular CardTerminal");
			CardTerminal terminal = beIDCard.getCardTerminal();

			System.out.println("reading identity file");
			byte[] identityFile = beIDCard.readFile(FileType.Identity);
			Identity identity = TlvParser.parse(identityFile, Identity.class);
			System.out.println("card holder is " + identity.getFirstName() + " " + identity.getName());
			String userId = identity.getNationalNumber();

			if (beIDCards.getAllBeIDCards().contains(beIDCard)) {
				System.out.println("waiting for card removal");
				beIDCards.waitUntilCardRemoved(beIDCard);
			}

			System.out.println("We want only a card from our imprinted CardTerminal back");
			beIDCard = beIDCards.getOneBeIDCard(terminal);
			assertNotNull(beIDCard);

			identityFile = beIDCard.readFile(FileType.Identity);
			identity = TlvParser.parse(identityFile, Identity.class);
			assertEquals(userId, identity.getNationalNumber());
		} catch (CancelledException cex) {
			System.err.println("Cancelled By User");
		}

	}
}
