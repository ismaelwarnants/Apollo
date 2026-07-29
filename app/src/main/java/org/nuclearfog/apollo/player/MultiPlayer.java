package org.nuclearfog.apollo.player;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.PowerManager;
import android.util.Log;

import androidx.annotation.FloatRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.nuclearfog.apollo.store.preferences.AppPreferences;

import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * custom MediaPlayer implementation containing multiple MediaPlayer to switch fast tracks.
 *
 * @author nuclearfog
 */
public class MultiPlayer {

	private static final String TAG = "MultiPlayer";
	/**
	 * attribution tag declared in AndroidManifest
	 */
	private static final String ATTR_TAG = "audio_playback";
	/**
	 * indicates that there is no fade in/out in progress
	 */
	private static final int FADE_IDLE = 9;
	/**
	 * indicates that the current track is fading in
	 */
	private static final int FADE_IN = 10;
	/**
	 * indicates that the current track is fading out
	 */
	private static final int FADE_OUT = 11;
	/**
	 * indicates that the current track is fading out. The next track will be faded in
	 */
	private static final int FADE_OUT_IN = 12;
	/**
	 * volume steps used to fade in or out
	 */
	private static final float FADE_STEPS = 0.1f;
	/**
	 * duration of fade effect in ms
	 */
	private static final long FADE_DELAY = 500;
	/**
	 * duration of one volume step of the fade effect
	 */
	private static final long FADE_RESOLUTION = Math.round(FADE_DELAY * FADE_STEPS);
	/**
	 * number of player instances used for playback
	 *
	 * @see #mPlayers
	 */
	private static final int PLAYER_INST = 3;
	/**
	 * milliseconds to wait until to retry loading track
	 */
	private static final int ERROR_RETRY = 1000;
	/**
	 * Thread pool used to periodically poll the current play position for audio fade effect
	 */
	private ScheduledExecutorService threadPool = Executors.newSingleThreadScheduledExecutor();
	/**
	 * used to stop fade task if playback is paused or fade effect is disabled
	 */
	@Nullable
	private Future<?> fadeTask;
	/**
	 * handler used to run tasks in the player'S thread
	 */
	private Handler playerHandler;
	/**
	 * mediaplayer used to switch between tracks
	 */
	private MediaPlayer[] mPlayers = new MediaPlayer[PLAYER_INST];
	/**
	 * used to check if media file is valid
	 */
	private MediaMetadataRetriever metadataLoader = new MediaMetadataRetriever();
	/**
	 * current mediaplayer's index of {@link #mPlayers}
	 */
	private int selectedPlayer = 0;
	/**
	 * true if mediaplayer is currently playing
	 */
	private boolean isPlaying = false;
	/**
	 * set to true if player was initialized successfully
	 */
	private boolean initialized = false;
	/**
	 * true if player continues to next track automatically
	 */
	private boolean continuous = true;
	/**
	 * current fade in/out status {@link #FADE_IDLE ,#FADE_IN,#FADE_OUT}
	 */
	private int fadeMode = FADE_IDLE;
	/**
	 * volume of the current selected media player
	 */
	@FloatRange(from = 0f, to = 1f)
	private float volume = 0f;
	/**
	 * volume limit
	 */
	@FloatRange(from = 0f, to = 1f)
	private float maxVolume = 1f;
	/**
	 * enable/disable fade in/out effect
	 */
	private boolean fadeEffectEnabled;

	/**
	 * current media session ID of the playback
	 */
	private final int sessionId;

	/**
	 * callback for playback changes
	 */
	private OnPlaybackStatusCallback callback;

	/**
	 * @param context  context from service
	 * @param callback a callback used to inform about playback changes
	 */
	public MultiPlayer(Context context, int sessionId, OnPlaybackStatusCallback callback) {
		playerHandler = new Handler(context.getMainLooper());
		fadeEffectEnabled = AppPreferences.getInstance(context).fadeEffectEnabled();
		this.callback = callback;
		this.sessionId = sessionId;
		AudioAttributes audioAttributes = new AudioAttributes.Builder()
				.setUsage(AudioAttributes.USAGE_MEDIA)
				.setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
				.build();
		for (int i = 0; i < mPlayers.length; i++) {
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
				mPlayers[i] = new MediaPlayer(context.createAttributionContext(ATTR_TAG));
			} else {
				mPlayers[i] = new MediaPlayer();
			}
			mPlayers[i].setAudioAttributes(audioAttributes);
			mPlayers[i].setAudioSessionId(sessionId);
			mPlayers[i].setWakeMode(context.getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);
			mPlayers[i].setOnCompletionListener(this::onCompletion);
			mPlayers[i].setOnErrorListener(this::onError);
		}
		if (fadeEffectEnabled) {
			setAllVolume(0f);
		}
	}

	/**
	 * get audio session ID of the player
	 *
	 * @return audio session ID used by this players
	 */
	public int getSessionId() {
		return sessionId;
	}

	/**
	 * @param uri The path of the file, or the http/rtsp URL of the stream you want to play
	 * @return true if player is ready to play
	 */
	public synchronized boolean setDataSource(Context context, @NonNull Uri uri) {
		// set source of the current selected player
		MediaPlayer current = mPlayers[selectedPlayer];
		initialized = setDataSourceImpl(current, context, uri);
		return initialized;
	}

	/**
	 * Set the MediaPlayer to start when this MediaPlayer finishes playback.
	 *
	 * @param uri The path of the file, or the http/rtsp URL of the stream you want to play
	 * @return true if next data source is initialized successfully
	 */
	public synchronized boolean setNextDataSource(Context context, @Nullable Uri uri) {
		MediaPlayer current = mPlayers[selectedPlayer];
		MediaPlayer next = mPlayers[(selectedPlayer + 1) % mPlayers.length];
		if (uri != null) {
			continuous = setDataSourceImpl(next, context, uri);
			if (initialized) {
				if (continuous) {
					current.setNextMediaPlayer(next);
				} else {
					current.setNextMediaPlayer(null);
				}
			}
			return continuous;
		} else {
			if (initialized) {
				current.setNextMediaPlayer(null);
			}
			continuous = false;
			return true;
		}
	}

	/**
	 * @return True if the player is ready to go, false otherwise
	 */
	public synchronized boolean initialized() {
		return initialized;
	}

	/**
	 * Starts or resumes playback.
	 */
	public synchronized void play() {
		MediaPlayer player = mPlayers[selectedPlayer];
		try {
			if (!player.isPlaying()) {
				isPlaying = true;
				if (!fadeEffectEnabled) {
					setFadeTask(false);
					setCurrentVolume(1f);
				} else {
					fadeMode = FADE_IN;
					setCurrentVolume(0f);
					setFadeTask(true);
				}
				player.start();
			}
		} catch (IllegalStateException exception) {
			Log.e(TAG, "play()", exception);
			reset();
		}
	}

	/**
	 * Pauses playback. Call start() to resume.
	 *
	 * @param force true to stop playback immediately
	 */
	public synchronized void pause(boolean force) {
		MediaPlayer player = mPlayers[selectedPlayer];
		try {
			isPlaying = false;
			if (force || !fadeEffectEnabled) {
				setFadeTask(false);
				if (player.isPlaying())
					player.pause();
				fadeMode = FADE_IDLE;
			} else {
				fadeMode = FADE_OUT;
			}
		} catch (IllegalStateException exception) {
			Log.e(TAG, "pause()", exception);
			reset();
		}
	}

	/**
	 * stops playback
	 */
	public synchronized void stop() {
		MediaPlayer player = mPlayers[selectedPlayer];
		try {
			setFadeTask(false);
			player.stop();
			isPlaying = false;
			if (initialized) {
				player.prepare();
				player.seekTo(0);
			}
		} catch (IllegalStateException | IOException exception) {
			Log.e(TAG, "stop()", exception);
			reset();
		}
	}

	/**
	 * Releases media player
	 */
	public synchronized void release() {
		threadPool.shutdown();
		for (MediaPlayer player : mPlayers) {
			player.release();
		}
	}

	/**
	 * Gets the duration of the file.
	 *
	 * @return The duration in milliseconds
	 */
	public synchronized long getDuration() {
		try {
			if (initialized) {
				return Math.max(mPlayers[selectedPlayer].getDuration(), 0);
			}
		} catch (RuntimeException exception) {
			Log.e(TAG, "getDuration()", exception);
		}
		return 0;
	}

	/**
	 * Gets the current playback position.
	 *
	 * @return The current position in milliseconds
	 */
	public synchronized long getPosition() {
		try {
			if (initialized)
				return Math.max(mPlayers[selectedPlayer].getCurrentPosition(), 0);
		} catch (RuntimeException exception) {
			Log.e(TAG, "getPosition()", exception);
		}
		return 0;
	}

	/**
	 * Sets the current playback position.
	 *
	 * @param position The offset in milliseconds from the start to seek to
	 */
	public synchronized void setPosition(long position) {
		try {
			if (initialized && (!fadeEffectEnabled || fadeMode == FADE_IDLE)) {
				// limit max position to prevent conflict with fade out
				long max = getDuration();
				if (fadeEffectEnabled)
					max -= FADE_DELAY * 2;
				if (max > 0) {
					if (position > max) {
						position = max;
					} else if (position < 0) {
						position = 0;
					}
					mPlayers[selectedPlayer].seekTo((int) position);
				}
			}
		} catch (IllegalStateException exception) {
			Log.e(TAG, "setPosition() pos=" + position, exception);
		}
	}

	/**
	 * check if the current selected player is playing
	 *
	 * @return true if a playback is in progress
	 */
	public synchronized boolean isPlaying() {
		return isPlaying;
	}

	/**
	 * set volume limit of the player
	 *
	 * @param newVolume volume limit
	 */
	public synchronized void setMaxVolume(@FloatRange(from = 0f, to = 1f) float newVolume) {
		maxVolume = newVolume;
		if (fadeEffectEnabled) {
			setCurrentVolume(Math.min(volume, newVolume));
		} else {
			setAllVolume(newVolume);
		}
	}

	/**
	 * enable/disable fade effect
	 */
	public synchronized void setFadeEffect(boolean enable) {
		if (fadeEffectEnabled != enable) {
			fadeEffectEnabled = enable;
			fadeMode = FADE_IDLE;
			setFadeTask(enable);
			if (!fadeEffectEnabled) {
				setAllVolume(maxVolume);
			}
		}
	}

	/**
	 * @param player The {@link MediaPlayer} to use
	 * @param uri    The path of the file, or the http/rtsp URL of the stream you want to play
	 * @return true if initialized
	 */
	private boolean setDataSourceImpl(MediaPlayer player, Context context, @NonNull Uri uri) {
		try {
			player.reset();
			// check file if valid
			metadataLoader.setDataSource(context, uri);
			String hasAudio = metadataLoader.extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_AUDIO);
			if (hasAudio == null || !hasAudio.equals("yes")) {
				Log.w(TAG, "invalid media file!");
				return false;
			}
			// init player
			player.setDataSource(context, uri);
			player.prepare();
			return true;
		} catch (RuntimeException | IOException err) {
			Log.e(TAG, "setDataSourceImpl(): could not open media file!");
			return false;
		}
	}

	/**
	 * set volume of the current player
	 *
	 * @param newVolume new volume for the current player
	 */
	private synchronized void setCurrentVolume(@FloatRange(from = 0f, to = 1f) float newVolume) {
		try {
			volume = newVolume;
			// use cubic volume scale
			newVolume = newVolume * newVolume;
			mPlayers[selectedPlayer].setVolume(newVolume, newVolume);
		} catch (RuntimeException exception) {
			Log.e(TAG, "setVolume(): failed to set volume!");
		}
	}

	/**
	 * set volume of all players
	 *
	 * @param newVolume new volume applied to all players
	 */
	private void setAllVolume(@FloatRange(from = 0f, to = 1f) float newVolume) {
		for (MediaPlayer mp : mPlayers) {
			try {
				mp.setVolume(newVolume, newVolume);
			} catch (RuntimeException exception) {
				Log.e(TAG, "setAllVolume(): failed to set volume!");
			}
		}
	}

	/**
	 * called periodically while playback to detect playback changes for fade effect
	 */
	private void onAudioFadeTrack() {
		if (fadeTask != null) {
			switch (fadeMode) {
				// fade out current track, then fade in next track
				case FADE_OUT_IN:
					setCurrentVolume(Math.max(volume - FADE_STEPS, 0f));
					if (volume == 0f) {
						gotoNext();
						fadeMode = FADE_IN;
						callback.onWentToNext();
					}
					break;

				// fade out current track, then pause
				case FADE_OUT:
					setCurrentVolume(Math.max(volume - FADE_STEPS, 0f));
					if (volume == 0f) {
						pause(true);
					}
					break;

				// play and fade in current track
				case FADE_IN:
					setCurrentVolume(Math.min(volume + FADE_STEPS, maxVolume));
					if (volume == maxVolume) {
						fadeMode = FADE_IDLE;
					}
					break;

				// detect end of the track, then fade out, then fade in to new track if any
				default:
					long diff = getDuration() - getPosition();
					if (diff <= (FADE_DELAY + FADE_RESOLUTION)) {
						fadeMode = continuous ? FADE_OUT_IN : FADE_OUT;
					}
					break;
			}
		}
	}

	/**
	 * enable/disable periodic fade polling
	 *
	 * @param enable true to enable fading
	 */
	private synchronized void setFadeTask(boolean enable) {
		if (enable) {
			if (fadeTask == null) {
				fadeTask = threadPool.scheduleWithFixedDelay(() -> playerHandler.post(this::onAudioFadeTrack), FADE_RESOLUTION, FADE_RESOLUTION, TimeUnit.MILLISECONDS);
			}
		} else if (fadeTask != null) {
			fadeTask.cancel(false);
			fadeTask = null;
			fadeMode = FADE_IDLE;
		}
	}

	/**
	 * reset Multiplayer
	 */
	private synchronized void reset() {
		for (MediaPlayer mp : mPlayers) {
			mp.reset();
		}
		initialized = false;
		isPlaying = false;
		fadeMode = FADE_IDLE;
		selectedPlayer = 0;
		volume = 0f;
		maxVolume = 1f;
	}

	/**
	 * increase player index
	 */
	private void gotoNext() {
		selectedPlayer = (selectedPlayer + 1) % PLAYER_INST;
		volume = fadeEffectEnabled ? 0f : 1f;
		mPlayers[selectedPlayer].setVolume(volume, volume);
	}

	/**
	 * called when a mediaplayer finished playback
	 *
	 * @noinspection unused
	 * @see android.media.MediaPlayer.OnCompletionListener
	 */
	private void onCompletion(MediaPlayer mp) {
		if (continuous) {
			if (fadeEffectEnabled) {
				// Fix: sometimes the end position doesn't match the track duration
				// which causes that the "track end" detection will fail
				if (mp.getDuration() - mp.getCurrentPosition() > FADE_DELAY) {
					gotoNext();
					fadeMode = FADE_IN;
					callback.onWentToNext();
				}
			} else {
				gotoNext();
				callback.onWentToNext();
			}
		} else {
			pause(true);
			callback.onPlaybackChanged();
		}
	}

	/**
	 * called if the mediaplayer reports an error
	 *
	 * @noinspection unused
	 * @see android.media.MediaPlayer.OnErrorListener
	 */
	private boolean onError(MediaPlayer mp, int what, int extra) {
		Log.e(TAG, "onError(" + what + ", " + extra + "), " + this);
		if (initialized) {
			setFadeTask(false);
			reset();
			if (what == -38 || extra == -1004) {
				// caused by wrong state, stop playback
				callback.onPlaybackChanged();
			} else {
				// delay error handling
				playerHandler.postDelayed(() -> callback.onPlaybackError(), ERROR_RETRY);
			}
			return true;
		}
		return false;
	}


	@NonNull
	@Override
	public String toString() {
		return "current=" + selectedPlayer + " initialized=" + initialized + " isPlaying=" + isPlaying + " isFading=" + (fadeEffectEnabled && fadeMode != FADE_IDLE);
	}

	/**
	 * callback used for playback service
	 */
	public interface OnPlaybackStatusCallback {

		/**
		 * called if the playback status changed
		 */
		void onPlaybackChanged();

		/**
		 * called if the player has switched to another track
		 */
		void onWentToNext();

		/**
		 * called if a playback error occurs
		 */
		void onPlaybackError();
	}
}