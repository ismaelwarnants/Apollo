package org.nuclearfog.apollo.model;

import android.graphics.drawable.Drawable;

import androidx.annotation.Nullable;

/**
 * @author nuclearfog
 */
public class AppTheme {

	/**
	 * theme name
	 */
	public final String mName;

	/**
	 * preview drawable
	 */
	@Nullable
	public final Drawable mPreview;

	/**
	 *
	 */
	public AppTheme(String name, @Nullable Drawable prev) {
		mName = name;
		mPreview = prev;
	}
}