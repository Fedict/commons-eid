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

import be.fedict.commons.eid.client.CardAndTerminalManager;
import be.fedict.commons.eid.client.event.CardEventsListener;
import be.fedict.commons.eid.client.event.CardTerminalEventsListener;
import be.fedict.commons.eid.client.tests.integration.simulation.SimulatedCard;
import be.fedict.commons.eid.client.tests.integration.simulation.SimulatedCardTerminal;
import be.fedict.commons.eid.client.tests.integration.simulation.SimulatedCardTerminals;
import org.junit.Before;
import org.junit.Test;

import javax.smartcardio.ATR;
import javax.smartcardio.Card;
import javax.smartcardio.CardTerminal;
import java.util.*;

import static org.junit.Assert.assertEquals;

public class CardAndTerminalManagerTests {

	private static final int NUMBER_OF_TERMINALS = 16;
	private static final int NUMBER_OF_CARDS = 16;

	private List<SimulatedCard> simulatedBeIDCard;
	private List<SimulatedCardTerminal> simulatedCardTerminal;
	private SimulatedCardTerminals simulatedCardTerminals;

	@Before
	public void setUp() {
		simulatedBeIDCard = new ArrayList<>(NUMBER_OF_CARDS);
		for (int i = 0; i < NUMBER_OF_CARDS; i++) {
			simulatedBeIDCard.add(new SimulatedCard(new ATR(new byte[]{
					0x3b, (byte) 0x98, (byte) i, 0x40, (byte) i, (byte) i, (byte) i, (byte) i, 0x01, 0x01, (byte) 0xad, 0x13, 0x10
			})));
		}

		simulatedCardTerminal = new ArrayList<>(NUMBER_OF_TERMINALS);
		for (int i = 0; i < NUMBER_OF_TERMINALS; i++) {
			simulatedCardTerminal.add(new SimulatedCardTerminal("Fedix SCR " + i));
		}

		simulatedCardTerminals = new SimulatedCardTerminals();
	}

	private class RecordKeepingCardTerminalEventsListener implements CardTerminalEventsListener {
		private final Set<CardTerminal> recordedState;

		public RecordKeepingCardTerminalEventsListener() {
			recordedState = new HashSet<>();
		}

		@Override
		public synchronized void terminalAttached(CardTerminal cardTerminal) {
			recordedState.add(cardTerminal);

		}

		@Override
		public synchronized void terminalDetached(CardTerminal cardTerminal) {
			recordedState.remove(cardTerminal);

		}

		public synchronized Set<CardTerminal> getRecordedState() {
			return new HashSet<>(recordedState);
		}

		@Override
		public void terminalEventsInitialized() {
		}
	}

	private class RecordKeepingCardEventsListener implements CardEventsListener {
		private final Map<CardTerminal, Card> recordedState;

		public RecordKeepingCardEventsListener() {
			recordedState = new HashMap<>();
		}

		@Override
		public synchronized void cardInserted(CardTerminal cardTerminal, Card card) {
			if (recordedState.containsKey(cardTerminal)) {
				throw new IllegalStateException("Cannot Insert 2 Cards in 1 CardTerminal");
			}
			recordedState.put(cardTerminal, card);
		}

		@Override
		public synchronized void cardRemoved(CardTerminal cardTerminal) {
			if (!recordedState.containsKey(cardTerminal)) {
				throw new IllegalStateException("Cannot Remove Card That is not There");
			}
			recordedState.remove(cardTerminal);
		}

		public synchronized Map<CardTerminal, Card> getRecordedState() {
			return recordedState;
		}

		@Override
		public void cardEventsInitialized() {
		}
	}

	@Test
	public void testTerminalAttachDetachDetection() throws InterruptedException {
		Random random = new Random(0);
		Set<CardTerminal> expectedState = new HashSet<>();

		CardAndTerminalManager cardAndTerminalManager = new CardAndTerminalManager(new TestLogger(), simulatedCardTerminals);
		RecordKeepingCardTerminalEventsListener recorder = new RecordKeepingCardTerminalEventsListener();
		cardAndTerminalManager.addCardTerminalListener(recorder);
		cardAndTerminalManager.addCardTerminalListener(new NPEProneCardTerminalEventsListener());
		cardAndTerminalManager.start();

		System.err.println("attaching and detaching some simulated cardterminals");

		ArrayList<SimulatedCardTerminal> terminalsToExercise = new ArrayList<>(simulatedCardTerminal);
		Set<SimulatedCardTerminal> detachedTerminalSet = new HashSet<>(terminalsToExercise);
		Set<SimulatedCardTerminal> attachedTerminalSet = new HashSet<>();

		for (int i = 0; i < 1000; i++) {
			for (int j = 0; j < random.nextInt(NUMBER_OF_TERMINALS); j++) {
				SimulatedCardTerminal terminalToAttach = terminalsToExercise.get(random.nextInt(NUMBER_OF_TERMINALS));
				if (detachedTerminalSet.contains(terminalToAttach)) {
					expectedState.add(terminalToAttach);
					simulatedCardTerminals.attachCardTerminal(terminalToAttach);
					detachedTerminalSet.remove(terminalToAttach);
					attachedTerminalSet.add(terminalToAttach);
					System.out.println("attached [" + terminalToAttach.getName() + "]");
					StringUtils.printTerminalSet(expectedState);
					StringUtils.printTerminalSet(recorder.getRecordedState());
				}
			}

			for (int j = 0; j < random.nextInt(NUMBER_OF_TERMINALS); j++) {
				SimulatedCardTerminal terminalToDetach = terminalsToExercise.get(random.nextInt(NUMBER_OF_TERMINALS));
				if (attachedTerminalSet.contains(terminalToDetach)) {
					expectedState.remove(terminalToDetach);
					simulatedCardTerminals.detachCardTerminal(terminalToDetach);
					detachedTerminalSet.add(terminalToDetach);
					attachedTerminalSet.remove(terminalToDetach);
					System.out.println("detached [" + terminalToDetach.getName() + "]");
					StringUtils.printTerminalSet(expectedState);
					StringUtils.printTerminalSet(recorder.getRecordedState());
				}
			}
		}

		Thread.sleep(1000);
		cardAndTerminalManager.stop();
		assertEquals(expectedState, recorder.getRecordedState());
	}

	@Test
	public void testCardInsertRemoveDetection() throws Exception {
		Random random = new Random(0);
		Map<SimulatedCardTerminal, SimulatedCard> expectedState = new HashMap<>();
		CardAndTerminalManager cardAndTerminalManager = new CardAndTerminalManager(new TestLogger(), simulatedCardTerminals);
		RecordKeepingCardEventsListener recorder = new RecordKeepingCardEventsListener();
		cardAndTerminalManager.addCardListener(recorder);
		cardAndTerminalManager.addCardListener(new NPEProneCardEventsListener());
		cardAndTerminalManager.start();

		ArrayList<SimulatedCardTerminal> terminalsToExercise = new ArrayList<>(simulatedCardTerminal);
		Set<SimulatedCardTerminal> emptyTerminalSet = new HashSet<>(terminalsToExercise);
		Set<SimulatedCardTerminal> fullTerminalSet = new HashSet<>();

		ArrayList<SimulatedCard> cardsToExercise = new ArrayList<>(simulatedBeIDCard);
		Set<SimulatedCard> unusedCardSet = new HashSet<>(cardsToExercise);
		Set<SimulatedCard> usedCardSet = new HashSet<>();

		System.err.println("attaching some simulated card readers");
		for (SimulatedCardTerminal terminal : emptyTerminalSet) {
			simulatedCardTerminals.attachCardTerminal(terminal);
		}

		System.err.println("inserting and removing some simulated cards");
		for (int i = 0; i < 1000; i++) {
			for (int j = 0; j < random.nextInt(NUMBER_OF_CARDS); j++) {
				SimulatedCardTerminal terminalToInsertCardInto = terminalsToExercise.get(random.nextInt(NUMBER_OF_TERMINALS));
				SimulatedCard cardToInsert = cardsToExercise.get(random.nextInt(NUMBER_OF_CARDS));

				if (emptyTerminalSet.contains(terminalToInsertCardInto) && unusedCardSet.contains(cardToInsert)) {
					expectedState.put(terminalToInsertCardInto, cardToInsert);
					terminalToInsertCardInto.insertCard(cardToInsert);
					emptyTerminalSet.remove(terminalToInsertCardInto);
					fullTerminalSet.add(terminalToInsertCardInto);
					unusedCardSet.remove(cardToInsert);
					usedCardSet.add(cardToInsert);
					System.out.println("inserted [" + StringUtils.atrToString(cardToInsert.getATR()) + "] into [" + terminalToInsertCardInto.getName() + "]");
				}
			}

			for (int j = 0; j < random.nextInt(NUMBER_OF_CARDS); j++) {
				SimulatedCardTerminal terminalToRemoveCardFrom = terminalsToExercise.get(random.nextInt(NUMBER_OF_TERMINALS));
				SimulatedCard cardToRemove = expectedState.get(terminalToRemoveCardFrom);

				if (fullTerminalSet.contains(terminalToRemoveCardFrom) && usedCardSet.contains(cardToRemove)) {
					expectedState.remove(terminalToRemoveCardFrom);
					terminalToRemoveCardFrom.removeCard();
					emptyTerminalSet.add(terminalToRemoveCardFrom);
					fullTerminalSet.remove(terminalToRemoveCardFrom);
					usedCardSet.remove(cardToRemove);
					unusedCardSet.add(cardToRemove);
					System.out.println("removed [" + StringUtils.atrToString(cardToRemove.getATR()) + "] from [" + terminalToRemoveCardFrom.getName() + "]");
				}
			}
		}

		Thread.sleep(1000);
		cardAndTerminalManager.stop();
		assertEquals(expectedState, recorder.getRecordedState());
	}

	private class NPEProneCardTerminalEventsListener implements CardTerminalEventsListener {
		@Override
		public void terminalAttached(CardTerminal cardTerminal) {
			throw new NullPointerException("Fake NPE attempting to trash a CardTerminalEventsListener");
		}

		@Override
		public void terminalDetached(CardTerminal cardTerminal) {
			throw new NullPointerException("Fake NPE attempting to trash a CardTerminalEventsListener");
		}

		@Override
		public void terminalEventsInitialized() {
			System.out.println("Terminal Events Initialised");
		}
	}

	private class NPEProneCardEventsListener implements CardEventsListener {
		@Override
		public void cardInserted(CardTerminal cardTerminal, Card card) {
			throw new NullPointerException("Fake NPE attempting to trash a CardEventsListener");
		}

		@Override
		public void cardRemoved(CardTerminal cardTerminal) {
			throw new NullPointerException("Fake NPE attempting to trash a CardEventsListener");
		}

		@Override
		public void cardEventsInitialized() {
			System.out.println("Card Events Initialised");
		}
	}
}
