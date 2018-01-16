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

import javax.smartcardio.ResponseAPDU;

/**
 * A ResponseAPDUException encapsulates a ResponseAPDU that lead to the
 * exception, making it available to the catching code.
 *
 * @author Frank Marien
 */
public class ResponseAPDUException extends RuntimeException {

	private final ResponseAPDU apdu;

	public ResponseAPDUException(ResponseAPDU apdu) {
		super();
		this.apdu = apdu;
	}

	public ResponseAPDUException(String message, ResponseAPDU apdu) {
		super(message + " [" + Integer.toHexString(apdu.getSW()) + "]");
		this.apdu = apdu;
	}

	public ResponseAPDU getApdu() {
		return apdu;
	}
}
