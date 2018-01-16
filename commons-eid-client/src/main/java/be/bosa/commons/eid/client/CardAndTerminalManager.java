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

import be.bosa.commons.eid.client.event.CardEventsListener;
import be.bosa.commons.eid.client.event.CardTerminalEventsListener;
import be.bosa.commons.eid.client.impl.LibJ2PCSCGNULinuxFix;
import be.bosa.commons.eid.client.impl.VoidLogger;
import be.bosa.commons.eid.client.spi.Logger;

import javax.smartcardio.Card;
import javax.smartcardio.CardException;
import javax.smartcardio.CardTerminal;
import javax.smartcardio.CardTerminals;
import javax.smartcardio.CardTerminals.State;
import javax.smartcardio.TerminalFactory;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * A CardAndTerminalManager maintains an active state overview of all
 * javax.smartcardio.CardTerminal attached to a system's pcsc subsystem, and
 * notifies registered:
 * <ul>
 * <li>CardTerminalEventsListeners of any CardTerminals Attached or Detached
 * <li>CardEventsListeners of any Cards inserted into or removed from any
 * attached CardTerminals
 * </ul>
 * Note that at the level of CardAndTerminalManager there is no distinction
 * between types of cards or terminals: They are merely reported using the
 * standard javax.smartcardio classes.
 *
 * @author Frank Marien
 */
public class CardAndTerminalManager implements Runnable {

	private static final int DEFAULT_DELAY = 250;

	private boolean running, subSystemInitialized, autoconnect;
	private Thread worker;
	private Set<CardTerminal> terminalsPresent, terminalsWithCards;
	private final CardTerminals cardTerminals;
	private final Set<String> terminalsToIgnoreCardEventsFor;
	private final Set<CardTerminalEventsListener> cardTerminalEventsListeners;
	private final Set<CardEventsListener> cardEventsListeners;
	private int delay;
	private final Logger logger;
	private Protocol protocol;

	public enum Protocol {
		T0("T=0"), T1("T=1"), TCL("T=CL"), ANY("*");

		private final String protocol;

		Protocol(String protocol) {
			this.protocol = protocol;
		}

		String getProtocol() {
			return protocol;
		}
	}

	/**
	 * Instantiate a CardAndTerminalManager working on the standard smartcardio
	 * CardTerminals, and without any logging.
	 */
	public CardAndTerminalManager() {
		this(new VoidLogger());
	}

	/**
	 * Instantiate a CardAndTerminalManager working on the standard smartcardio
	 * CardTerminals, and logging to the Logger implementation given.
	 *
	 * @param logger the logger instance
	 */
	public CardAndTerminalManager(Logger logger) {
		this(logger, null);
	}

	/**
	 * Instantiate a CardAndTerminalManager working on a specific CardTerminals
	 * instance and without any logging. In normal operation, you would use the
	 * constructor that takes no CardTerminals parameter, but using this one you
	 * could, for example obtain a CardTerminals instance from a different
	 * TerminalFactory, or from your own implementation.
	 *
	 * @param cardTerminals instance to obtain terminal and card events from
	 */
	public CardAndTerminalManager(CardTerminals cardTerminals) {
		this(new VoidLogger(), cardTerminals);
	}

	/**
	 * Instantiate a CardAndTerminalManager working on a specific CardTerminals
	 * instance, and that logs to the given Logger.In normal operation, you
	 * would use the constructor that takes no CardTerminals parameter, but
	 * using this one you could, for example obtain a CardTerminals instance
	 * from a different TerminalFactory, or from your own implementation.
	 *
	 * @param logger        the logger instance
	 * @param cardTerminals instance to obtain terminal and card events from
	 */
	public CardAndTerminalManager(Logger logger, CardTerminals cardTerminals) {
		// work around implementation bug in some GNU/Linux JRE's that causes libpcsc not to be found.
		LibJ2PCSCGNULinuxFix.fixNativeLibrary(logger);

		this.cardTerminalEventsListeners = Collections.synchronizedSet(new HashSet<>());
		this.cardEventsListeners = Collections.synchronizedSet(new HashSet<>());
		this.terminalsToIgnoreCardEventsFor = Collections.synchronizedSet(new HashSet<>());
		this.delay = DEFAULT_DELAY;
		this.logger = logger;
		this.running = false;
		this.subSystemInitialized = false;
		this.autoconnect = true;
		this.protocol = Protocol.ANY;

		if (cardTerminals == null) {
			TerminalFactory terminalFactory = TerminalFactory
					.getDefault();
			this.cardTerminals = terminalFactory.terminals();
		} else {
			this.cardTerminals = cardTerminals;
		}
	}

	/**
	 * Register a CardTerminalEventsListener instance. This will subsequently be
	 * called for any Terminal Attaches/Detaches on CardTerminals that we're not
	 * ignoring
	 *
	 * @param listener the CardTerminalEventsListener to be registered
	 * @return this CardAndTerminalManager to allow for method chaining.
	 * @see #ignoreCardEventsFor(String)
	 */
	public CardAndTerminalManager addCardTerminalListener(CardTerminalEventsListener listener) {
		cardTerminalEventsListeners.add(listener);
		return this;
	}

	/**
	 * Register a CardEventsListener instance. This will subsequently be called
	 * for any Card Inserts/Removals on CardTerminals that we're not ignoring
	 *
	 * @param listener the CardEventsListener to be registered
	 * @return this CardAndTerminalManager to allow for method chaining.
	 * @see #ignoreCardEventsFor(String)
	 */
	public CardAndTerminalManager addCardListener(CardEventsListener listener) {
		cardEventsListeners.add(listener);
		return this;
	}

	/**
	 * Start this CardAndTerminalManager. Doing this after registering one or
	 * more CardTerminalEventsListener and/or CardEventsListener instances will
	 * cause these be be called with the initial situation: The terminals and
	 * cards already present. Calling start() before registering any listeners
	 * will cause these to not see the initial situation.
	 *
	 * @return this CardAndTerminalManager to allow for method chaining.
	 */
	public CardAndTerminalManager start() {
		logger.debug("CardAndTerminalManager worker thread start requested.");
		worker = new Thread(this, "CardAndTerminalManager");
		worker.setDaemon(true);
		worker.start();
		return this;
	}

	// --------------------------------------------------------------------------------------------------

	/**
	 * Unregister a CardTerminalEventsListener instance.
	 *
	 * @param listener the CardTerminalEventsListener to be unregistered
	 * @return this CardAndTerminalManager to allow for method chaining.
	 */
	public CardAndTerminalManager removeCardTerminalListener(CardTerminalEventsListener listener) {
		cardTerminalEventsListeners.remove(listener);
		return this;
	}

	/**
	 * Unregister a CardEventsListener instance.
	 *
	 * @param listener the CardEventsListener to be unregistered
	 * @return this CardAndTerminalManager to allow for method chaining.
	 */
	public CardAndTerminalManager removeCardListener(CardEventsListener listener) {
		cardEventsListeners.remove(listener);
		return this;
	}

	/**
	 * Start ignoring the CardTerminal with the name given. A CardTerminal's
	 * name is the exact String as returned by
	 * {@link javax.smartcardio.CardTerminal#getName() CardTerminal.getName()}
	 * Note that this name is neither very stable, nor portable between
	 * operating systems: it is constructed by the PCSC subsystem in an
	 * arbitrary fashion, and may change between releases.
	 *
	 * @return this CardAndTerminalManager to allow for method chaining.
	 */
	public CardAndTerminalManager ignoreCardEventsFor(String terminalName) {
		terminalsToIgnoreCardEventsFor.add(terminalName);
		return this;
	}

	/**
	 * Start accepting events for the CardTerminal with the name given, where
	 * these were being ignored due to a previous call to
	 * {@link #ignoreCardEventsFor(String)}.
	 *
	 * @return this CardAndTerminalManager to allow for method chaining.
	 */
	public CardAndTerminalManager acceptCardEventsFor(String terminalName) {
		terminalsToIgnoreCardEventsFor.remove(terminalName);
		return this;
	}

	// -----------------------------------------------------------------------

	/**
	 * Stop this CardAndTerminalManager. This will may block until the worker
	 * thread has returned, meaning that after this call returns, no registered
	 * listeners will receive any more events.
	 *
	 * @return this CardAndTerminalManager to allow for method chaining.
	 */
	public CardAndTerminalManager stop() throws InterruptedException {
		logger.debug("CardAndTerminalManager worker thread stop requested.");
		running = false;
		worker.interrupt();
		worker.join();
		return this;
	}

	/**
	 * Returns the PCSC polling delay currently in use
	 *
	 * @return the PCSC polling delay currently in use
	 */
	public int getDelay() {
		return delay;
	}

	/**
	 * Set the PCSC polling delay. A CardAndTerminalsManager will wait for a
	 * maximum of newDelay milliseconds for new events to be received, before
	 * issuing a new call to the PCSC subsystem. The higher this number, the
	 * less CPU this CardAndTerminalsManager will take, but the greater the
	 * chance that terminal attach/detach events will be noticed late.
	 *
	 * @param newDelay the new delay to trust the PCSC subsystem for
	 * @return this CardAndTerminalManager to allow for method chaining.
	 */
	public CardAndTerminalManager setDelay(int newDelay) {
		delay = newDelay;
		return this;
	}

	/**
	 * Return whether this CardAndTerminalsManager will automatically connect()
	 * to any cards inserted.
	 *
	 * @return this CardAndTerminalManager to allow for method chaining.
	 */
	public boolean isAutoconnect() {
		return autoconnect;
	}

	/**
	 * Set whether this CardAndTerminalsManager will automatically connect() to
	 * any cards inserted.
	 *
	 * @return this CardAndTerminalManager to allow for method chaining.
	 */
	public CardAndTerminalManager setAutoconnect(boolean newAutoConnect) {
		autoconnect = newAutoConnect;
		return this;
	}

	/**
	 * return which card protocols this CardAndTerminalsManager will attempt to
	 * connect to cards with. (if autoconnect is true, see
	 * {@link CardAndTerminalManager#setAutoconnect(boolean)}) the default is
	 * Protocol.ANY which allows any protocol.
	 *
	 * @return the currently attempted protocol(s)
	 */
	public Protocol getProtocol() {
		return protocol;
	}

	/**
	 * Determines which card protocols this CardAndTerminalsManager will attempt
	 * to connect to cards with. (if autoconnect is true, see
	 * {@link CardAndTerminalManager#setAutoconnect(boolean)}) the default is
	 * Protocol.ANY which allows any protocol.
	 *
	 * @param newProtocol the card protocol(s) to attempt connection to the cards with
	 * @return this CardAndTerminalManager to allow for method chaining.
	 */
	public CardAndTerminalManager setProtocol(Protocol newProtocol) {
		protocol = newProtocol;
		return this;
	}

	@Override
	public void run() {
		running = true;
		logger.debug("CardAndTerminalManager worker thread started.");

		try {
			// do an initial run, making sure current status is detected
			// this sends terminal attach and card insert events for this
			// initial state to any listeners
			handlePCSCEvents();

			// advise listeners that initial state was sent, and that any
			// further events are relative to this
			listenersInitialized();

			// keep updating
			while (running) {
				handlePCSCEvents();
			}
		} catch (InterruptedException iex) {
			if (running) {
				logger.error("CardAndTerminalManager worker thread unexpectedly interrupted: " + iex.getLocalizedMessage());
			}
		}

		logger.debug("CardAndTerminalManager worker thread ended.");
	}

	private void handlePCSCEvents() throws InterruptedException {
		if (!subSystemInitialized) {
			logger.debug("subsystem not initialized");
			try {
				if (terminalsPresent == null || terminalsWithCards == null) {
					terminalsPresent = new HashSet<>(cardTerminals.list(State.ALL));
					terminalsWithCards = terminalsWithCardsIn(terminalsPresent);
				}

				listenersTerminalsAttachedCardsInserted(terminalsPresent, terminalsWithCards);
				subSystemInitialized = true;
			} catch (CardException cex) {
				logCardException(cex, "Cannot enumerate card terminals [1] (No Card Readers Connected?)");
				clear();
				sleepForDelay();
				return;
			}
		}

		try {
			// can't use waitForChange properly, that is in blocking mode, without delay argument, since it sometimes
			// misses reader attach events.. (TODO: test on other platforms) this limits us to what is basically a
			// polling strategy, with a small speed gain where waitForChange *does* detect events (because it will
			// return faster than delay) for most events this will make reaction instantaneous, and worst case = delay
			cardTerminals.waitForChange(delay);
		} catch (CardException cex) {
			// waitForChange fails (e.g. PCSC is there but no readers)
			logCardException(cex, "Cannot wait for card terminal events [2] (No Card Readers Connected?)");
			clear();
			sleepForDelay();
			return;
		} catch (IllegalStateException ise) {
			// waitForChange fails (e.g. PCSC is not there)
			logger.debug("Cannot wait for card terminal changes (no PCSC subsystem?): " + ise.getLocalizedMessage());
			clear();
			sleepForDelay();
			return;
		}

		try {
			// get fresh state
			Set<CardTerminal> currentTerminals = new HashSet<>(cardTerminals.list(State.ALL));
			Set<CardTerminal> currentTerminalsWithCards = terminalsWithCardsIn(currentTerminals);

			// determine terminals that were attached since previous state
			Set<CardTerminal> terminalsAttached = new HashSet<>(currentTerminals);
			terminalsAttached.removeAll(terminalsPresent);

			// determine terminals that had cards inserted since previous state
			Set<CardTerminal> terminalsWithCardsInserted = new HashSet<>(currentTerminalsWithCards);
			terminalsWithCardsInserted.removeAll(terminalsWithCards);

			// determine terminals that had cards removed since previous state
			Set<CardTerminal> terminalsWithCardsRemoved = new HashSet<>(terminalsWithCards);
			terminalsWithCardsRemoved.removeAll(currentTerminalsWithCards);

			// determine terminals detached since previous state
			Set<CardTerminal> terminalsDetached = new HashSet<>(terminalsPresent);
			terminalsDetached.removeAll(currentTerminals);

			// keep fresh state to compare to next time (and to return to synchronous callers)
			terminalsPresent = currentTerminals;
			terminalsWithCards = currentTerminalsWithCards;

			// advise the listeners where appropriate, always in the order attach, insert, remove, detach
			listenersUpdateInSequence(terminalsAttached, terminalsWithCardsInserted, terminalsWithCardsRemoved, terminalsDetached);
		} catch (CardException cex) {
			// if a CardException occurs, assume we're out of readers (only
			// CardTerminals.list throws that here)
			// CardTerminal fails in that case, instead of simply seeing zero
			// CardTerminals.
			logCardException(cex, "Cannot wait for card terminal changes (no PCSC subsystem?)");
			clear();
			sleepForDelay();
		}
	}

	private boolean areCardEventsIgnoredFor(CardTerminal cardTerminal) {
		for (String prefixToMatch : copyOf(terminalsToIgnoreCardEventsFor)) {
			if (cardTerminal.getName().startsWith(prefixToMatch)) {
				return true;
			}
		}

		return false;
	}

	private Set<CardTerminal> terminalsWithCardsIn(Set<CardTerminal> terminals) {
		Set<CardTerminal> terminalsWithCards = new HashSet<>();

		for (CardTerminal terminal : terminals) {
			try {
				if (terminal.isCardPresent() && !areCardEventsIgnoredFor(terminal)) {
					terminalsWithCards.add(terminal);
				}
			} catch (CardException cex) {
				logger.error("Problem determining card presence in terminal [" + terminal.getName() + "]");
			}
		}

		return terminalsWithCards;
	}

	private void clear() {
		// if we were already initialized, we may have sent attached and insert
		// events we now pretend to remove and detach all that we know of, for
		// consistency
		if (subSystemInitialized) {
			listenersCardsRemovedTerminalsDetached(terminalsWithCards, terminalsPresent);
		}

		terminalsPresent = null;
		terminalsWithCards = null;
		subSystemInitialized = false;
		logger.debug("cleared");
	}

	private void listenersTerminalsAttachedCardsInserted(Set<CardTerminal> attached, Set<CardTerminal> inserted) {
		listenersTerminalsAttached(attached);
		listenersTerminalsWithCardsInserted(inserted);
	}

	private void listenersCardsRemovedTerminalsDetached(Set<CardTerminal> removed, Set<CardTerminal> detached) {
		listenersTerminalsWithCardsRemoved(removed);
		listenersTerminalsDetached(detached);
	}

	private void listenersUpdateInSequence(Set<CardTerminal> attached, Set<CardTerminal> inserted, Set<CardTerminal> removed, Set<CardTerminal> detached) {
		listenersTerminalsAttached(attached);
		listenersTerminalsWithCardsInserted(inserted);
		listenersTerminalsWithCardsRemoved(removed);
		listenersTerminalsDetached(detached);
	}

	private void listenersInitialized() {
		listenersTerminalEventsInitialized();
		listenersCardEventsInitialized();
	}

	private void listenersCardEventsInitialized() {
		for (CardEventsListener listener : copyOf(cardEventsListeners)) {
			try {
				listener.cardEventsInitialized();
			} catch (Exception thrownInListener) {
				logger.error("Exception thrown in CardEventsListener.cardRemoved:" + thrownInListener.getMessage());
			}
		}
	}

	private void listenersTerminalEventsInitialized() {
		for (CardTerminalEventsListener listener : copyOf(cardTerminalEventsListeners)) {
			try {
				listener.terminalEventsInitialized();
			} catch (Exception thrownInListener) {
				logger.error("Exception thrown in CardTerminalEventsListener.terminalAttached:" + thrownInListener.getMessage());
			}
		}
	}

	// Tell listeners about attached readers
	private void listenersTerminalsAttached(Set<CardTerminal> attached) {
		if (!attached.isEmpty()) {
			for (CardTerminal terminal : attached) {
				for (CardTerminalEventsListener listener : copyOf(cardTerminalEventsListeners)) {
					try {
						listener.terminalAttached(terminal);
					} catch (Exception thrownInListener) {
						logger.error("Exception thrown in CardTerminalEventsListener.terminalAttached:" + thrownInListener.getMessage());
					}
				}
			}
		}
	}

	// Tell listeners about detached readers
	private void listenersTerminalsDetached(Set<CardTerminal> detached) {
		if (!detached.isEmpty()) {
			Set<CardTerminalEventsListener> copyOfListeners = copyOf(cardTerminalEventsListeners);

			for (CardTerminal terminal : detached) {
				for (CardTerminalEventsListener listener : copyOfListeners) {
					try {
						listener.terminalDetached(terminal);
					} catch (Exception thrownInListener) {
						logger.error("Exception thrown in CardTerminalEventsListener.terminalDetached:" + thrownInListener.getMessage());
					}
				}
			}
		}
	}

	@SuppressWarnings("SynchronizationOnLocalVariableOrMethodParameter")
	private <T> Set<T> copyOf(Set<T> set) {
		synchronized (set) {
			return new HashSet<>(set);
		}
	}

	// Tell listeners about removed cards
	private void listenersTerminalsWithCardsRemoved(Set<CardTerminal> removed) {
		if (!removed.isEmpty()) {
			for (CardTerminal terminal : removed) {
				for (CardEventsListener listener : copyOf(cardEventsListeners)) {
					try {
						listener.cardRemoved(terminal);
					} catch (Exception thrownInListener) {
						logger.error("Exception thrown in CardEventsListener.cardRemoved:" + thrownInListener.getMessage());
					}
				}
			}
		}
	}

	// Tell listeners about inserted cards. giving them the CardTerminal and a
	// Card object
	// if autoconnect is enabled (the default), the card argument may be
	// automatically
	// filled out, but it may still be null, if the connect failed.
	private void listenersTerminalsWithCardsInserted(Set<CardTerminal> inserted) {
		if (!inserted.isEmpty()) {
			for (CardTerminal terminal : inserted) {
				Card card = null;

				if (autoconnect) {
					try {
						card = terminal.connect(protocol.getProtocol());
					} catch (CardException cex) {
						logger.debug("terminal.connect(" + protocol.getProtocol() + ") failed. " + cex.getMessage());
					}
				}

				for (CardEventsListener listener : copyOf(cardEventsListeners)) {
					try {
						listener.cardInserted(terminal, card);
					} catch (Exception thrownInListener) {
						logger.error("Exception thrown in CardEventsListener.cardInserted:" + thrownInListener.getMessage());
					}
				}
			}
		}
	}

	private void sleepForDelay() throws InterruptedException {
		Thread.sleep(delay);
	}

	private void logCardException(CardException cex, String where) {
		logger.debug(where + ": " + cex.getMessage());
		logger.debug("no card readers connected?");
		Throwable cause = cex.getCause();
		if (cause == null) {
			return;
		}
		logger.debug("cause: " + cause.getMessage());
		logger.debug("cause type: " + cause.getClass().getName());
	}
}
