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

package be.fedict.commons.eid.examples.events;

import be.fedict.commons.eid.client.BeIDCard;
import be.fedict.commons.eid.client.BeIDCardManager;
import be.fedict.commons.eid.client.CardAndTerminalManager;
import be.fedict.commons.eid.client.event.BeIDCardEventsListener;
import be.fedict.commons.eid.client.event.CardEventsListener;
import be.fedict.commons.eid.client.event.CardTerminalEventsListener;

import javax.smartcardio.Card;
import javax.smartcardio.CardTerminal;
import java.math.BigInteger;

/*
 * mixed asynchronous detection of CardTerminals, BeID and non-BeID cards,
 * using a BeIDCardManager with your own CardAndTerminalManager
 */
public class MixedDetectionExamples implements BeIDCardEventsListener, CardEventsListener, CardTerminalEventsListener {

	public static void main(String[] args) throws InterruptedException {
		new MixedDetectionExamples().demonstrate();
	}

	private void demonstrate() throws InterruptedException {
		CardAndTerminalManager cardAndTerminalManager = new CardAndTerminalManager();

		BeIDCardManager beIDCardManager = new BeIDCardManager(cardAndTerminalManager);
		beIDCardManager.addBeIDCardEventListener(this);
		beIDCardManager.addOtherCardEventListener(this);

		cardAndTerminalManager.addCardTerminalListener(this);

		System.out.println("First, you'll see events for terminals and Cards that were already present");

		beIDCardManager.start();
		cardAndTerminalManager.start();

		//noinspection InfiniteLoopStatement
		for (;;) {
			Thread.sleep(2000);
		}
	}

	@Override
	public void terminalAttached(CardTerminal cardTerminal) {
		System.out.println("CardTerminal [" + cardTerminal.getName() + "] attached\n");
	}

	@Override
	public void terminalDetached(CardTerminal cardTerminal) {
		System.out.println("CardTerminal [" + cardTerminal.getName() + "] detached\n");
	}

	@Override
	public void terminalEventsInitialized() {
		System.out.println("From now on you'll see terminals being Attached/Detached");
	}

	@Override
	public void eIDCardRemoved(CardTerminal cardTerminal, BeIDCard card) {
		System.out.println("BeID Card Removed From Card Termimal [" + cardTerminal.getName() + "]\n");
	}

	@Override
	public void eIDCardInserted(CardTerminal cardTerminal, BeIDCard card) {
		System.out.println("BeID Card Inserted Into Card Termimal [" + cardTerminal.getName() + "]\n");
	}

	@Override
	public void eIDCardEventsInitialized() {
		System.out.println("From now on you'll see BeID Cards being Inserted/Removed");
	}

	@Override
	public void cardInserted(CardTerminal cardTerminal, Card card) {
		if (card != null) {
			System.out.println("Other Card [" + String.format("%x", new BigInteger(1, card.getATR().getBytes())) + "] Inserted Into Terminal [" + cardTerminal.getName() + "]");
		} else {
			System.out.println("Other Card Inserted Into Terminal [" + cardTerminal.getName() + "] but failed to connect()");
		}
	}

	@Override
	public void cardRemoved(CardTerminal cardTerminal) {
		System.out.println("Other Card Removed From [" + cardTerminal.getName() + "]");
	}

	@Override
	public void cardEventsInitialized() {
		System.out.println("From now on you'll see Non-BeID Cards being Inserted/Removed");
	}

}
