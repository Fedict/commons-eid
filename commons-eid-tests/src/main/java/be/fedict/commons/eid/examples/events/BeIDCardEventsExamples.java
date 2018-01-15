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

import javax.smartcardio.CardTerminal;
import be.fedict.commons.eid.client.BeIDCard;
import be.fedict.commons.eid.client.BeIDCardManager;
import be.fedict.commons.eid.client.event.BeIDCardEventsListener;

/*
 * Get information about BeID cards being inserted and removed, while doing
 * something else:
 */
public class BeIDCardEventsExamples {

	public static void main(String[] args) throws InterruptedException {
		BeIDCardEventsExamples examples = new BeIDCardEventsExamples();
		examples.demonstrateBasicAsynchronousUsage();
	}

	public void demonstrateBasicAsynchronousUsage() throws InterruptedException {
		BeIDCardManager beIDCardManagerWithDefaultSettings = new BeIDCardManager();

		beIDCardManagerWithDefaultSettings.addBeIDCardEventListener(new BeIDCardEventsListener() {
			@Override
			public void eIDCardRemoved(CardTerminal cardTerminal, BeIDCard card) {
				System.out.println("BeID Card Removed From Card Terminal [" + cardTerminal.getName() + "]\n");
			}

			@Override
			public void eIDCardInserted(CardTerminal cardTerminal, BeIDCard card) {
				System.out.println("BeID Card Inserted Into Card Terminal [" + cardTerminal.getName() + "]\n");
			}

			@Override
			public void eIDCardEventsInitialized() {
				System.out.println("From now on you'll see BeID Cards being Inserted/Removed");
			}
		});

		System.out.println("First, you'll see Inserted events for BeID Cards that were already inserted");

		beIDCardManagerWithDefaultSettings.start();

		//noinspection InfiniteLoopStatement
		for (;;) {
			Thread.sleep(2000);
		}
	}
}
