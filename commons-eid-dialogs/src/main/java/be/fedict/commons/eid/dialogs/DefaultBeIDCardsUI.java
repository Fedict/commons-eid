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

package be.fedict.commons.eid.dialogs;

import be.fedict.commons.eid.client.BeIDCard;
import be.fedict.commons.eid.client.CancelledException;
import be.fedict.commons.eid.client.OutOfCardsException;
import be.fedict.commons.eid.client.impl.LocaleManager;
import be.fedict.commons.eid.client.spi.BeIDCardsUI;

import javax.swing.*;
import java.awt.*;
import java.util.Collection;
import java.util.Locale;

/**
 * Default Implementation of BeIDCardsUI Interface
 * 
 * @author Frank Marien
 * 
 */
public class DefaultBeIDCardsUI implements BeIDCardsUI {
	private final Component parentComponent;
	private Messages messages;
	private JFrame adviseFrame;
	private BeIDSelector selectionDialog;
	private Locale locale;

	public DefaultBeIDCardsUI() {
		this(null);
	}

	public DefaultBeIDCardsUI(Component parentComponent) {
		this(parentComponent, null);
	}

	public DefaultBeIDCardsUI(Component parentComponent, Messages messages) {
		if (GraphicsEnvironment.isHeadless()) {
			throw new UnsupportedOperationException("DefaultBeIDCardsUI is a GUI and cannot run in a headless environment");
		}

		this.parentComponent = parentComponent;
		if (messages != null) {
			this.messages = messages;
			setLocale(messages.getLocale());
		} else {
			this.messages = Messages.getInstance(getLocale());
		}
	}

	@Override
	public void adviseCardTerminalRequired() {
		showAdvise(
				messages.getMessage(Messages.MESSAGE_ID.CONNECT_READER),
				messages.getMessage(Messages.MESSAGE_ID.CONNECT_READER)
		);
	}

	@Override
	public void adviseBeIDCardRequired() {
		showAdvise(
				messages.getMessage(Messages.MESSAGE_ID.INSERT_CARD_QUESTION),
				messages.getMessage(Messages.MESSAGE_ID.INSERT_CARD_QUESTION)
		);
	}

	@Override
	public void adviseBeIDCardRemovalRequired() {
		showAdvise(
				messages.getMessage(Messages.MESSAGE_ID.REMOVE_CARD),
				messages.getMessage(Messages.MESSAGE_ID.REMOVE_CARD)
		);

	}

	@Override
	public BeIDCard selectBeIDCard(Collection<BeIDCard> availableCards) throws CancelledException, OutOfCardsException {
		try {
			selectionDialog = new BeIDSelector(parentComponent, "Select eID card", availableCards);
			return selectionDialog.choose();
		} finally {
			selectionDialog = null;
		}
	}

	@Override
	public void adviseEnd() {
		if (null != adviseFrame) {
			adviseFrame.dispose();
			adviseFrame = null;
		}
	}

	/*
	 * **********************************************************************************************************************
	 */

	private void showAdvise(String title, String message) {
		if (null != adviseFrame) {
			adviseEnd();
		}

		adviseFrame = new JFrame(title);
		JPanel panel = new JPanelWithInsets(new Insets(10, 30, 10, 30));
		BoxLayout boxLayout = new BoxLayout(panel, BoxLayout.PAGE_AXIS);
		panel.setLayout(boxLayout);

		panel.add(new JLabel(message));
		adviseFrame.getContentPane().add(panel);
		adviseFrame.pack();

		if (parentComponent != null) {
			adviseFrame.setLocationRelativeTo(parentComponent);
		} else {
			Util.centerOnScreen(adviseFrame);
		}
		adviseFrame.setAlwaysOnTop(true);
		adviseFrame.setVisible(true);
	}

	@Override
	public void eIDCardInsertedDuringSelection(BeIDCard card) {
		if (selectionDialog != null) {
			selectionDialog.addEIDCard(card);
		}
	}

	@Override
	public void eIDCardRemovedDuringSelection(BeIDCard card) {
		if (selectionDialog != null) {
			selectionDialog.removeEIDCard(card);
		}
	}

	@Override
	public void setLocale(Locale newLocale) {
		this.locale = newLocale;
		this.messages = Messages.getInstance(newLocale);
	}

	@Override
	public Locale getLocale() {
		if (locale != null) {
			return locale;
		}
		return LocaleManager.getLocale();
	}

}
