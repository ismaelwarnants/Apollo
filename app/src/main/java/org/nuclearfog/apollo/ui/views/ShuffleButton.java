package org.nuclearfog.apollo.ui.views;

import android.content.Context;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.view.View.OnLongClickListener;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageButton;
import androidx.core.content.ContextCompat;

import org.nuclearfog.apollo.R;
import org.nuclearfog.apollo.utils.ApolloUtils;
import org.nuclearfog.apollo.utils.Constants;
import org.nuclearfog.apollo.utils.MusicUtils;
import org.nuclearfog.apollo.utils.ThemeUtils;

/**
 * @author Andrew Neal (andrewdneal@gmail.com)
 * @author nuclearfog
 */
public class ShuffleButton extends AppCompatImageButton implements OnLongClickListener {

	/**
	 * @param context The {@link Context} to use
	 */
	public ShuffleButton(@NonNull Context context) {
		this(context, null);
	}

	/**
	 * @param context The {@link Context} to use
	 * @param attrs   The attributes of the XML tag that is inflating the view.
	 */
	public ShuffleButton(@NonNull Context context, @Nullable AttributeSet attrs) {
		super(context, attrs);
		// Theme the selector
		ThemeUtils mTheme = new ThemeUtils(context);
		mTheme.setBackgroundColor(this);
		// Show the cheat sheet
		setOnLongClickListener(this);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean onLongClick(View view) {
		if (TextUtils.isEmpty(view.getContentDescription()))
			return false;
		ApolloUtils.showCheatSheet(view);
		return true;
	}

	/**
	 * Sets the correct drawable for the shuffle state.
	 */
	public void updateShuffleState(int shuffleMode) {
		if (shuffleMode == MusicUtils.SHUFFLE_AUTO || shuffleMode == MusicUtils.SHUFFLE_NORMAL) {
			setContentDescription(getResources().getString(R.string.accessibility_shuffle_all));
			Drawable drawable = ContextCompat.getDrawable(getContext(), R.drawable.btn_playback_shuffle);
			if (drawable != null)
				drawable.setColorFilter(new PorterDuffColorFilter(Color.WHITE, PorterDuff.Mode.MULTIPLY));
			setImageDrawable(drawable);
		} else if (shuffleMode == MusicUtils.SHUFFLE_NONE) {
			setContentDescription(getResources().getString(R.string.accessibility_shuffle));
			Drawable drawable = ContextCompat.getDrawable(getContext(), R.drawable.btn_playback_shuffle);
			if (drawable != null)
				drawable.setColorFilter(new PorterDuffColorFilter(Color.WHITE & Constants.TRANSPARENCY_MASK_INACTIVE, PorterDuff.Mode.MULTIPLY));
			setImageDrawable(drawable);
		}
	}
}