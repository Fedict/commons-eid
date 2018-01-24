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

import be.bosa.commons.eid.client.FileType;
import be.bosa.commons.eid.client.event.BeIDCardListener;

public class TestBeIDCardListener implements BeIDCardListener {

	@Override
	public void notifyReadProgress(FileType fileType, int offset, int estimatedMaxSize) {
		System.out.println("read progress of " + fileType.name() + ":" + offset + " of " + estimatedMaxSize);
	}

	@Override
	public void notifySigningBegin(FileType keyType) {
		System.out.println("signing with " + (keyType == FileType.AuthentificationCertificate ? "authentication" : "non-repudiation") + " key has begun");
	}

	@Override
	public void notifySigningEnd(FileType keyType) {
		System.out.println("signing with " + (keyType == FileType.AuthentificationCertificate ? "authentication" : "non-repudiation") + " key has ended");
	}
}
