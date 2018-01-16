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

import be.bosa.commons.eid.client.CardAndTerminalManager;
import be.bosa.commons.eid.client.event.CardTerminalEventsListener;

import javax.smartcardio.CardTerminal;

public class TerminalEventsExamples {

	public static void main(String[] args) throws InterruptedException {
		TerminalEventsExamples examples = new TerminalEventsExamples();
		examples.cardTerminalsBasicAsynchronous();
	}

	public void cardTerminalsBasicAsynchronous() throws InterruptedException {
		CardAndTerminalManager cardAndTerminalManager = new CardAndTerminalManager();

		cardAndTerminalManager.addCardTerminalListener(new CardTerminalEventsListener() {
			@Override
			public void terminalDetached(CardTerminal cardTerminal) {
				System.out.println("CardTerminal [" + cardTerminal.getName() + "] detached\n");
			}

			@Override
			public void terminalAttached(CardTerminal cardTerminal) {
				System.out.println("CardTerminal [" + cardTerminal.getName() + "] attached\n");
			}

			@Override
			public void terminalEventsInitialized() {
				System.out.println("From now on you'll see terminals being Attached/Detached");
			}
		});

		System.out.println("First, you'll see Attach events for CardTerminals that were already attached");

		cardAndTerminalManager.start();

		//noinspection InfiniteLoopStatement
		for (; ; ) {
			Thread.sleep(2000);
		}
	}
}
