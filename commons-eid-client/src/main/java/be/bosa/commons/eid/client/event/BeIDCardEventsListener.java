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

package be.bosa.commons.eid.client.event;

import be.bosa.commons.eid.client.BeIDCard;
import be.bosa.commons.eid.client.BeIDCardManager;

import javax.smartcardio.CardTerminal;

/**
 * The BeIDCardEventsListener represents events delivered by an
 * {@link BeIDCardManager}. Register one or more instances of a class
 * implementing BeIDCardEventsListener to respond to BeID cards being inserted
 * and removed.
 *
 * @author Frank Marien
 */
public interface BeIDCardEventsListener {
	void eIDCardEventsInitialized();

	void eIDCardInserted(CardTerminal cardTerminal, BeIDCard card);

	void eIDCardRemoved(CardTerminal cardTerminal, BeIDCard card);
}
