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

package be.fedict.commons.eid.dialogs;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Toolkit;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.lang.reflect.InvocationTargetException;
import java.text.DateFormat;
import java.util.Collection;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.ListCellRenderer;
import javax.swing.SwingUtilities;
import be.fedict.commons.eid.client.BeIDCard;
import be.fedict.commons.eid.client.FileType;
import be.fedict.commons.eid.client.OutOfCardsException;
import be.fedict.commons.eid.client.CancelledException;
import be.fedict.commons.eid.client.event.BeIDCardListener;
import be.fedict.commons.eid.consumer.Identity;
import be.fedict.commons.eid.consumer.text.Format;
import be.fedict.commons.eid.consumer.tlv.TlvParser;

/**
 * Dynamically changing dialog listing BeIDCards by photo and main identity data
 * part of the DefaultBeIDCardsUI. Based on the original, static BeID selector
 * dialog from eid-applet.
 * 
 * @author Frank Marien
 * 
 */
public class BeIDSelector {

	private JDialog dialog;
	private JPanel masterPanel;
	private DefaultListModel<ListData> listModel;
	private JList<ListData> list;

	private final Component parentComponent;
	private final ListData selectedListData;
	private final Map<BeIDCard, ListDataUpdater> updaters;
	private int identitiesbeingRead;
	private boolean outOfCards;

	public BeIDSelector(Component parentComponent, String title, Collection<BeIDCard> initialCards) {
		this.parentComponent = parentComponent;
		this.selectedListData = new ListData(null);
		this.updaters = new HashMap<>();
		this.identitiesbeingRead = 0;
		this.outOfCards = false;

		initComponents(title);
		for (BeIDCard card : initialCards) {
			addEIDCard(card);
		}
		list.addMouseListener(new ListMouseAdapter());
	}

	public void addEIDCard(BeIDCard card) {
		ListData listData = new ListData(card);
		addToList(listData);
		ListDataUpdater listDataUpdater = new ListDataUpdater(this, listData);
		updaters.put(card, listDataUpdater);
		listDataUpdater.start();
	}

	public void removeEIDCard(BeIDCard card) {
		ListDataUpdater listDataUpdater = updaters.get(card);
		listDataUpdater.stop();
		updaters.remove(card);
		removeFromList(listDataUpdater.getListData());
	}

	public synchronized void startReadingIdentity() {
		identitiesbeingRead++;
		notifyAll();
	}

	public synchronized void endReadingIdentity() {
		identitiesbeingRead--;
		repack();
		notifyAll();
	}

	public synchronized void waitUntilIdentitiesRead() {
		try {
			while (identitiesbeingRead > 0) {
				wait();
			}
		} catch (InterruptedException iex) {
			throw new RuntimeException("Interrupted", iex);
		}
	}

	public void stop() {
		for (ListDataUpdater updater : updaters.values()) {
			updater.stop();
		}

		for (ListDataUpdater updater : updaters.values()) {
			try {
				updater.join();
			} catch (InterruptedException iex) {
				return;
			}
		}
	}

	public BeIDCard choose() throws OutOfCardsException, CancelledException {
		waitUntilIdentitiesRead();

		if (parentComponent != null) {
			dialog.setLocationRelativeTo(parentComponent);
		} else {
			Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
			dialog.setLocation(
					(screen.width - dialog.getSize().width) / 2,
					(screen.height - dialog.getSize().height) / 2);
		}

		dialog.setResizable(false);
		dialog.setAlwaysOnTop(true);
		dialog.setVisible(true);

		// dialog is modal so setVisible will block until dispose is called.
		// mouseListener calls dispose after setting selection, on double-click
		// removeFromList calls dispose after setting outOfCards when last card
		// removed
		// user closing dialog will have no selection and outOfCards not set
		// indicating cancel
		if (outOfCards) {
			throw new OutOfCardsException();
		}
		if (selectedListData.getCard() == null) {
			throw new CancelledException();
		}

		return selectedListData.getCard();
	}

	private void initComponents(String title) {
		try {
			SwingUtilities.invokeAndWait(() -> {
				dialog = new JDialog((Frame) null, title, true);
				masterPanel = new JPanel(new BorderLayout());
				masterPanel.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
				listModel = new DefaultListModel<>();
				list = new JList<>(listModel);
				list.setCellRenderer(new EidListCellRenderer());
				masterPanel.add(list);
				dialog.add(masterPanel);
			});
		} catch (InterruptedException | InvocationTargetException e) {
			throw new RuntimeException(e);
		}
	}

	private synchronized void updateListData(ListData listData) {
		SwingUtilities.invokeLater(() -> {
			int index = listModel.indexOf(listData);
			if (index != -1) {
				listModel.set(index, listData);
			}
		});
	}

	private void addToList(ListData listData) {
		SwingUtilities.invokeLater(() -> listModel.addElement(listData));
	}

	private void removeFromList(ListData listData) {
		SwingUtilities.invokeLater(() -> {
			listModel.removeElement(listData);
			if (listModel.isEmpty()) {
				selectedListData.card = null;
				outOfCards = true;
				dialog.dispose();
			} else {
				dialog.pack();
			}
		});
	}

	private void repack() {
		SwingUtilities.invokeLater(() -> dialog.pack());
	}

	private static class ListData {
		private BeIDCard card;
		private Identity identity;
		private ImageIcon photo;
		private int photoProgress, photoSizeEstimate;
		private boolean error;

		public ListData(BeIDCard card) {
			this.card = card;
		}

		public BeIDCard getCard() {
			return card;
		}

		public ImageIcon getPhoto() {
			return photo;
		}

		public Identity getIdentity() {
			return identity;
		}

		public void setIdentity(Identity identity) {
			this.identity = identity;
		}

		public void setPhoto(ImageIcon photo) {
			this.photo = photo;
		}

		public int getPhotoProgress() {
			return photoProgress;
		}

		public void setPhotoProgress(int photoProgress) {
			this.photoProgress = photoProgress;
		}

		public void setPhotoSizeEstimate(int photoSizeEstimate) {
			this.photoSizeEstimate = photoSizeEstimate;
		}

		public int getPhotoSizeEstimate() {
			return photoSizeEstimate;
		}

		public boolean hasError() {
			return error;
		}

		public void setError() {
			error = true;
		}
	}

	private static class EidListCellRenderer extends JPanel implements ListCellRenderer<ListData> {
		public Component getListCellRendererComponent(JList<? extends ListData> list, ListData listData, int index, boolean isSelected, boolean cellHasFocus) {
			JPanel panel = new JPanel();
			panel.setLayout(new FlowLayout(FlowLayout.LEFT));

			if (listData.hasError()) {
				panel.setBackground(redden(isSelected ? list.getSelectionBackground() : list.getBackground()));
			} else {
				panel.setBackground(isSelected ? list.getSelectionBackground() : list.getBackground());
			}

			panel.add(new PhotoPanel(listData.getPhoto(), listData.getPhotoProgress(), listData.getPhotoSizeEstimate()));
			panel.add(new IdentityPanel(listData.getIdentity()));

			return panel;
		}

		private Color redden(Color originalColor) {
			Color less = originalColor.darker().darker();
			Color more = originalColor.brighter().brighter();
			return new Color(more.getRed(), less.getGreen(), less.getBlue());
		}
	}

	private static class PhotoPanel extends JPanel {
		private JProgressBar progressBar;

		public PhotoPanel(ImageIcon photo, int progress, int max) {
			super(new GridBagLayout());
			setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
			Dimension fixedSize = new Dimension(140, 200);
			setPreferredSize(fixedSize);
			setMinimumSize(fixedSize);
			setMaximumSize(fixedSize);

			if (photo == null) {
				progressBar = new JProgressBar(0, max);
				progressBar.setIndeterminate(false);
				progressBar.setValue(progress);
				fixedSize = new Dimension(100, 16);
				progressBar.setPreferredSize(fixedSize);
				add(progressBar);
			} else {
				add(new JLabel(photo));
			}
		}
	}

	private static class IdentityPanel extends JPanel {
		public IdentityPanel(Identity identity) {
			super(new GridBagLayout());
			setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
			setMinimumSize(new Dimension(140, 200));
			setOpaque(false);

			if (identity == null) {
				add(new JLabel("-"));
			} else {
				GridBagConstraints gbc = new GridBagConstraints();
				gbc.gridy = 0;
				gbc.anchor = GridBagConstraints.LINE_START;
				gbc.ipady = 4;
				add(new JLabel(identity.getName()), gbc);

				gbc = new GridBagConstraints();
				gbc.gridy = 1;
				gbc.anchor = GridBagConstraints.LINE_START;
				gbc.ipady = 4;
				add(new JLabel(identity.getFirstName() + " " + identity.getMiddleName()), gbc);

				gbc = new GridBagConstraints();
				gbc.gridy = 2;
				gbc.ipady = 8;
				add(Box.createVerticalGlue(), gbc);

				gbc = new GridBagConstraints();
				gbc.gridy = 3;
				gbc.anchor = GridBagConstraints.LINE_START;
				gbc.ipady = 4;
				DateFormat dateFormat = DateFormat.getDateInstance(DateFormat.DEFAULT, Locale.getDefault());
				add(new JLabel(identity.getPlaceOfBirth() + " " + dateFormat.format(identity.getDateOfBirth().getTime())), gbc);

				gbc = new GridBagConstraints();
				gbc.gridy = 4;
				gbc.ipady = 8;
				add(Box.createVerticalGlue(), gbc);

				gbc = new GridBagConstraints();
				gbc.gridy = 5;
				gbc.anchor = GridBagConstraints.LINE_START;
				gbc.ipady = 4;
				add(new JLabel(identity.getNationality().toUpperCase()), gbc);

				gbc = new GridBagConstraints();
				gbc.gridy = 6;
				gbc.ipady = 8;
				add(Box.createVerticalGlue(), gbc);

				gbc = new GridBagConstraints();
				gbc.gridy = 7;
				gbc.anchor = GridBagConstraints.LINE_START;
				gbc.ipady = 4;
				add(new JLabel(Format.formatCardNumber(identity.getCardNumber())), gbc);
			}
		}
	}

	private static class ListDataUpdater implements Runnable {
		final private BeIDSelector selectionDialog;
		final private ListData listData;
		final private Thread worker;

		public ListDataUpdater(BeIDSelector selectionDialog, ListData listData) {
			this.selectionDialog = selectionDialog;
			this.listData = listData;
			this.worker = new Thread(this, "ListDataUpdater");
			this.worker.setDaemon(true);
			setWorkerName(null, null);
			this.selectionDialog.startReadingIdentity();
		}

		public void stop() {
			worker.interrupt();
		}

		public void start() {
			worker.start();
		}

		public void join() throws InterruptedException {
			worker.join();
		}

		@Override
		public void run() {
			Identity identity = null;
			setWorkerName(null, "Reading Identity");

			try {
				identity = TlvParser.parse(listData.getCard().readFile(FileType.Identity), Identity.class);
				listData.setIdentity(identity);
				selectionDialog.updateListData(listData);
				setWorkerName(identity, "Identity Read");
			} catch (Exception ex) {
				listData.setError();
				selectionDialog.updateListData(listData);
				setWorkerName(identity, "Error Reading Identity");
			} finally {
				selectionDialog.endReadingIdentity();
			}

			setWorkerName(identity, "Reading Photo");

			try {
				listData.setPhotoSizeEstimate(FileType.Photo.getEstimatedMaxSize());
				selectionDialog.updateListData(listData);

				listData.getCard().addCardListener(new BeIDCardListener() {
					@Override
					public void notifyReadProgress(FileType fileType, int offset, int estimatedMaxSize) {
						listData.setPhotoProgress(offset);
						selectionDialog.updateListData(listData);
					}

					@Override
					public void notifySigningBegin(FileType keyType) {
						// can safely ignore this here
					}

					@Override
					public void notifySigningEnd(FileType keyType) {
						// can safely ignore this here
					}
				});

				byte[] photoFile = listData.getCard().readFile(FileType.Photo);
				BufferedImage photoImage = ImageIO.read(new ByteArrayInputStream(photoFile));
				listData.setPhoto(new ImageIcon(photoImage));
				selectionDialog.updateListData(listData);
				setWorkerName(identity, "All Done");
			} catch (Exception ex) {
				listData.setError();
				selectionDialog.updateListData(listData);
				setWorkerName(identity, "Error Reading Photo");
			}
		}

		private void setWorkerName(Identity identity, String activity) {
			StringBuilder builder = new StringBuilder("ListDataUpdater");

			if (identity != null) {
				builder.append(" [");
				if (identity.getFirstName() != null) {
					builder.append(identity.getFirstName());
					builder.append(" ");
				}

				if (identity.getName() != null) {
					builder.append(identity.getName());
				}

				builder.append("]");
			}

			if (activity != null) {
				builder.append(" [");
				builder.append(activity);
				builder.append("]");
			}

			worker.setName(builder.toString());
		}

		public ListData getListData() {
			return listData;
		}
	}

	private class ListMouseAdapter extends MouseAdapter {
		public void mouseClicked(MouseEvent mouseEvent) {
			JList theList = (JList) mouseEvent.getSource();
			if (mouseEvent.getClickCount() == 2) {
				int index = theList.locationToIndex(mouseEvent.getPoint());
				if (index >= 0) {
					stop();
					Object object = theList.getModel().getElementAt(index);
					ListData listData = (ListData) object;
					selectedListData.card = listData.getCard();
					dialog.dispose();
				}
			}
		}
	}
}
