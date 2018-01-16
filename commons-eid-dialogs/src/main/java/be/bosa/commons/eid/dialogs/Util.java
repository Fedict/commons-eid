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

package be.bosa.commons.eid.dialogs;

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
