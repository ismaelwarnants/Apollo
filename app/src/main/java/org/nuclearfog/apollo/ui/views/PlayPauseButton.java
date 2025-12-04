package org.nuclearfog.apollo.ui.views;

import android.content.Context;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.view.View.OnLongClickListener;
import android.widget.ImageButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageButton;

import org.nuclearfog.apollo.R;
import org.nuclearfog.apollo.utils.ApolloUtils;
import org.nuclearfog.apollo.utils.ThemeUtils;

/**
 * A custom {@link ImageButton} that represents the "play and pause" button.
 *
 * @author Andrew Neal (andrewdneal@gmail.com)
 */
public class PlayPauseButton extends AppCompatImageButton implements OnLongClickListener {

	/**
	 * @param context The {@link Context} to use
	 */
	public PlayPauseButton(@NonNull Context context) {
		this(context, null);
	}

	/**
	 * @param context The {@link Context} to use
	 * @param attrs   The attributes of the XML tag that is inflating the view.
	 */
	public PlayPauseButton(@NonNull Context context, @Nullable AttributeSet attrs) {
		super(context, attrs);
		// Theme the selector
		ThemeUtils mTheme = new ThemeUtils(context);
		mTheme.setBackgroundColor(this);
		setOnLongClickListener(this);
		updateState(false);
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
	 * Sets the correct drawable for playback.
	 */
	public void updateState(boolean play) {
		if (play) {
			setContentDescription(getResources().getString(R.string.accessibility_pause));
			setImageResource(R.drawable.btn_playback_pause);
		} else {
			setContentDescription(getResources().getString(R.string.accessibility_play));
			setImageResource(R.drawable.btn_playback_play);
		}
		requestLayout();
	}
}