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

package be.bosa.commons.eid.examples.events;

import be.bosa.commons.eid.client.BeIDCard;
import be.bosa.commons.eid.client.BeIDCards;
import be.bosa.commons.eid.client.CancelledException;
import be.bosa.commons.eid.client.FileType;
import be.bosa.commons.eid.client.exception.BeIDException;
import be.bosa.commons.eid.consumer.Identity;
import be.bosa.commons.eid.consumer.tlv.TlvParser;

import java.util.Set;

public class BeIDCardsExample {

	public static void main(String[] args) throws InterruptedException {
		BeIDCardsExample examples = new BeIDCardsExample();
		examples.demonstrate();
	}

	public void demonstrate() throws InterruptedException {
		BeIDCards beIDCardsWithDefaultSettings = new BeIDCards();

		Set<BeIDCard> cards = beIDCardsWithDefaultSettings.getAllBeIDCards();
		System.out.println(cards.size() + " BeID Cards found");

		try {
			BeIDCard card = beIDCardsWithDefaultSettings.getOneBeIDCard();

			try {
				byte[] idData = card.readFile(FileType.Identity);
				Identity id = TlvParser.parse(idData, Identity.class);
				System.out.println(id.firstName + "'s card");
			} catch (BeIDException cex) {
				cex.printStackTrace();
			}

			System.out.println("Please remove the card now.");
			beIDCardsWithDefaultSettings.waitUntilCardRemoved(card);
			System.out.println("Thank you.");
		} catch (CancelledException cex) {
			System.out.println("Cancelled By User");
		}
	}

}
