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
import be.bosa.commons.eid.client.BeIDCardManager;
import be.bosa.commons.eid.client.event.BeIDCardEventsListener;
import be.bosa.commons.eid.client.event.CardEventsListener;
import org.junit.Test;

import javax.smartcardio.Card;
import javax.smartcardio.CardTerminal;
import java.math.BigInteger;

public class BeIDCardManagerExercise implements BeIDCardEventsListener, CardEventsListener {

	@Test
	public void testAsynchronous() throws InterruptedException {
		BeIDCardManager beIDCardManager = new BeIDCardManager(new TestLogger());
		beIDCardManager.addBeIDCardEventListener(this);
		beIDCardManager.addOtherCardEventListener(this);
		beIDCardManager.start();

		System.err.println("main thread running.. do some card tricks..");

		//noinspection InfiniteLoopStatement
		for (; ; ) {
			System.out.println(".");
			Thread.sleep(500);
		}
	}

	@Override
	public void eIDCardInserted(CardTerminal cardTerminal, BeIDCard card) {
		System.err.println("eID Card Inserted Into [" + StringUtils.getShortTerminalname(cardTerminal.getName()) + "]");
	}

	@Override
	public void eIDCardRemoved(CardTerminal cardTerminal, BeIDCard card) {
		System.err.println("eID Card Removed From [" + StringUtils.getShortTerminalname(cardTerminal.getName()) + "]");
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
		System.out.println("Other Card Events Initialised");

	}

	@Override
	public void eIDCardEventsInitialized() {
		System.out.println("BeID Card Events Initialised");
	}
}
