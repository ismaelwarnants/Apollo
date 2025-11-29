package org.nuclearfog.apollo.ui.adapters.listview.holder;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import org.nuclearfog.apollo.R;

/**
 * Used to efficiently cache and recycle the {@link View}s used in the artist,
 * album, song, playlist, and genre adapters.
 *
 * @author Andrew Neal (andrewdneal@gmail.com)
 */
public class MusicHolder {

	/**
	 * This is the artist or album image
	 */
	public final ImageView mImage;

	/**
	 * This is the first line displayed in the list or grid
	 * <p>
	 * {@code #getView()} of a specific adapter for more detailed info
	 */
	public final TextView mLineOne;

	/**
	 * This is displayed on the right side of the first line in the list or grid
	 * <p>
	 * {@code #getView()} of a specific adapter for more detailed info
	 */
	public final TextView mLineOneRight;

	/**
	 * This is the second line displayed in the list or grid
	 * <p>
	 * {@code #getView()} of a specific adapter for more detailed info
	 */
	public final TextView mLineTwo;

	/**
	 * This is the third line displayed in the list or grid
	 * <p>
	 * {@code #getView()} of a specific adapter for more detailed info
	 */
	public final TextView mLineThree;

	/**
	 *
	 */
	public MusicHolder(View view) {
		super();

		// Initialize mImage
		mImage = view.findViewById(R.id.image);

		// Initialize mLineOne
		mLineOne = view.findViewById(R.id.line_one);

		// Initialize mLineOneRight
		mLineOneRight = view.findViewById(R.id.line_one_right);

		// Initialize mLineTwo
		mLineTwo = view.findViewById(R.id.line_two);

		// Initialize mLineThree
		mLineThree = view.findViewById(R.id.line_three);
	}
}