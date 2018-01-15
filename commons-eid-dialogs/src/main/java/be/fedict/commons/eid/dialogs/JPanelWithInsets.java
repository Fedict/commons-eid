package be.fedict.commons.eid.dialogs;

import javax.swing.*;
import java.awt.*;

class JPanelWithInsets extends JPanel {
	private final Insets insets;

	public JPanelWithInsets(Insets insets) {
		this.insets = insets;
	}

	public JPanelWithInsets(LayoutManager layoutManager, Insets insets) {
		super(layoutManager);
		this.insets = insets;
	}

	@Override
	public Insets getInsets() {
		return insets;
	}
}
