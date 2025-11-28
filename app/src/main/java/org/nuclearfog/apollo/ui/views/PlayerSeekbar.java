package org.nuclearfog.apollo.ui.views;

import android.content.Context;
import android.graphics.PorterDuff;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.nuclearfog.apollo.R;
import org.nuclearfog.apollo.utils.AnimatorUtils;
import org.nuclearfog.apollo.utils.PreferenceUtils;
import org.nuclearfog.apollo.utils.StringUtils;

import java.lang.ref.WeakReference;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Custom view providing a seekbar for the player and progress/duration viewer
 *
 * @author nuclearfog
 */
public class PlayerSeekbar extends LinearLayout implements OnSeekBarChangeListener {

	private TextView posText, durText;
	private SeekBar seekbar;

	@Nullable
	private OnPlayerSeekListener listener;
	private Future<?> updateTask;

	/**
	 * current position of the seekbar in milliseconds
	 */
	private long position = 0;

	/**
	 * duration of the seekbar in milliseconds
	 */
	private long duration = 0;

	/**
	 * set to true to enable automatic seekbar updates
	 */
	private boolean updateSeekbar = false;

	/**
	 * thread pool used to run a task to periodically update seekbar and time
	 */
	private ScheduledExecutorService threadPool = Executors.newSingleThreadScheduledExecutor();

	private Runnable mUpdater = new Updater(this);

	/**
	 *
	 */
	public PlayerSeekbar(@NonNull Context context) {
		this(context, null);
	}

	/**
	 *
	 */
	public PlayerSeekbar(@NonNull Context context, @Nullable AttributeSet attrs) {
		super(context, attrs);
		seekbar = new SeekBar(context);
		posText = new TextView(context);
		durText = new TextView(context);
		PreferenceUtils mPrefs = PreferenceUtils.getInstance(context);
		LayoutParams seekbarParam = new LayoutParams(0, LayoutParams.WRAP_CONTENT, 8.0f);
		LayoutParams textParam = new LayoutParams(0, LayoutParams.WRAP_CONTENT, 1.0f);
		int color = getResources().getColor(R.color.audio_player_current_time);
		float textSize = getResources().getDimension(R.dimen.text_size_micro);
		int themeColor = mPrefs.getThemeColor();
		// configure position and duration time views
		posText.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize);
		durText.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize);
		posText.setGravity(Gravity.CENTER);
		durText.setGravity(Gravity.CENTER);
		posText.setTextColor(color);
		durText.setTextColor(color);
		posText.setSingleLine();
		durText.setSingleLine();
		posText.setLayoutParams(textParam);
		durText.setLayoutParams(textParam);
		// configure seekbar
		seekbar.getProgressDrawable().setColorFilter(themeColor, PorterDuff.Mode.SRC_IN);
		seekbar.getThumb().setColorFilter(themeColor, PorterDuff.Mode.SRC_IN);
		seekbar.setMax(1000);
		seekbar.setLayoutParams(seekbarParam);
		seekbar.setOnSeekBarChangeListener(this);
		// configure root view
		setOrientation(HORIZONTAL);
		setGravity(Gravity.CENTER);
		addView(posText);
		addView(seekbar);
		addView(durText);
		// init current time view
		updatePositionText();
		updateDurationText();
	}


	@Override
	public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
		if (listener != null && fromUser) {
			position = (duration * progress) / 1000L;
			updatePositionText();
			listener.onSeek(position);
		}
	}


	@Override
	public void onStartTrackingTouch(SeekBar seekBar) {
		updateSeekbar = false;
	}


	@Override
	public void onStopTrackingTouch(SeekBar seekBar) {
		updateSeekbar = true;
	}

	/**
	 * clean up resources associated with this view
	 */
	public void release() {
		threadPool.shutdown();
	}

	/**
	 * set time for seekbar
	 *
	 * @param time time in milliseconds
	 */
	public void setCurrentTime(@IntRange(from = 0) long time) {
		position = Math.min(time, duration);
		updatePositionText();
		updateSeekbarPosition();
	}

	/**
	 * set duration of the seekbar
	 *
	 * @param time time in milliseconds
	 */
	public void setTotalTime(@IntRange(from = 0) long time) {
		duration = time;
		updateDurationText();
		seek(0);
	}

	/**
	 * seek to a new position
	 *
	 * @param time time in milliseconds
	 */
	public void seek(@IntRange(from = 0) long time) {
		if (duration > 0) {
			position = Math.min(time, duration);
			updateSeekbarPosition();
		} else {
			position = 0;
			updateSeekbarPosition();
		}
		updatePositionText();
	}

	/**
	 * enable automatic seekbar moving
	 *
	 * @param isPlaying true to move the seekbar automatically
	 */
	public void setPlayStatus(boolean isPlaying) {
		updateSeekbar = isPlaying;
		AnimatorUtils.pulse(posText, !isPlaying);
		if (isPlaying) {
			if (updateTask == null)
				updateTask = threadPool.scheduleWithFixedDelay(mUpdater, Updater.CYCLE_MS, Updater.CYCLE_MS, TimeUnit.MILLISECONDS);
		} else if (updateTask != null) {
			updateTask.cancel(true);
			updateTask = null;
		}
	}

	/**
	 * set listener to call when the user interact with the seekbar
	 */
	public void setOnPlayerSeekListener(OnPlayerSeekListener listener) {
		this.listener = listener;
	}

	/**
	 * update current position time
	 */
	private void updatePositionText() {
		posText.setText(StringUtils.makeTimeString(getContext(), position));
	}

	/**
	 * update duration time
	 */
	private void updateDurationText() {
		durText.setText(StringUtils.makeTimeString(getContext(), duration));
	}

	/**
	 * update seekbar position
	 */
	private void updateSeekbarPosition() {
		if (duration > 0) {
			seekbar.setProgress((int) (1000L * position / duration));
		}
	}

	/**
	 * called periodically by {@link Updater} to update the seekbar position
	 */
	private void update() {
		if (updateSeekbar) {
			position = Math.min(position + Updater.CYCLE_MS, duration);
			seek(position);
		}
	}

	/**
	 * Listener interface used to update player position after user interaction
	 */
	public interface OnPlayerSeekListener {

		/**
		 * called to seek to a specific position
		 *
		 * @param position position in milliseconds
		 */
		void onSeek(long position);
	}

	/**
	 * Runnable used to update the seekbar position periodically
	 */
	private static final class Updater implements Runnable {

		/**
		 * time to refresh seekbar/position
		 */
		static final int CYCLE_MS = 500;

		private WeakReference<PlayerSeekbar> callback;

		/**
		 * @param player callback to this view
		 */
		Updater(PlayerSeekbar player) {
			callback = new WeakReference<>(player);
		}


		@Override
		public void run() {
			PlayerSeekbar seekbar = callback.get();
			if (seekbar != null) {
				seekbar.update();
			}
		}
	}
}