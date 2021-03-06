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

package be.bosa.commons.eid.client.tests.integration;

import javax.smartcardio.ATR;
import javax.smartcardio.CardTerminal;
import java.math.BigInteger;
import java.util.Set;

public class StringUtils {
	
	public static String atrToString(ATR atr) {
		return String.format("%x", new BigInteger(1, atr.getBytes()));
	}

	public static String getShortTerminalname(String terminalName) {
		StringBuilder shortName = new StringBuilder();
		String[] words = terminalName.split(" ");
		if (words.length > 1) {
			shortName.append(words[0]);
			shortName.append(" ");
			shortName.append(words[1]);
		} else {
			shortName.append(terminalName);
		}

		return shortName.toString();
	}

	public static void printTerminalSet(Set<CardTerminal> set) {
		StringBuilder overviewLine = new StringBuilder();

		for (CardTerminal terminal : set) {
			overviewLine.append("[");
			overviewLine.append(terminal.getName());
			overviewLine.append("] ");
		}

		System.out.println(overviewLine.toString());
	}
}
