package org.nuclearfog.apollo.ui.views;

import android.content.Context;
import android.content.res.TypedArray;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnLongClickListener;
import android.view.View.OnTouchListener;

import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageButton;

import org.nuclearfog.apollo.R;
import org.nuclearfog.apollo.utils.ApolloUtils;
import org.nuclearfog.apollo.utils.ThemeUtils;

import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * A custom {@link AppCompatImageButton} that represents the "previous" & "next" button
 * also supporting "forward" & "rewind" by long pressing
 *
 * @author nuclearfog
 */
public class ForwardRewindButton extends AppCompatImageButton implements OnTouchListener, OnLongClickListener {

	private static final long S_INTERVAL = 400;

	private ScheduledExecutorService threadPool = Executors.newSingleThreadScheduledExecutor();
	private Handler mHandler;
	private Future<?> task;

	@Nullable
	private RepeatListener mListener;

	@IntRange(from = 0)
	private int mRepeatCount = 0;

	/**
	 * {@inheritDoc}
	 */
	public ForwardRewindButton(@NonNull Context context) {
		this(context, null);
	}

	/**
	 * {@inheritDoc}
	 */
	public ForwardRewindButton(@NonNull Context context, @Nullable AttributeSet attrs) {
		super(context, attrs);
		setScaleType(ScaleType.CENTER_CROP);
		// Theme the selector
		ThemeUtils mTheme = new ThemeUtils(context);
		mHandler = new Handler(context.getMainLooper());
		TypedArray ta = getContext().obtainStyledAttributes(attrs, R.styleable.ForwardRewindButton);
		int mode = ta.getInt(R.styleable.ForwardRewindButton_button, 0);
		ta.recycle();
		mTheme.setBackgroundColor(this);
		setFocusable(true);
		// set button icon
		if (mode == 1) {
			setImageResource(R.drawable.btn_playback_next);
			setContentDescription(getContext().getString(R.string.accessibility_next));
		} else if (mode == 2) {
			setImageResource(R.drawable.btn_playback_previous);
			setContentDescription(getContext().getString(R.string.accessibility_prev));
		}

		setOnTouchListener(this);
		setOnLongClickListener(this);
	}


	@Override
	public final boolean onTouch(View v, MotionEvent event) {
		switch (event.getActionMasked()) {
			case MotionEvent.ACTION_DOWN:
				// start periodic method call
				if (mListener != null) {
					task = threadPool.scheduleWithFixedDelay(() -> mHandler.post(this::refresh), S_INTERVAL, S_INTERVAL, TimeUnit.MILLISECONDS);
				}
				break;

			case MotionEvent.ACTION_UP:
				// stop periodic method call
				if (task != null) {
					task.cancel(true);
					task = null;
				}
				// check if button was long pressed
				if (mRepeatCount > 0) {
					refresh();
					mRepeatCount = 0;
					// reset pressed state manually (not done automatically after returning true)
					setPressed(false);
					return true;
				}
				break;
		}
		return false;
	}


	@Override
	public final boolean onLongClick(View v) {
		if (mListener == null) {
			ApolloUtils.showCheatSheet(this);
			return true;
		}
		return false;
	}

	/**
	 * Sets the listener to be called while the button is pressed and the
	 * interval in milliseconds with which it will be called.
	 *
	 * @param l The listener that will be called
	 */
	public void setRepeatListener(@Nullable RepeatListener l) {
		mListener = l;
	}

	/**
	 *
	 */
	private void refresh() {
		if (mListener != null) {
			mListener.onRepeat(this, mRepeatCount++);
		}
	}

	/**
	 * Click listener for this button
	 */
	public interface RepeatListener {

		/**
		 * @param v           View to be set
		 * @param repeatCount The number of repeat counts
		 */
		void onRepeat(View v, int repeatCount);
	}
}