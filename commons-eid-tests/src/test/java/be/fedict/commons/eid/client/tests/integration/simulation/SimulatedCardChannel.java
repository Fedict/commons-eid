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

package be.fedict.commons.eid.client.tests.integration.simulation;

import javax.smartcardio.Card;
import javax.smartcardio.CardChannel;
import javax.smartcardio.CardException;
import javax.smartcardio.CommandAPDU;
import javax.smartcardio.ResponseAPDU;
import java.nio.ByteBuffer;

public class SimulatedCardChannel extends CardChannel {
	private final SimulatedCard card;

	public SimulatedCardChannel(SimulatedCard card) {
		this.card = card;
	}

	@Override
	public void close() {
		throw new RuntimeException("Not Implemented In SimulatedCardChannel");
	}

	@Override
	public Card getCard() {
		return this.card;
	}

	@Override
	public int getChannelNumber() {
		return 0;
	}

	@Override
	public ResponseAPDU transmit(CommandAPDU apdu) throws CardException {
		return this.card.transmit(apdu);
	}

	@Override
	public int transmit(ByteBuffer bb0, ByteBuffer bb1) {
		throw new RuntimeException("Not Implemented In SimulatedCardChannel");
	}
}
