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

package be.bosa.commons.eid.client;

import be.bosa.commons.eid.client.event.BeIDCardEventsListener;
import be.bosa.commons.eid.client.event.CardTerminalEventsListener;
import be.bosa.commons.eid.client.impl.LocaleManager;
import be.bosa.commons.eid.client.impl.VoidLogger;
import be.bosa.commons.eid.client.spi.BeIDCardsUI;
import be.bosa.commons.eid.client.spi.Logger;
import be.bosa.commons.eid.client.spi.Sleeper;

import javax.smartcardio.CardTerminal;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * BeIDCards is a synchronous approach to Belgian Identity Cards and their
 * presence in the user's system, as opposed to the asynchronous, event-driven
 * approach of {@link BeIDCardManager} (but BeIDCards uses an underlying
 * {@link BeIDCardManager} to achieve it's goals). It's main purpose is to have
 * a very simple way to get a user's BeIDCard instance, abstracting away and
 * delegating issues such as terminal connection, card insertion, and handling
 * multiple eligible cards.
 * <p>
 * BeIDCards handle user interaction (if any) through an instance of
 * BeIDCardsUI, which can be supplied at construction, or left to the supplied
 * default, which will instantiate a be.bosa.commons.eid.dialogs.DefaultBeIDCardsUI
 * (which needs to be available on the class path)
 *
 * @author Frank Marien
 */
public class BeIDCards implements AutoCloseable {
	private static final String UI_MISSING_LOG_MESSAGE = "No BeIDCardsUI set and can't load DefaultBeIDCardsUI";
	private static final String DEFAULT_UI_IMPLEMENTATION = "be.bosa.commons.eid.dialogs.DefaultBeIDCardsUI";

	private final Logger logger;
	private final CardAndTerminalManager cardAndTerminalManager;
	private final BeIDCardManager cardManager;
	private final Map<CardTerminal, BeIDCard> beIDTerminalsAndCards;
	private final Sleeper terminalManagerInitSleeper, cardTerminalSleeper;
	private final Sleeper cardManagerInitSleeper, beIDSleeper;

	private BeIDCardsUI ui;
	private int cardTerminalsAttached;
	private boolean terminalsInitialized, cardsInitialized, uiSelectingCard;

	/**
	 * a BeIDCards without logging, using the default BeIDCardsUI
	 */
	public BeIDCards() {
		this(new VoidLogger(), null);
	}

	/**
	 * a BeIDCards without logging, using the supplied BeIDCardsUI
	 *
	 * @param ui an instance of BeIDCardsUI
	 *           that will be called upon for any user interaction required to
	 *           handle other calls. The UI's Locale will be used globally for
	 *           subsequent UI actions, as if setLocale() was called, except
	 *           where the Locale is explicity set for individual BeIDCard
	 *           instances.
	 */
	public BeIDCards(BeIDCardsUI ui) {
		this(new VoidLogger(), ui);
	}

	/**
	 * a BeIDCards logging to supplied logger, using the default BeIDCardsUI
	 *
	 * @param logger the logger instance
	 */
	public BeIDCards(Logger logger) {
		this(logger, null);
	}

	/**
	 * a BeIDCards logging to logger, using the supplied BeIDCardsUI and locale
	 *
	 * @param logger the logger instance
	 * @param ui     an instance of BeIDCardsUI that will be called upon for any
	 *               user interaction required to handle other calls. The UI's
	 *               Locale will be used globally for subsequent UI actions,
	 *               as if setLocale() was called, except where the Locale is
	 *               explicity set for individual BeIDCard instances.
	 */
	public BeIDCards(Logger logger, BeIDCardsUI ui) {
		this.logger = logger;

		this.cardAndTerminalManager = new CardAndTerminalManager(logger);
		this.cardAndTerminalManager.setProtocol(CardAndTerminalManager.Protocol.T0);
		this.cardManager = new BeIDCardManager(logger, cardAndTerminalManager);

		this.terminalManagerInitSleeper = new Sleeper();
		this.cardManagerInitSleeper = new Sleeper();
		this.cardTerminalSleeper = new Sleeper();
		this.beIDSleeper = new Sleeper();
		this.beIDTerminalsAndCards = Collections.synchronizedMap(new HashMap<>());

		this.terminalsInitialized = false;
		this.cardsInitialized = false;
		this.uiSelectingCard = false;

		setUI(ui);

		this.cardAndTerminalManager.addCardTerminalListener(new DefaultCardTerminalEventsListener());
		this.cardManager.addBeIDCardEventListener(new DefaultBeIDCardEventsListener());

		this.cardAndTerminalManager.start();
	}

	/**
	 * Return whether any BeID Cards are currently present.
	 *
	 * @return true if one or more BeID Cards are inserted in one or more
	 * connected CardTerminals, false if zero BeID Cards are present
	 */
	public boolean hasBeIDCards() {
		return hasBeIDCards(null);
	}

	/**
	 * Return whether any BeID Cards are currently present.
	 *
	 * @param terminal if not null, only this terminal will be considered in
	 *                 determining whether beID Cards are present.
	 * @return true if one or more BeID Cards are inserted in one or more
	 * connected CardTerminals, false if zero BeID Cards are present
	 */
	public boolean hasBeIDCards(CardTerminal terminal) {
		waitUntilCardsInitialized();

		boolean result = terminal != null ? beIDTerminalsAndCards.containsKey(terminal) : !beIDTerminalsAndCards.isEmpty();
		logger.debug("hasBeIDCards returns " + result);

		return result;
	}

	/**
	 * return Set of all BeID Cards present. Will return empty Set if no BeID
	 * cards are present at time of call
	 *
	 * @return a (possibly empty) set of all BeID Cards inserted at time of call
	 */
	public Set<BeIDCard> getAllBeIDCards() {
		waitUntilCardsInitialized();
		return new HashSet<>(beIDTerminalsAndCards.values());
	}

	/**
	 * return exactly one BeID Card.
	 * <p>
	 * This may block when called when no BeID Cards are present, until at least
	 * one BeID card is inserted, at which point this will be returned. If, at
	 * time of call, more than one BeID card is present, will request the UI to
	 * select between those, and return the selected card. If the UI is called
	 * upon to request the user to select between different cards, or to insert
	 * one card, and the user declines, CancelledException is thrown.
	 *
	 * @return a BeIDCard instance. The only one present, or one chosen out of several by the user
	 */
	public BeIDCard getOneBeIDCard() throws CancelledException {
		return getOneBeIDCard(null);
	}

	/**
	 * return a BeID Card inserted into a given CardTerminal
	 *
	 * @param terminal if not null, only BeID Cards in this particular CardTerminal
	 *                 will be considered.
	 *                 <p>
	 *                 May block when called when no BeID Cards are present, until at
	 *                 least one BeID card is inserted, at which point this will be
	 *                 returned. If, at time of call, more than one BeID card is
	 *                 present, will request the UI to select between those, and
	 *                 return the selected card. If the UI is called upon to request
	 *                 the user to select between different cards, or to insert one
	 *                 card, and the user declines, CancelledException is thrown.
	 * @return a BeIDCard instance. The only one present, or one chosen out of several by the user
	 */
	public BeIDCard getOneBeIDCard(CardTerminal terminal) throws CancelledException {
		BeIDCard selectedCard = null;

		do {
			waitForAtLeastOneCardTerminal();
			waitForAtLeastOneBeIDCard(terminal);

			// copy current list of BeID Cards to avoid holding a lock on it
			// during possible selectBeIDCard dialog.
			// (because we'd deadlock when user inserts/removes a card while
			// selectBeIDCard has not returned)
			Map<CardTerminal, BeIDCard> currentBeIDCards = new HashMap<>(beIDTerminalsAndCards);

			if (terminal != null) {
				// if selecting by terminal and we have a card in the requested one, return that immediately.
				// (This will return null if the terminal we want doesn't have a card, and continue the loop).
				selectedCard = currentBeIDCards.get(terminal);
			} else if (currentBeIDCards.size() == 1) {
				// we have only one BeID card. return it.
				selectedCard = currentBeIDCards.values().iterator().next();
			} else {
				// more than one, call upon the UI to obtain a selection
				try {
					logger.debug("selecting");
					uiSelectingCard = true;
					selectedCard = getUI().selectBeIDCard(currentBeIDCards.values());
				} catch (OutOfCardsException oocex) {
					// if we run out of cards, waitForAtLeastOneBeIDCard will ask for one in the next loop
				} finally {
					uiSelectingCard = false;
					logger.debug("no longer selecting");
				}
			}
		} while (selectedCard == null);

		return selectedCard;
	}

	/**
	 * wait for a particular BeID card to be removed. Note that this only works
	 * with BeID objects that were acquired using either the
	 * {@link #getOneBeIDCard()} or {@link #getAllBeIDCards()} methods from the
	 * same BeIDCards instance. If, at time of call, that particular card is
	 * present, the UI is called upon to prompt the user to remove that card.
	 *
	 * @return this BeIDCards instance to allow for method chaining
	 */
	public BeIDCards waitUntilCardRemoved(BeIDCard card) {
		if (getAllBeIDCards().contains(card)) {
			try {
				logger.debug("waitUntilCardRemoved blocking until card removed");
				getUI().adviseBeIDCardRemovalRequired();
				while (getAllBeIDCards().contains(card)) {
					beIDSleeper.sleepUntilAwakened();
				}
			} finally {
				getUI().adviseEnd();
			}
		}
		logger.debug("waitUntilCardRemoved returning");
		return this;
	}

	public boolean hasCardTerminals() {
		waitUntilTerminalsInitialized();
		return cardTerminalsAttached > 0;
	}

	/**
	 * Call close() if you no longer need this BeIDCards instance.
	 */
	@Override
	public void close() throws InterruptedException {
		this.cardAndTerminalManager.stop();
	}

	/**
	 * Set the Locale to use for subsequent UI operations. BeIDCards and
	 * BeIDCardManager share the same global Locale, so this will impact and and
	 * all instances of either. BeIDCard instances may have individual,
	 * per-instance Locale settings, however.
	 *
	 * @param newLocale will be used globally for subsequent UI actions, as if
	 *                  setLocale() was called, except where the Locale is explicity
	 *                  set for individual BeIDCard instances.
	 * @return this BeIDCards, to allow method chaining
	 */
	public BeIDCards setLocale(Locale newLocale) {
		LocaleManager.setLocale(newLocale);

		for (BeIDCard card : new HashSet<>(beIDTerminalsAndCards.values())) {
			card.setLocale(newLocale);
		}

		return this;
	}

	/**
	 * @return the currently set Locale
	 */
	public Locale getLocale() {
		return LocaleManager.getLocale();
	}

	private void setUI(BeIDCardsUI ui) {
		this.ui = ui;
		if (ui != null) {
			setLocale(ui.getLocale());
		}
	}

	private BeIDCardsUI getUI() {
		if (ui == null) {
			try {
				ClassLoader classLoader = BeIDCard.class.getClassLoader();
				Class<?> uiClass = classLoader.loadClass(DEFAULT_UI_IMPLEMENTATION);
				setUI((BeIDCardsUI) uiClass.newInstance());
			} catch (Exception e) {
				logger.error(UI_MISSING_LOG_MESSAGE);
				throw new UnsupportedOperationException(UI_MISSING_LOG_MESSAGE, e);
			}
		}

		return ui;
	}

	private void waitUntilCardsInitialized() {
		while (!cardsInitialized) {
			logger.debug("Waiting for CardAndTerminalManager Cards initialisation");
			cardManagerInitSleeper.sleepUntilAwakened();
			logger.debug("CardAndTerminalManager now has cards initialized");
		}
	}

	private void waitUntilTerminalsInitialized() {
		while (!terminalsInitialized) {
			logger.debug("Waiting for CardAndTerminalManager Terminals initialisation");
			terminalManagerInitSleeper.sleepUntilAwakened();
			logger.debug("CardAndTerminalManager now has terminals initialized");
		}
	}

	private void waitForAtLeastOneBeIDCard(CardTerminal terminal) {
		if (!hasBeIDCards(terminal)) {
			try {
				getUI().adviseBeIDCardRequired();
				while (!hasBeIDCards(terminal)) {
					beIDSleeper.sleepUntilAwakened();
				}
			} finally {
				getUI().adviseEnd();
			}
		}
	}

	private void waitForAtLeastOneCardTerminal() {
		if (!hasCardTerminals()) {
			try {
				getUI().adviseCardTerminalRequired();
				while (!hasCardTerminals()) {
					cardTerminalSleeper.sleepUntilAwakened();
				}
			} finally {
				getUI().adviseEnd();
			}

			// If we just found our first CardTerminal, give us 100ms to get notified about any eID cards that may
			// already present in that CardTerminal.
			// We'll get notified about any cards much faster than 100ms, and worst case, 100ms is not noticeable.
			// Better than calling adviseBeIDCardRequired and adviseEnd with a few seconds in between.
			if (!hasBeIDCards()) {
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					throw new RuntimeException(e);
				}
			}
		}
	}

	private class DefaultCardTerminalEventsListener implements CardTerminalEventsListener {

		@Override
		public void terminalEventsInitialized() {
			terminalsInitialized = true;
			terminalManagerInitSleeper.awaken();
		}

		@Override
		public void terminalDetached(CardTerminal cardTerminal) {
			cardTerminalsAttached--;
			cardTerminalSleeper.awaken();

		}

		@Override
		public void terminalAttached(CardTerminal cardTerminal) {
			cardTerminalsAttached++;
			cardTerminalSleeper.awaken();
		}
	}

	private class DefaultBeIDCardEventsListener implements BeIDCardEventsListener {
		@Override
		public void eIDCardInserted(CardTerminal cardTerminal, BeIDCard card) {
			logger.debug("eID Card Insertion Reported");

			if (uiSelectingCard) {
				try {
					getUI().eIDCardInsertedDuringSelection(card);
				} catch (Exception ex) {
					logger.error("Exception in UI:eIDCardInserted" + ex.getMessage());
				}
			}

			synchronized (beIDTerminalsAndCards) {
				beIDTerminalsAndCards.put(cardTerminal, card);
				beIDSleeper.awaken();
			}
		}

		@Override
		public void eIDCardRemoved(CardTerminal cardTerminal, BeIDCard card) {
			logger.debug("eID Card Removal Reported");

			if (uiSelectingCard) {
				try {
					getUI().eIDCardRemovedDuringSelection(card);
				} catch (Exception ex) {
					logger.error("Exception in UI:eIDCardRemoved" + ex.getMessage());
				}
			}

			synchronized (beIDTerminalsAndCards) {
				beIDTerminalsAndCards.remove(cardTerminal);
				beIDSleeper.awaken();
			}
		}

		@Override
		public void eIDCardEventsInitialized() {
			logger.debug("eIDCardEventsInitialized");
			cardsInitialized = true;
			cardManagerInitSleeper.awaken();
		}
	}
}
