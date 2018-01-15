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

import be.fedict.commons.eid.client.BeIDCard;
import be.fedict.commons.eid.client.BeIDCards;
import be.fedict.commons.eid.client.CancelledException;
import be.fedict.commons.eid.client.FileType;
import be.fedict.commons.eid.client.event.BeIDCardListener;
import be.fedict.commons.eid.client.impl.CCID;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;

public class CCIDExercises extends BeIDTest {
	
	@Test
	public void listCCIDFeatures() throws CancelledException {
		BeIDCard beIDCard = getBeIDCard();
		beIDCard.addCardListener(new TestBeIDCardListener());

		for (CCID.FEATURE feature : CCID.FEATURE.values()) {
			System.out.println(feature.name() + "\t" + (beIDCard.cardTerminalHasCCIDFeature(feature) ? "AVAILABLE" : "NOT AVAILABLE"));
		}
	}

	@Test
	public void listCCIDFeaturesWithPPDU() throws CancelledException {
		CCID.riskPPDU(true);
		BeIDCard beIDCard = getBeIDCard();
		beIDCard.addCardListener(new TestBeIDCardListener());

		for (CCID.FEATURE feature : CCID.FEATURE.values()) {
			System.out.println(feature.name() + "\t" + (beIDCard.cardTerminalHasCCIDFeature(feature) ? "AVAILABLE" : "NOT AVAILABLE"));
		}
	}

}
