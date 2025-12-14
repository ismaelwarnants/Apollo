package org.nuclearfog.apollo.player;

import android.content.Context;
import android.media.AudioManager;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Handler;
import android.util.Log;

import androidx.annotation.FloatRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * custom MediaPlayer implementation containing two MediaPlayer to switch fast tracks
 *
 * @author nuclearfog
 */
public class MultiPlayer {

	private static final String TAG = "MultiPlayer";
	/**
	 * indicates that there is no fade in/out in progress
	 */
	private static final int NONE = 9;
	/**
	 * indicates that the current track is fading in
	 */
	private static final int FADE_IN = 10;
	/**
	 * indicates that the current track is fading out
	 */
	private static final int FADE_OUT = 11;
	/**
	 * indicates that there is a crossfading in progress
	 */
	private static final int XFADE = 12;
	/**
	 * volume steps used to fade in or out
	 */
	private static final float FADE_STEPS = 0.08f;
	/**
	 * duration of fade effect in ms
	 */
	private static final long FADE_DELAY = 1000;
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

	@Nullable
	private Future<?> xfadeTask;
	private MediaMetadataRetriever mmr;
	private OnPlaybackStatusCallback callback;
	private Handler playerHandler, xfadeHandler;

	/**
	 * thread pool used to periodically poll the current play position for crossfading
	 */
	private ScheduledExecutorService threadPool = Executors.newSingleThreadScheduledExecutor();

	/**
	 * mediaplayer used to switch between tracks
	 */
	private MediaPlayer[] mPlayers = new MediaPlayer[PLAYER_INST];
	/**
	 * current mediaplayer's index of {@link #mPlayers}
	 */
	private int currentPlayer = 0;
	/**
	 * true if mediaplayer is currently playing
	 */
	private volatile boolean isPlaying = false;
	/**
	 * set to true if player was initialized successfully
	 */
	private volatile boolean initialized = false;
	/**
	 * true if player continues to next track automatically
	 */
	private volatile boolean continuous = true;
	/**
	 * current fade in/out status {@link #NONE,#FADE_IN,#FADE_OUT}
	 */
	private volatile int xfadeMode = NONE;
	/**
	 * enable/disable fade in/out effect
	 */
	private boolean crossfade;
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
	 * @param context   context from service
	 * @param callback  a callback used to inform about playback changes
	 * @param crossfade true to enable crossfade
	 */
	public MultiPlayer(Context context, OnPlaybackStatusCallback callback, boolean crossfade) {
		playerHandler = new Handler(context.getMainLooper());
		xfadeHandler = new Handler(context.getMainLooper());
		mmr = new MediaMetadataRetriever();
		this.crossfade = crossfade;
		this.callback = callback;
		for (int i = 0; i < mPlayers.length; i++) {
			mPlayers[i] = new MediaPlayer();
			mPlayers[i].setAudioStreamType(AudioManager.STREAM_MUSIC);
			mPlayers[i].setAudioSessionId(mPlayers[0].getAudioSessionId());
			mPlayers[i].setOnCompletionListener(this::onCompletion);
			mPlayers[i].setOnErrorListener(this::onError);
		}
	}

	/**
	 * @param uri The path of the file, or the http/rtsp URL of the stream you want to play
	 * @return true if player is ready to play
	 */
	public boolean setDataSource(Context context, @NonNull Uri uri) {
		// stop current playback
		MediaPlayer player = mPlayers[currentPlayer];
		if (initialized)
			player.stop();
		// set source of the current selected player
		initialized = setDataSourceImpl(player, context, uri);
		return initialized;
	}

	/**
	 * Set the MediaPlayer to start when this MediaPlayer finishes playback.
	 *
	 * @param uri The path of the file, or the http/rtsp URL of the stream you want to play
	 * @return true if next data source is initialized successfully
	 */
	public boolean setNextDataSource(Context context, @Nullable Uri uri) {
		MediaPlayer current = mPlayers[currentPlayer];
		MediaPlayer next = mPlayers[(currentPlayer + 1) % mPlayers.length];
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
	public boolean initialized() {
		return initialized;
	}

	/**
	 * Starts or resumes playback.
	 */
	public synchronized void play() {
		try {
			MediaPlayer player = mPlayers[currentPlayer];
			if (!player.isPlaying()) {
				isPlaying = true;
				if (!crossfade) {
					setCrossfadeTask(false);
					setVolume(1f);
				} else {
					xfadeMode = FADE_IN;
					setVolume(0f);
					setCrossfadeTask(true);
				}
				player.start();
				callback.onPlaybackChanged();
			}
		} catch (IllegalStateException exception) {
			Log.e(TAG, "failed to start player");
			stop();
		}
	}

	/**
	 * Pauses playback. Call start() to resume.
	 *
	 * @param force true to stop playback immediately
	 */
	public synchronized void pause(boolean force) {
		MediaPlayer player = mPlayers[currentPlayer];
		try {
			if (force || !crossfade) {
				setCrossfadeTask(false);
				player.pause();
				isPlaying = false;
				callback.onPlaybackChanged();
			} else {
				xfadeMode = FADE_OUT;
			}
		} catch (IllegalStateException exception) {
			Log.e(TAG, "failed to pause player");
			stop();
		}
	}

	/**
	 * stops playback
	 */
	public synchronized void stop() {
		MediaPlayer player = mPlayers[currentPlayer];
		try {
			setCrossfadeTask(false);
			player.stop();
			player.prepare();
			player.seekTo(0);
			isPlaying = false;
			callback.onPlaybackChanged();
		} catch (IllegalStateException | IOException exception) {
			Log.e(TAG, "stop():", exception);
			player.reset();
			initialized = false;
		}
	}

	/**
	 * Releases media player
	 */
	public synchronized void release() {
		threadPool.shutdown();
		for (MediaPlayer player : mPlayers) {
			try {
				player.release();
			} catch (IllegalStateException exception) {
				Log.e(TAG, "failed to release player", exception);
			}
		}
	}

	/**
	 * Gets the duration of the file.
	 *
	 * @return The duration in milliseconds
	 */
	public synchronized long getDuration() {
		try {
			if (initialized)
				return mPlayers[currentPlayer].getDuration();
		} catch (IllegalStateException exception) {
			Log.e(TAG, "invalid player duration");
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
				return mPlayers[currentPlayer].getCurrentPosition();
		} catch (IllegalStateException exception) {
			Log.e(TAG, "getPosition(): invalid player position!");
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
			if (initialized && (!crossfade || xfadeMode == NONE)) {
				// limit max position to prevent conflict with fade out
				long max = getDuration() - (FADE_DELAY * 2);
				if (max > 0) {
					if (position > max) {
						position = max;
					} else if (position < 0) {
						position = 0;
					}
					mPlayers[currentPlayer].seekTo((int) position);
				}
			}
		} catch (IllegalStateException exception) {
			Log.e(TAG, "setPosition() failed! pos=" + position + " duration=" + getDuration());
		}
	}

	/**
	 * Returns the audio session ID.
	 *
	 * @return The current audio session ID.
	 */
	public synchronized int getAudioSessionId() {
		return mPlayers[currentPlayer].getAudioSessionId();
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
		setVolume(Math.min(volume, newVolume));
	}

	/**
	 * enable/disable crossfade
	 */
	public synchronized void setCrossfade(boolean enable) {
		if (crossfade != enable) {
			crossfade = enable;
			xfadeMode = NONE;
			setCrossfadeTask(enable);
			if (!crossfade) {
				setVolume(1f);
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
			// check file if valid
			mmr.setDataSource(context, uri);
			String hasAudio = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_AUDIO);
			if (hasAudio == null || !hasAudio.equals("yes")) {
				Log.w(TAG, "invalid media file!");
				return false;
			}
			// init player
			player.reset();
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
	private void setVolume(@FloatRange(from = 0f, to = 1f) float newVolume) {
		try {
			mPlayers[currentPlayer].setVolume(newVolume, newVolume);
			volume = newVolume;
		} catch (RuntimeException exception) {
			Log.e(TAG, "setVolume(): failed to set volume!");
		}
	}

	/**
	 * called periodically while playback to detect playback changes for crossfading
	 */
	private void onCrossfadeTrack() {
		if (xfadeTask != null) {
			switch (xfadeMode) {
				// force crossfade between two tracks
				case XFADE:
					setVolume(Math.max(volume - FADE_STEPS, 0f));
					if (volume == 0f) {
						xfadeMode = FADE_IN;
					}
					break;

				// fade out current track, then pause
				case FADE_OUT:
					setVolume(Math.max(volume - FADE_STEPS, 0f));
					if (volume == 0f) {
						xfadeMode = NONE;
						pause(true);
					}
					break;

				// play and fade in current track
				case FADE_IN:
					setVolume(Math.min(volume + FADE_STEPS, maxVolume));
					if (volume == maxVolume) {
						xfadeMode = NONE;
					}
					break;

				// detect end of the track then cross fade to new track if any
				default:
					long diff = Math.abs(getDuration() - getPosition());
					if (diff <= FADE_DELAY) {
						xfadeMode = XFADE;
					}
					break;
			}
		}
	}

	/**
	 * enable/disable periodic crossfade polling
	 *
	 * @param enable true to enable crossfading
	 */
	private void setCrossfadeTask(boolean enable) {
		// set new cross fade task
		if (enable) {
			if (xfadeTask == null) {
				xfadeTask = threadPool.scheduleWithFixedDelay(() -> xfadeHandler.post(this::onCrossfadeTrack), FADE_RESOLUTION, FADE_RESOLUTION, TimeUnit.MILLISECONDS);
			}
		} else if (xfadeTask != null) {
			xfadeTask.cancel(false);
			xfadeTask = null;
			xfadeMode = NONE;
			setVolume(maxVolume);
		}
	}

	/**
	 * increase player index
	 */
	private void setNextPlayer() {
		currentPlayer = (currentPlayer + 1) % PLAYER_INST;
	}

	/**
	 * called when a mediaplayer finished playback
	 *
	 * @noinspection unused
	 * @see android.media.MediaPlayer.OnCompletionListener
	 */
	private void onCompletion(MediaPlayer mp) {
		if (continuous) {
			setNextPlayer();
			callback.onWentToNext();
		} else {
			pause(true);
			callback.onPlaybackChanged();
		}
	}

	/**
	 * called if the mediaplayer reports an error
	 *
	 * @see android.media.MediaPlayer.OnErrorListener
	 */
	private boolean onError(MediaPlayer mp, int what, int extra) {
		Log.e(TAG, "onError(): (" + what + ", " + extra + ")");
		if (initialized) {
			setCrossfadeTask(false);
			initialized = false;
			isPlaying = false;
			xfadeMode = NONE;
			mp.reset();
			// delay callback
			playerHandler.postDelayed(() -> callback.onPlaybackError(), ERROR_RETRY);
			return true;
		}
		return false;
	}


	@NonNull
	@Override
	public String toString() {
		return "current=" + currentPlayer + " initialized=" + initialized + " isPlaying=" + isPlaying + " fade=" + (xfadeMode != NONE);
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