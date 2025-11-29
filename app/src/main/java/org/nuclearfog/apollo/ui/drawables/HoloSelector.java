package org.nuclearfog.apollo.ui.drawables;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.StateListDrawable;

/**
 * A themable {@link StateListDrawable}.
 *
 * @author Andrew Neal (andrewdneal@gmail.com)
 * @author nuclearfog
 */
public class HoloSelector extends StateListDrawable {

	/**
	 * Button states
	 */
	private static final int[][] STATES = {
			{android.R.attr.state_focused},
			{android.R.attr.state_pressed},
			{}
	};

	/**
	 * @param color color when the button is pressed
	 */
	public HoloSelector(int color) {
		// Focused
		addState(STATES[0], new ColorDrawable(color));
		// Pressed
		addState(STATES[1], new ColorDrawable(color));
		// Default
		addState(STATES[2], new ColorDrawable(Color.TRANSPARENT));
		setExitFadeDuration(400);
	}
}