package org.nuclearfog.apollo.ui.views;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;

import org.nuclearfog.apollo.utils.PreferenceUtils;

/**
 * Used as a thin strip placed just above the bottom action bar or just below
 * the top action bar.
 *
 * @author Andrew Neal (andrewdneal@gmail.com)
 */
public class ColorStrip extends View {

	/**
	 * {@inheritDoc}
	 */
	public ColorStrip(Context context, AttributeSet attrs) {
		super(context, attrs);
		// Theme the color strip
		int color = PreferenceUtils.getInstance(context).getThemeColor();
		setBackgroundColor(color);
	}
}