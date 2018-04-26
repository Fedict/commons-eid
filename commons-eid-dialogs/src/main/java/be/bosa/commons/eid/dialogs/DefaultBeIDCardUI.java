/*
 * Commons eID Project.
 * Copyright (C) 2008-2013 FedICT.
 * Copyright (C) 2009 Frank Cornelis.
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

package be.bosa.commons.eid.dialogs;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.Arrays;
import java.util.Locale;

import javax.swing.*;
import javax.swing.border.Border;

import be.bosa.commons.eid.client.CancelledException;
import be.bosa.commons.eid.client.PINPurpose;
import be.bosa.commons.eid.client.spi.BeIDCardUI;

/**
 * Default Implementation of BeIDCardUI Interface
 * 
 * @author Frank Cornelis
 * @author Frank Marien
 * 
 */
public class DefaultBeIDCardUI implements BeIDCardUI {

	public static final int MIN_PIN_SIZE = 4;
	public static final int MAX_PIN_SIZE = 12;
	public static final int PUK_SIZE = 6;

	private static final String OPERATION_CANCELLED = "operation cancelled.";
	private static final int BORDER_SIZE = 20;

	// TODO can pinPadFrame and secureReaderTransactionFrame be on-screen at the same time? if not can be one member var and one dispose method
	private final Component parentComponent;
	private JFrame pinPadFrame;
	private JFrame secureReaderTransactionFrame;
	private Locale locale;
	private Messages messages;

	public DefaultBeIDCardUI() {
		this(null);
	}

	public DefaultBeIDCardUI(Messages messages) {
		this(null, messages);
	}

	public DefaultBeIDCardUI(Component parentComponent, Messages messages) {
		if (GraphicsEnvironment.isHeadless()) {
			throw new UnsupportedOperationException("DefaultBeIDCardUI is a GUI and hence requires an interactive GraphicsEnvironment");
		}

		this.parentComponent = parentComponent;
		if (messages != null) {
			this.messages = messages;
		} else {
			this.messages = Messages.getInstance();
		}
	}

	@Override
	public void advisePINBlocked() {
		JOptionPane.showMessageDialog(parentComponent, messages.getMessage(Messages.MESSAGE_ID.PIN_BLOCKED), "eID card blocked", JOptionPane.ERROR_MESSAGE);
	}

	@Override
	public void advisePINChanged() {
		JOptionPane.showMessageDialog(parentComponent, messages.getMessage(Messages.MESSAGE_ID.PIN_CHANGED), "eID PIN change", JOptionPane.INFORMATION_MESSAGE);
	}

	@Override
	public void advisePINPadChangePIN(int retriesLeft) {
		showPINPadFrame(retriesLeft, "eID PIN change", messages.getMessage(Messages.MESSAGE_ID.PIN_PAD_CHANGE));

	}

	@Override
	public void advisePINPadNewPINEntry(int retriesLeft) {
		showPINPadFrame(retriesLeft, "eID PIN change", messages.getMessage(Messages.MESSAGE_ID.PIN_PAD_MODIFY_NEW));
	}

	@Override
	public void advisePINPadNewPINEntryAgain(int retriesLeft) {
		showPINPadFrame(retriesLeft, "eID PIN change", messages.getMessage(Messages.MESSAGE_ID.PIN_PAD_MODIFY_NEW_AGAIN));
	}

	@Override
	public void advisePINPadOldPINEntry(int retriesLeft) {
		showPINPadFrame(retriesLeft, "eID PIN change", messages.getMessage(Messages.MESSAGE_ID.PIN_PAD_MODIFY_OLD));

	}

	@Override
	public void advisePINPadOperationEnd() {
		disposePINPadFrame();
	}

	@Override
	public void advisePINPadPINEntry(int retriesLeft, PINPurpose purpose) {
		showPINPadFrame(retriesLeft, "PIN",
				messages.getMessage(Messages.MESSAGE_ID.PIN_REASON),
				messages.getMessage(Messages.MESSAGE_ID.PIN_PAD)
		);
	}

	@Override
	public void advisePINPadPUKEntry(int retriesLeft) {
		showPINPadFrame(retriesLeft, "eID PIN unblock", messages.getMessage(Messages.MESSAGE_ID.PUK_PAD));

	}

	@Override
	public void advisePINUnblocked() {
		JOptionPane.showMessageDialog(parentComponent, messages.getMessage(Messages.MESSAGE_ID.PIN_UNBLOCKED), "eID PIN unblock", JOptionPane.INFORMATION_MESSAGE);
	}

	@Override
	public char[][] obtainOldAndNewPIN(int retriesLeft) {
		Box mainPanel = Box.createVerticalBox();

		if (-1 != retriesLeft) {
			mainPanel.add(Box.createVerticalStrut(4));
			Box retriesPanel = createWarningBox(messages.getMessage(Messages.MESSAGE_ID.RETRIES_LEFT) + ": " + retriesLeft);
			mainPanel.add(retriesPanel);
			mainPanel.add(Box.createVerticalStrut(24));
		}

		JPasswordField oldPinField = new JPasswordField(MAX_PIN_SIZE);
		Box oldPinPanel = Box.createHorizontalBox();
		JLabel oldPinLabel = new JLabel(messages.getMessage(Messages.MESSAGE_ID.CURRENT_PIN) + ":");
		oldPinLabel.setLabelFor(oldPinField);
		oldPinPanel.add(oldPinLabel);
		oldPinPanel.add(Box.createHorizontalStrut(5));
		oldPinPanel.add(oldPinField);
		mainPanel.add(oldPinPanel);

		mainPanel.add(Box.createVerticalStrut(5));

		JPasswordField newPinField = new JPasswordField(MAX_PIN_SIZE);
		Box newPinPanel = Box.createHorizontalBox();
		JLabel newPinLabel = new JLabel(messages.getMessage(Messages.MESSAGE_ID.NEW_PIN) + ":");
		newPinLabel.setLabelFor(newPinField);
		newPinPanel.add(newPinLabel);
		newPinPanel.add(Box.createHorizontalStrut(5));
		newPinPanel.add(newPinField);
		mainPanel.add(newPinPanel);

		mainPanel.add(Box.createVerticalStrut(5));

		JPasswordField new2PinField = new JPasswordField(MAX_PIN_SIZE);
			Box new2PinPanel = Box.createHorizontalBox();
			JLabel new2PinLabel = new JLabel(
					messages.getMessage(Messages.MESSAGE_ID.NEW_PIN) + ":");
			new2PinLabel.setLabelFor(new2PinField);
			new2PinPanel.add(new2PinLabel);
			new2PinPanel.add(Box.createHorizontalStrut(5));
			new2PinPanel.add(new2PinField);
			mainPanel.add(new2PinPanel);

		int result = JOptionPane.showOptionDialog(parentComponent, mainPanel,
				"Change eID PIN", JOptionPane.OK_CANCEL_OPTION,
				JOptionPane.QUESTION_MESSAGE, null, null, null
		);
		if (result != JOptionPane.OK_OPTION) {
			throw new RuntimeException(OPERATION_CANCELLED);
		}
		if (!Arrays.equals(newPinField.getPassword(), new2PinField.getPassword())) {
			throw new RuntimeException("new PINs not equal");
		}
		char[] oldPin = new char[oldPinField.getPassword().length];
		char[] newPin = new char[newPinField.getPassword().length];
		System.arraycopy(oldPinField.getPassword(), 0, oldPin, 0, oldPinField.getPassword().length);
		System.arraycopy(newPinField.getPassword(), 0, newPin, 0, newPinField.getPassword().length);
		Arrays.fill(oldPinField.getPassword(), (char) 0);
		Arrays.fill(newPinField.getPassword(), (char) 0);
		return new char[][]{oldPin, newPin};
	}

	@Override
	public char[] obtainPIN(int retriesLeft, PINPurpose reason) throws CancelledException {
		JPanel mainPanel = new JPanelWithInsets(new Insets(BORDER_SIZE, BORDER_SIZE, BORDER_SIZE, BORDER_SIZE));
		BoxLayout boxLayout = new BoxLayout(mainPanel, BoxLayout.PAGE_AXIS);
		mainPanel.setLayout(boxLayout);

		Box reasonPanel = Box.createHorizontalBox();
		JLabel reasonLabel = new JLabel(messages.getMessage(Messages.MESSAGE_ID.PIN_REASON));
		reasonPanel.add(reasonLabel);
		reasonPanel.add(Box.createHorizontalGlue());
		mainPanel.add(reasonPanel);
		mainPanel.add(Box.createVerticalStrut(16));

		if (-1 != retriesLeft) {
			addWarningBox(mainPanel, messages.getMessage(Messages.MESSAGE_ID.RETRIES_LEFT) + ": " + retriesLeft);
		}

		Box passwordPanel = Box.createHorizontalBox();
		JLabel promptLabel = new JLabel(messages.getMessage(Messages.MESSAGE_ID.LABEL_PIN) + ": ");
		passwordPanel.add(promptLabel);
		passwordPanel.add(Box.createHorizontalStrut(5));
		JPasswordField passwordField = new JPasswordField(MAX_PIN_SIZE);
		promptLabel.setLabelFor(passwordField);
		passwordPanel.add(passwordField);
		passwordPanel.setBorder(createGenerousLowerBevelBorder());
		mainPanel.add(passwordPanel);

		// button panel
		JPanel buttonPanel = new JPanelWithInsets(new FlowLayout(FlowLayout.RIGHT), new Insets(0, 0, 5, 5));
		JButton okButton = new JButton(messages.getMessage(Messages.MESSAGE_ID.OK));
		okButton.setEnabled(false);
		buttonPanel.add(okButton);
		JButton cancelButton = new JButton(messages.getMessage(Messages.MESSAGE_ID.CANCEL));
		buttonPanel.add(cancelButton);

		// dialog box
		JDialog dialog = new JDialog((Frame) null, messages.getMessage(Messages.MESSAGE_ID.ENTER_PIN), true);
		dialog.setLayout(new BorderLayout());
		dialog.getContentPane().add(mainPanel, BorderLayout.CENTER);
		dialog.getContentPane().add(buttonPanel, BorderLayout.SOUTH);

		DialogResult dialogResult = new DialogResult();

		okButton.addActionListener(event -> {
			dialogResult.result = DialogResult.Result.OK;
			dialog.dispose();
		});
		cancelButton.addActionListener(event -> {
			dialogResult.result = DialogResult.Result.CANCEL;
			dialog.dispose();
		});
		passwordField.addActionListener(event -> {
			int pinSize = passwordField.getPassword().length;
			if (MIN_PIN_SIZE <= pinSize && pinSize <= MAX_PIN_SIZE) {
				dialogResult.result = DialogResult.Result.OK;
				dialog.dispose();
			}
		});

		passwordField.addKeyListener(new KeyListener() {
			public void keyPressed(KeyEvent e) { }

			public void keyReleased(KeyEvent e) {
				int pinSize = passwordField.getPassword().length;
				if (MIN_PIN_SIZE <= pinSize && pinSize <= MAX_PIN_SIZE) {
					okButton.setEnabled(true);
				} else {
					okButton.setEnabled(false);
				}
			}

			public void keyTyped(KeyEvent e) { }
		});

		dialog.pack();
		if (parentComponent != null) {
			dialog.setLocationRelativeTo(parentComponent);
		} else {
			Util.centerOnScreen(dialog);
		}

		dialog.setAlwaysOnTop(true);
		dialog.setVisible(true); // setVisible will wait until some button or so has been pressed

		if (dialogResult.result == DialogResult.Result.OK) {
			return passwordField.getPassword();
		}

		throw new CancelledException();
	}

	@Override
	public char[][] obtainPUKCodes(int retriesLeft) {
		Box mainPanel = Box.createVerticalBox();

		if (-1 != retriesLeft) {
			addWarningBox(mainPanel, messages.getMessage(Messages.MESSAGE_ID.RETRIES_LEFT) + ": " + retriesLeft);
		}

		JPasswordField puk1Field = new JPasswordField(8);
		Box puk1Panel = Box.createHorizontalBox();
		JLabel puk1Label = new JLabel("eID PUK1:");
		puk1Label.setLabelFor(puk1Field);
		puk1Panel.add(puk1Label);
		puk1Panel.add(Box.createHorizontalStrut(5));
		puk1Panel.add(puk1Field);
		mainPanel.add(puk1Panel);

		mainPanel.add(Box.createVerticalStrut(5));

		JPasswordField puk2Field = new JPasswordField(8);
		Box puk2Panel = Box.createHorizontalBox();
		JLabel puk2Label = new JLabel("eID PUK2:");
		puk2Label.setLabelFor(puk2Field);
		puk2Panel.add(puk2Label);
		puk2Panel.add(Box.createHorizontalStrut(5));
		puk2Panel.add(puk2Field);
		mainPanel.add(puk2Panel);

		int result = JOptionPane.showOptionDialog(parentComponent, mainPanel,
				"eID PIN unblock", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE,
				null, null, null);
		if (result != JOptionPane.OK_OPTION) {
			throw new RuntimeException(OPERATION_CANCELLED);
		}
		if (puk1Field.getPassword().length != PUK_SIZE || puk2Field.getPassword().length != PUK_SIZE) {
			throw new RuntimeException("PUK size incorrect");
		}
		char[] puk1 = new char[puk1Field.getPassword().length];
		char[] puk2 = new char[puk2Field.getPassword().length];
		System.arraycopy(puk1Field.getPassword(), 0, puk1, 0, puk1Field.getPassword().length);
		System.arraycopy(puk2Field.getPassword(), 0, puk2, 0, puk2Field.getPassword().length);
		Arrays.fill(puk1Field.getPassword(), (char) 0);
		Arrays.fill(puk2Field.getPassword(), (char) 0);
		return new char[][]{puk1, puk2};
	}

	@Override
	public void adviseSecureReaderOperation() {
		if (null != secureReaderTransactionFrame) {
			disposeSecureReaderFrame();
		}
		secureReaderTransactionFrame = new JFrame("Transaction Confirmation");
		JPanel panel = new JPanelWithInsets(new Insets(10, 30, 10, 30));
		BoxLayout boxLayout = new BoxLayout(panel, BoxLayout.PAGE_AXIS);
		panel.setLayout(boxLayout);
		panel.add(new JLabel("Check the transaction message on the secure card reader."));

		secureReaderTransactionFrame.getContentPane().add(panel);
		secureReaderTransactionFrame.pack();

		if (parentComponent != null) {
			secureReaderTransactionFrame.setLocationRelativeTo(parentComponent);
		} else {
			Util.centerOnScreen(secureReaderTransactionFrame);
		}

		secureReaderTransactionFrame.setAlwaysOnTop(true);
		secureReaderTransactionFrame.setVisible(true);
	}

	@Override
	public void adviseSecureReaderOperationEnd() {
		disposeSecureReaderFrame();
	}

	/*
	 * **********************************************************************************************************************
	 */

	private void addWarningBox(JComponent parent, String warningMessage) {
		parent.add(Box.createVerticalStrut(4));
		Box retriesPanel = createWarningBox(warningMessage);
		parent.add(retriesPanel);
		parent.add(Box.createVerticalStrut(24));
	}

	private Box createWarningBox(String warningText) {
		Box warningBox = Box.createHorizontalBox();
		JLabel warningLabel = new JLabel(warningText);
		warningLabel.setForeground(Color.RED);
		Icon warningIcon = UIManager.getIcon("OptionPane.warningIcon");
		if (warningIcon != null) warningLabel.setIcon(warningIcon);
		warningBox.add(warningLabel);
		warningBox.add(Box.createHorizontalGlue());
		warningBox.setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createLineBorder(Color.red, 1),
				BorderFactory.createEmptyBorder(8, 8, 8, 8))
		);
		return warningBox;
	}

	private Border createGenerousLowerBevelBorder() {
		return BorderFactory.createCompoundBorder(
				BorderFactory.createLoweredBevelBorder(),
				BorderFactory.createEmptyBorder(16, 16, 16, 16)
		);
	}

	private void showPINPadFrame(int retriesLeft, String title, String... messages) {
		if (null != pinPadFrame) {
			disposePINPadFrame();
		}
		pinPadFrame = new JFrame(title);
		JPanel panel = new JPanel();
		BoxLayout boxLayout = new BoxLayout(panel, BoxLayout.PAGE_AXIS);
		panel.setLayout(boxLayout);
		panel.setBorder(BorderFactory.createEmptyBorder(32, 32, 32, 32));

		if (messages.length > 0) {
			JLabel label = new JLabel(messages[0]);
			label.setAlignmentX((float) 0.5);
			panel.add(label);
		}

		if (-1 != retriesLeft) {
			panel.add(Box.createVerticalStrut(24));
			Box warningBox = createWarningBox(this.messages.getMessage(Messages.MESSAGE_ID.RETRIES_LEFT) + ": " + retriesLeft);
			panel.add(warningBox);
			panel.add(Box.createVerticalStrut(24));
		}

		for (int i = 1; i < messages.length; i++) {
			JLabel label = new JLabel(messages[i]);
			label.setAlignmentX((float) 0.5);
			panel.add(label);
		}

		pinPadFrame.getContentPane().add(panel);
		pinPadFrame.pack();

		if (parentComponent != null) {
			pinPadFrame.setLocationRelativeTo(parentComponent);
		} else {
			Util.centerOnScreen(pinPadFrame);
		}

		pinPadFrame.setAlwaysOnTop(true);
		pinPadFrame.setVisible(true);
	}

	private void disposePINPadFrame() {
		if (null != pinPadFrame) {
			pinPadFrame.dispose();
			pinPadFrame = null;
		}
	}

	private void disposeSecureReaderFrame() {
		if (null != secureReaderTransactionFrame) {
			secureReaderTransactionFrame.dispose();
			secureReaderTransactionFrame = null;
		}
	}

	private static class DialogResult {
		enum Result {
			OK, CANCEL
		}

		public Result result = null;
	}

	@Override
	public void setLocale(Locale newLocale) {
		this.locale = newLocale;
		this.messages = Messages.getInstance(newLocale);
	}

	public Locale getLocale() {
		return locale;
	}
}
