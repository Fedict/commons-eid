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

package be.bosa.commons.eid.client.tests.integration.simulation;

import javax.smartcardio.CardException;
import javax.smartcardio.CardTerminal;
import javax.smartcardio.CardTerminals;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SimulatedCardTerminals extends CardTerminals {
	private final Set<SimulatedCardTerminal> terminals;

	public SimulatedCardTerminals() {
		this.terminals = new HashSet<>();
	}

	public synchronized void attachCardTerminal(SimulatedCardTerminal terminal) {
		terminal.setTerminals(this);
		this.terminals.add(terminal);
		notifyAll();
	}

	public synchronized void detachCardTerminal(SimulatedCardTerminal terminal) {
		terminal.setTerminals(null);
		this.terminals.remove(terminal);
		notifyAll();
	}

	public synchronized void propagateCardEvent() {
		notifyAll();
	}

	@Override
	public synchronized List<CardTerminal> list(State state) throws CardException {
		switch (state) {
			case ALL:
				return Collections.unmodifiableList(new ArrayList<>(this.terminals));

			case CARD_PRESENT: {
				ArrayList<CardTerminal> presentList = new ArrayList<>();
				for (CardTerminal terminal : this.terminals) {
					if (terminal.isCardPresent()) {
						presentList.add(terminal);
					}
				}
				return Collections.unmodifiableList(presentList);
			}

			case CARD_ABSENT: {
				ArrayList<CardTerminal> absentList = new ArrayList<>();
				for (CardTerminal terminal : this.terminals) {
					if (!terminal.isCardPresent()) {
						absentList.add(terminal);
					}
				}
				return Collections.unmodifiableList(absentList);
			}

			default:
				throw new CardException("list with CARD_INSERTION or CARD_REMOVAL not supported in SimulatedCardTerminals");
		}
	}

	@Override
	public synchronized boolean waitForChange(long timeout) {
		try {
			wait(timeout);
		} catch (InterruptedException iex) {
			return false;
		}
		return true;
	}
}