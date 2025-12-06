package org.nuclearfog.apollo.ui.views;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.view.View.OnLongClickListener;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageButton;

import org.nuclearfog.apollo.R;
import org.nuclearfog.apollo.utils.ApolloUtils;
import org.nuclearfog.apollo.utils.Constants;
import org.nuclearfog.apollo.utils.MusicUtils;
import org.nuclearfog.apollo.utils.ThemeUtils;

/**
 * A custom {@link AppCompatImageButton} that represents the "shuffle" & "repeat" button.
 *
 * @author Andrew Neal (andrewdneal@gmail.com)
 * @author nuclearfog
 */
public class ShuffleRepeatButton extends AppCompatImageButton implements OnLongClickListener {

	/**
	 * {@inheritDoc}
	 */
	public ShuffleRepeatButton(@NonNull Context context) {
		this(context, null);
	}

	/**
	 * {@inheritDoc}
	 */
	public ShuffleRepeatButton(@NonNull Context context, @Nullable AttributeSet attrs) {
		super(context, attrs);
		// Set the selector
		ThemeUtils mTheme = new ThemeUtils(context);
		mTheme.setBackgroundColor(this);
		setScaleType(ScaleType.CENTER_INSIDE);
		TypedArray ta = getContext().obtainStyledAttributes(attrs, R.styleable.ShuffleRepeatButton);
		int mode = ta.getInt(R.styleable.ForwardRewindButton_button, 0);
		ta.recycle();
		if (mode == 3) {
			updateButtonState(MusicUtils.SHUFFLE_NONE);
		} else if (mode == 4) {
			updateButtonState(MusicUtils.REPEAT_NONE);
		}
		// Show the cheat sheet
		setOnLongClickListener(this);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public final boolean onLongClick(@NonNull View view) {
		if (TextUtils.isEmpty(view.getContentDescription()))
			return false;
		ApolloUtils.showCheatSheet(view);
		return true;
	}

	/**
	 * Sets the correct drawable for the repeat state.
	 *
	 * @param mode repeat/shuffle mode state {@link MusicUtils#REPEAT_NONE,MusicUtils#REPEAT_CURRENT,MusicUtils#REPEAT_ALL,MusicUtils#SHUFFLE_AUTO,MusicUtils#SHUFFLE_NONE,MusicUtils#SHUFFLE_NORMAL}
	 */
	public void updateButtonState(int mode) {
		int color = Color.WHITE;
		switch (mode) {
			case MusicUtils.REPEAT_ALL:
				setContentDescription(getContext().getString(R.string.accessibility_repeat_all));
				setImageResource(R.drawable.btn_playback_repeat);
				break;

			case MusicUtils.REPEAT_CURRENT:
				setContentDescription(getContext().getString(R.string.accessibility_repeat_one));
				setImageResource(R.drawable.btn_playback_repeat_one);
				break;

			case MusicUtils.REPEAT_NONE:
				setContentDescription(getContext().getString(R.string.accessibility_repeat));
				setImageResource(R.drawable.btn_playback_repeat);
				color &= Constants.TRANSPARENCY_MASK_INACTIVE;
				break;

			case MusicUtils.SHUFFLE_AUTO:
			case MusicUtils.SHUFFLE_NORMAL:
				setContentDescription(getResources().getString(R.string.accessibility_shuffle_all));
				setImageResource(R.drawable.btn_playback_shuffle);
				break;

			case MusicUtils.SHUFFLE_NONE:
				setContentDescription(getResources().getString(R.string.accessibility_shuffle));
				setImageResource(R.drawable.btn_playback_shuffle);
				color &= Constants.TRANSPARENCY_MASK_INACTIVE;
				break;
		}
		if (getDrawable() != null) {
			getDrawable().setColorFilter(new PorterDuffColorFilter(color, PorterDuff.Mode.MULTIPLY));
		}
	}
}