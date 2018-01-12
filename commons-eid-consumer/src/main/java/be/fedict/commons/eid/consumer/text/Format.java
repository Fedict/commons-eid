/*
 * Commons eID Project.
 * Copyright (C) 2012-2013 FedICT.
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

package be.fedict.commons.eid.consumer.text;

/**
 * @author Frank Marien
 */
public class Format {

	private Format() {
	}

	/**
	 * Format a national number into YY.MM.DD-S&amp;G.CS
	 */
	public static String formatNationalNumber(String nationalNumber) {
		// YY MM DD S&G CS
		// 01 23 45 678 9A

		return nationalNumber.substring(0, 2) + '.'
				+ nationalNumber.substring(2, 4) + '.'
				+ nationalNumber.substring(4, 6) + '-'
				+ nationalNumber.substring(6, 9) + '.'
				+ nationalNumber.substring(9);
	}

	/**
	 * Format a card number into XXX-YYYYYYYY-ZZ
	 */
	public static String formatCardNumber(String cardNumber) {
		StringBuilder formatted = new StringBuilder();

		if (cardNumber.length() == 10 && cardNumber.startsWith("B")) {
			// B 0123456 78
			formatted.append(cardNumber.substring(0, 1));
			formatted.append(' ');
			formatted.append(cardNumber.substring(1, 7));
			formatted.append(' ');
			formatted.append(cardNumber.substring(8));
		} else if (cardNumber.length() == 12) {
			// 012-3456789-01
			formatted.append(cardNumber.substring(0, 3));
			formatted.append('-');
			formatted.append(cardNumber.substring(3, 10));
			formatted.append('-');
			formatted.append(cardNumber.substring(10));
		} else {
			formatted.append(cardNumber);
		}

		return formatted.toString();
	}
}
