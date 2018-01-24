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

package be.bosa.commons.eid.consumer.tlv;

import be.bosa.commons.eid.consumer.SpecialOrganisation;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.UnsupportedEncodingException;

/**
 * Data convertor for special organisation eID identity field.
 *
 * @author Frank Cornelis
 */
public class SpecialOrganisationConvertor implements DataConvertor<SpecialOrganisation> {

	private static final Log LOG = LogFactory.getLog(SpecialOrganisationConvertor.class);

	@Override
	public SpecialOrganisation convert(byte[] value) throws DataConvertorException {
		if (null == value) {
			return SpecialOrganisation.UNSPECIFIED;
		}

		try {
			String key = new String(value, "UTF-8");
			LOG.debug("key: \"" + key + "\"");
			return SpecialOrganisation.toSpecialOrganisation(key);
		} catch (final UnsupportedEncodingException uex) {
			throw new DataConvertorException("string error: " + uex.getMessage());
		}
	}
}
