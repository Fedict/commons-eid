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

package be.fedict.commons.eid.examples.events;

import java.math.BigInteger;
import javax.smartcardio.Card;
import javax.smartcardio.CardTerminal;
import be.fedict.commons.eid.client.CardAndTerminalManager;
import be.fedict.commons.eid.client.event.CardEventsListener;

public class CardEventsExamples {

	/*
	 * get information about Cards being inserted and removed, while doing
	 * something else:
	 */
	public static void main(String[] args) throws InterruptedException {
		CardEventsExamples examples = new CardEventsExamples();
		examples.cardTerminalsBasicAsynchronous();
	}

	public void cardTerminalsBasicAsynchronous() throws InterruptedException {
		CardAndTerminalManager cardAndTerminalManager = new CardAndTerminalManager();

		cardAndTerminalManager.addCardListener(new CardEventsListener() {
			@Override
			public void cardInserted(CardTerminal cardTerminal, Card card) {
				if (card != null) {
					System.err.println("Card [" + String.format("%x", new BigInteger(1, card.getATR().getBytes())) + "] Inserted Into Terminal [" + cardTerminal.getName() + "]");
				} else {
					System.err.println("Card present but failed to connect()");
				}
			}

			@Override
			public void cardRemoved(CardTerminal cardTerminal) {
				System.err.println("Card Removed From [" + cardTerminal.getName() + "]");
			}

			@Override
			public void cardEventsInitialized() {
				System.out.println("From now on you'll see Cards being Inserted/Removed");
			}
		});

		System.out.println("First, you'll see Inserted events for Cards that were already inserted");

		cardAndTerminalManager.start();

		//noinspection InfiniteLoopStatement
		for (;;) {
			Thread.sleep(2000);
		}
	}
}
