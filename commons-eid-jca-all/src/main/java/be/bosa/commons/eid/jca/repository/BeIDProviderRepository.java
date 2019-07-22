/*
 * Commons eID Project.
 * Copyright (C) 2014 - 2019 BOSA.
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
package be.bosa.commons.eid.jca.repository;

import java.security.Security;

import be.bosa.commons.eid.jca.BeIDProvider;

public class BeIDProviderRepository {

	public static BeIDProvider INSTANCE = new BeIDProvider();

	static {
		Security.addProvider(INSTANCE);
	}

}
