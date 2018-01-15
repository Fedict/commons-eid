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

import be.fedict.commons.eid.client.impl.LibJ2PCSCGNULinuxFix;
import org.junit.Test;

public class LibJ2PCSCGNULinuxFixTest {

	public static void main(final String[] args) {
		final LibJ2PCSCGNULinuxFixTest fixtest = new LibJ2PCSCGNULinuxFixTest();
		fixtest._testFix();
	}

	@Test
	public void testFix() {
		this._testFix();
	}

	private void _testFix() {
		LibJ2PCSCGNULinuxFix.fixNativeLibrary(new TestLogger());
	}
}