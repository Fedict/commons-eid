package be.fedict.commons.eid.dialogs;

import java.awt.*;

public class Util {

	private Util() {}

	public static void centerOnScreen(Window dialog) {
		GraphicsEnvironment graphicsEnvironment = GraphicsEnvironment.getLocalGraphicsEnvironment();
		GraphicsDevice graphicsDevice = graphicsEnvironment.getDefaultScreenDevice();
		DisplayMode displayMode = graphicsDevice.getDisplayMode();
		int screenWidth = displayMode.getWidth();
		int screenHeight = displayMode.getHeight();
		int dialogWidth = dialog.getWidth();
		int dialogHeight = dialog.getHeight();
		dialog.setLocation((screenWidth - dialogWidth) / 2, (screenHeight - dialogHeight) / 2);
	}

}
