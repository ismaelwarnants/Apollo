package org.nuclearfog.apollo.service;

import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.media.AudioManager;
import android.media.AudioManager.OnAudioFocusChangeListener;
import android.media.audiofx.AudioEffect;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import android.provider.MediaStore.Audio.AlbumColumns;
import android.provider.MediaStore.Audio.Media;
import android.provider.MediaStore.Files.FileColumns;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ServiceCompat;
import androidx.core.content.ContextCompat;
import androidx.media.AudioAttributesCompat;
import androidx.media.AudioFocusRequestCompat;
import androidx.media.AudioManagerCompat;

import org.nuclearfog.apollo.BuildConfig;
import org.nuclearfog.apollo.R;
import org.nuclearfog.apollo.cache.ImageFetcher;
import org.nuclearfog.apollo.model.Album;
import org.nuclearfog.apollo.model.Song;
import org.nuclearfog.apollo.player.MultiPlayer;
import org.nuclearfog.apollo.player.MultiPlayer.OnPlaybackStatusCallback;
import org.nuclearfog.apollo.receiver.ServiceBroadcastReceiver;
import org.nuclearfog.apollo.receiver.ServiceBroadcastReceiver.OnStatusChangedListener;
import org.nuclearfog.apollo.service.lists.PlaybackList;
import org.nuclearfog.apollo.service.lists.ShuffleList;
import org.nuclearfog.apollo.store.PlaylistStore;
import org.nuclearfog.apollo.store.PopularStore;
import org.nuclearfog.apollo.store.RecentStore;
import org.nuclearfog.apollo.store.preferences.AppPreferences;
import org.nuclearfog.apollo.store.preferences.PlayerPreferences;
import org.nuclearfog.apollo.ui.widgets.AppWidgetLarge;
import org.nuclearfog.apollo.ui.widgets.AppWidgetLargeAlt;
import org.nuclearfog.apollo.ui.widgets.AppWidgetRecent;
import org.nuclearfog.apollo.ui.widgets.AppWidgetSmall;
import org.nuclearfog.apollo.utils.ApolloUtils;
import org.nuclearfog.apollo.utils.AudioEffects;
import org.nuclearfog.apollo.utils.CursorFactory;

import java.io.Serializable;

/**
 * A background {@link Service} used to keep music playing between activities
 * and when the user moves Apollo into the background.
 *
 * @author nuclearfog
 */
public class MusicPlaybackService extends Service implements OnAudioFocusChangeListener, OnStatusChangedListener, OnPlaybackStatusCallback {
	/**
	 *
	 */
	private static final String TAG = "MusicPlaybackService";
	/**
	 * app package name
	 */
	private static final String APOLLO_PACKAGE_NAME = BuildConfig.APPLICATION_ID;
	/**
	 * used to determine if app is in foreground
	 */
	public static final String EXTRA_FOREGROUND = "nowinforeground";
	/**
	 * Indicates that the music has paused or resumed
	 */
	public static final String CHANGED_PLAYSTATE = APOLLO_PACKAGE_NAME + ".playstatechanged";
	/**
	 * Indicates the meta data has changed in some way, like a track change
	 */
	public static final String CHANGED_META = APOLLO_PACKAGE_NAME + ".metachanged";
	/**
	 * Indicates the playback queue has been updated
	 */
	public static final String CHANGED_QUEUE = APOLLO_PACKAGE_NAME + ".queuechanged";
	/**
	 * Indicates that the seek position of the current track has changed
	 */
	public static final String CHANGED_SEEK = APOLLO_PACKAGE_NAME + ".seekposchanged";
	/**
	 * Indicates that a widget was installed and needs to be updated
	 */
	public static final String CHANGED_WIDGET = APOLLO_PACKAGE_NAME + ".widgetchanged";
	/**
	 * Indicates the repeat mode changed
	 */
	public static final String CHANGED_REPEATMODE = APOLLO_PACKAGE_NAME + ".repeatmodechanged";
	/**
	 * Indicates the shuffle mode changed
	 */
	public static final String CHANGED_SHUFFLEMODE = APOLLO_PACKAGE_NAME + ".shufflemodechanged";
	/**
	 * Called to go toggle between pausing and playing the music
	 */
	public static final String ACTION_TOGGLEPAUSE = APOLLO_PACKAGE_NAME + ".togglepause";
	/**
	 * Called to go to stop the playback
	 */
	public static final String ACTION_STOP = APOLLO_PACKAGE_NAME + ".stop";
	/**
	 * Called to go to the previous track
	 */
	public static final String ACTION_PREVIOUS = APOLLO_PACKAGE_NAME + ".previous";
	/**
	 * Called to go to the next track
	 */
	public static final String ACTION_NEXT = APOLLO_PACKAGE_NAME + ".next";
	/**
	 * Called to change the repeat mode
	 */
	public static final String ACTION_REPEAT = APOLLO_PACKAGE_NAME + ".repeat";
	/**
	 * Called to change the shuffle mode
	 */
	public static final String ACTION_SHUFFLE = APOLLO_PACKAGE_NAME + ".shuffle";
	/**
	 * IntentFilter action used to trigger {@link MusicPlaybackService} to update the widgets
	 */
	public static final String ACTION_WIDGET_UPDATE = BuildConfig.APPLICATION_ID + ".update_widgets";
	/**
	 * bundle key of the play status (play/pause)
	 */
	public static final String KEY_IS_PLAYING = "isPlaying";
	/**
	 * bundle key used for the selected song of the playback list
	 */
	public static final String KEY_SONG = "song";
	/**
	 * bundle key used for the selected song album of the playback list
	 */
	public static final String KEY_ALBUM = "album";
	/**
	 * bundle key used for the current shuffle mode
	 */
	public static final String KEY_SHUFFLE = "shuffle";
	/**
	 * bundle key used for the current repeat mode
	 */
	public static final String KEY_REPEAT = "repeat";
	/**
	 * Moves a list to the next position in the queue
	 */
	public static final int MOVE_NEXT = 0xAE960453;
	/**
	 * Moves a list to the last position in the queue
	 */
	public static final int MOVE_LAST = 0xB03ED8F4;
	/**
	 * Shuffles no songs, turns shuffling off
	 */
	public static final int SHUFFLE_NONE = 0xD47F8582;
	/**
	 * Shuffles all tracks of the playback list
	 */
	public static final int SHUFFLE_NORMAL = 0xC5F90214;
	/**
	 * shuffle all available tracks
	 */
	public static final int SHUFFLE_AUTO = 0x45EBC386;
	/**
	 * Turns repeat off
	 */
	public static final int REPEAT_NONE = 0x28AEE9F7;
	/**
	 * Repeats the current track in a list
	 */
	public static final int REPEAT_CURRENT = 0x4478C4B2;
	/**
	 * Repeats all the tracks in a list
	 */
	public static final int REPEAT_ALL = 0xEE3F9E0B;
	/**
	 * widget classes used to update all widgets
	 */
	private static final Class<?>[] WIDGETS = {AppWidgetLarge.class, AppWidgetLargeAlt.class, AppWidgetRecent.class, AppWidgetSmall.class};
	/**
	 * Song play time used as threshold for rewinding to the beginning of the
	 * track instead of skipping to the previous track when getting the PREVIOUS
	 * command
	 */
	private static final long REWIND_INSTEAD_PREVIOUS_THRESHOLD = 3000L;
	/**
	 * amount of faulty tracks to be skipped before giving up
	 */
	private static final int RETRY_COUNT = 10;
	/**
	 * current playlist containing track ID's
	 */
	private PlaybackList mPlayList = new PlaybackList();
	/**
	 * shuffle list containing track indexes of the current playlist
	 */
	private ShuffleList mShuffleList = new ShuffleList();
	/**
	 * audio manager to gain audio focus
	 */
	private AudioManager mAudio;
	/**
	 * used to request/abandon audio focus
	 */
	private AudioFocusRequestCompat focusRequest;
	/**
	 * broadcast listener to detect status changes (e.g. widget update, headphone disconnect)
	 */
	private ServiceBroadcastReceiver serviceBroadcastReceiver;
	/**
	 * handler used to shutdown service after idle
	 */
	private ShutdownHandler shutdownHandler;
	/**
	 * The media player
	 */
	private MultiPlayer mPlayer;
	/**
	 * MediaSession to init media button support
	 */
	private MediaSessionCompat mSession;
	/**
	 * Used to build the notification
	 */
	private NotificationHelper mNotificationHelper;
	/**
	 * player settings
	 */
	private PlayerPreferences playerSettings;
	/**
	 * database for recently played tracks
	 */
	private RecentStore recentStore;
	/**
	 * database for the most played tracks
	 */
	private PopularStore popularStore;
	/**
	 * database for current playback list
	 */
	private PlaylistStore playlistStore;
	/**
	 * app widget manager used to update all installed widget on track/playback changes
	 */
	private AppWidgetManager widgetManager;
	/**
	 * current audio session ID
	 */
	private int audioSessionId;
	/**
	 * Used to know when the service is active
	 */
	private boolean mServiceInUse = false;
	/**
	 * used to check if service is running in the foreground
	 */
	private boolean isForeground = false;
	/**
	 *
	 */
	private ImageFetcher imageFetcher;
	/**
	 * current song to play
	 */
	@Nullable
	private Song currentSong;
	/**
	 * current album of the song to play
	 */
	@Nullable
	private Album currentAlbum;
	/**
	 * current shuffle mode {@link #SHUFFLE_NONE,#SHUFFLE_NORMAL,#SHUFFLE_AUTO}
	 */
	private int mShuffleMode = SHUFFLE_NONE;
	/**
	 * current repeat mode {@link #REPEAT_NONE,#REPEAT_CURRENT,#REPEAT_ALL}
	 */
	private int mRepeatMode = REPEAT_ALL;
	/**
	 * ID of the current running service (used to stop service)
	 */
	private int mServiceStartId = -1;

	/**
	 * {@inheritDoc}
	 */
	@Override
	public IBinder onBind(Intent intent) {
		mServiceInUse = true;
		shutdownHandler.stop();
		return new ServiceStub(this);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean onUnbind(Intent intent) {
		mServiceInUse = false;
		return false;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onCreate() {
		super.onCreate();
		// Initialize the database instances
		recentStore = RecentStore.getInstance(getApplicationContext());
		popularStore = PopularStore.getInstance(getApplicationContext());
		playlistStore = PlaylistStore.getInstance(getApplicationContext());
		// initialize broadcast receiver
		serviceBroadcastReceiver = new ServiceBroadcastReceiver(this);
		imageFetcher = new ImageFetcher(this);
		// Initialize the preferences
		playerSettings = PlayerPreferences.getInstance(this);
		AppPreferences appSettings = AppPreferences.getInstance(this);
		widgetManager = AppWidgetManager.getInstance(getApplicationContext());
		// initialize audio manager and audio session ID
		mAudio = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
		audioSessionId = mAudio.generateAudioSessionId();
		// Initialize the media player
		mPlayer = new MultiPlayer(getApplicationContext(), audioSessionId, this, appSettings.crossfadeEnabled());
		// init media session
		mSession = new MediaSessionCompat(getApplicationContext(), TAG);
		mSession.setCallback(new MediaButtonCallback(this), null);
		mSession.setActive(true);
		// Initialize the notification helper
		mNotificationHelper = new NotificationHelper(this, mSession);
		// init shutdown handler
		shutdownHandler = new ShutdownHandler(this);
		AudioAttributesCompat mAttributes = new AudioAttributesCompat.Builder()
				.setUsage(AudioAttributesCompat.USAGE_MEDIA)
				// prevent system fade in/out effects to be applied by setting content type to "speech"
				.setContentType(AudioAttributesCompat.CONTENT_TYPE_SPEECH).build();
		focusRequest = new AudioFocusRequestCompat.Builder(AudioManagerCompat.AUDIOFOCUS_GAIN)
				.setAudioAttributes(mAttributes).setOnAudioFocusChangeListener(this).build();
		// register receiver
		IntentFilter intentFilter = serviceBroadcastReceiver.createIntentFiler();
		ContextCompat.registerReceiver(this, serviceBroadcastReceiver, intentFilter, ContextCompat.RECEIVER_EXPORTED);
		// initialize audio effects
		if (appSettings.isExternalAudioFxPreferred()) {
			// send session ID to external equalizer if set
			ApolloUtils.notifyExternalEqualizer(this, getAudioSessionId());
		} else {
			AudioEffects.getInstance(getApplicationContext(), audioSessionId);
		}
		// initialize the playback/history list and state
		initPlaybackList();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onDestroy() {
		// Remove any sound effects
		Intent audioEffectsIntent = new Intent(AudioEffect.ACTION_CLOSE_AUDIO_EFFECT_CONTROL_SESSION);
		audioEffectsIntent.putExtra(AudioEffect.EXTRA_AUDIO_SESSION, getAudioSessionId());
		audioEffectsIntent.putExtra(AudioEffect.EXTRA_PACKAGE_NAME, APOLLO_PACKAGE_NAME);
		sendBroadcast(audioEffectsIntent);
		AudioEffects.release();
		//save playlist, history and shuffle/repeat state
		savePlaybackList(true);
		// Release the player
		mPlayer.release();
		// release player callbacks
		mSession.release();
		// Unregister the broadcast receiver
		unregisterReceiver(serviceBroadcastReceiver);
		// remove notification
		mNotificationHelper.dismissNotification();
		super.onDestroy();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onAudioFocusChange(int focusChange) {
		switch (focusChange) {
			case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
			case AudioManager.AUDIOFOCUS_LOSS:
				pause(true);
				break;

			case AudioManager.AUDIOFOCUS_GAIN:
				if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O)
					mPlayer.setMaxVolume(1f);
				break;

			case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
				if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O)
					mPlayer.setMaxVolume(.5f);
				break;
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		mServiceStartId = startId;
		if (intent != null) {
			boolean updateService = handleCommandIntent(intent);
			isForeground = intent.getBooleanExtra(EXTRA_FOREGROUND, false);
			// start foreground service
			if (isForeground) {
				mNotificationHelper.startForeground();
			}
			// update service status
			if (updateService) {
				mNotificationHelper.updateNotification();
				if (!isForeground || isPlaying()) {
					return START_STICKY;
				}
			} else {
				return START_STICKY;
			}
		}
		// Make sure the service will shut down on its own if it was
		// just started but not bound to and nothing is playing
		shutdownHandler.start();
		return START_NOT_STICKY;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onPlaybackChanged() {
		notifyChange(CHANGED_PLAYSTATE);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onWentToNext() {
		mPlayList.gotoNextPosition();
		updateTrackInformation(mPlayList.getSelected());
		setNextTrack(false);
		notifyChange(CHANGED_SEEK);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onPlaybackError() {
		Toast.makeText(getApplicationContext(), R.string.error_playback, Toast.LENGTH_LONG).show();
		openCurrentAndNext();
		notifyChange(CHANGED_PLAYSTATE);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onExternalStorageChanged(boolean mounted) {
		stop();
		if (mounted) {
			initPlaybackList();
		} else {
			savePlaybackList(true);
		}
		notifyChange(CHANGED_QUEUE);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onHeadphoneDisconnected() {
		pause(true);
	}

	/**
	 * @return True if music is playing, false otherwise
	 */
	public boolean isPlaying() {
		return mPlayer.isPlaying();
	}

	/**
	 * Returns the shuffle mode
	 *
	 * @return The current shuffle mode ({@link #SHUFFLE_AUTO,#SHUFFLE_NORMAL,#SHUFFLE_NONE}
	 */
	public int getShuffleMode() {
		return mShuffleMode;
	}

	/**
	 * Returns the repeat mode
	 *
	 * @return The current repeat mode {@link #REPEAT_ALL,#REPEAT_CURRENT,#REPEAT_NONE}
	 */
	public int getRepeatMode() {
		return mRepeatMode;
	}

	/**
	 * Returns the album name
	 *
	 * @return The current song album Name
	 */
	@Nullable
	public Album getCurrentAlbum() {
		return currentAlbum;
	}

	/**
	 * Returns the song name
	 *
	 * @return The current song name
	 */
	@Nullable
	public Song getCurrentSong() {
		return currentSong;
	}

	/**
	 * Stops playback.
	 */
	void stop() {
		mPlayer.stop();
		notifyChange(CHANGED_PLAYSTATE);
		AudioManagerCompat.abandonAudioFocusRequest(mAudio, focusRequest);
	}

	/**
	 * Resumes or starts playback.
	 */
	void play() {
		int returnCode = AudioManagerCompat.requestAudioFocus(mAudio, focusRequest);
		if (returnCode == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
			if (mPlayer.initialized() && !mPlayList.isEmpty()) {
				// jump to next track directly if reached the end
				long duration = mPlayer.getDuration();
				if (mRepeatMode != REPEAT_CURRENT && duration > 2000L && mPlayer.getPosition() >= duration - 2000L) {
					gotoNext();
				} else {
					mPlayer.play();
					notifyChange(CHANGED_PLAYSTATE);
				}
			} else {
				setShuffleMode(SHUFFLE_AUTO);
				mPlayer.play();
				notifyChange(CHANGED_PLAYSTATE);
			}
		} else {
			Log.i(TAG, "play(): could not request audio focus!");
		}
	}

	/**
	 * Temporarily pauses playback.
	 *
	 * @param force true to stop playback immediately (fade-out disabled)
	 */
	public void pause(boolean force) {
		mPlayer.pause(force);
		notifyChange(CHANGED_PLAYSTATE);
	}

	/**
	 * Changes from the current track to the next track
	 */
	void gotoNext() {
		if (!mPlayList.isEmpty()) {
			mPlayList.setPosition(incrementPosition(mPlayList.getPosition(), true));
			openCurrentAndNext();
		}
		play();
	}

	/**
	 * restart current track or go to preview track
	 */
	void gotoPrev() {
		long pos = mPlayer.getPosition();
		mPlayer.stop();
		// go to previous track if playback position is at beginning
		if (pos < REWIND_INSTEAD_PREVIOUS_THRESHOLD) {
			mPlayList.setPosition(decrementPosition(mPlayList.getPosition()));
			openCurrentAndNext();
		}
		play();
	}

	/**
	 * Seeks the current track to a specific time
	 *
	 * @param position The time to seek to
	 */
	void seekTo(long position) {
		mPlayer.setPosition(position);
		notifyChange(CHANGED_SEEK);
	}

	/**
	 * open file uri
	 *
	 * @param uri URI of the local file
	 */
	void openFile(@NonNull Uri uri) {
		stop();
		// open new track
		updateTrackInformation(uri);
		Song song = currentSong;
		// check if track is valid
		if (song != null && mPlayer.setDataSource(getApplicationContext(), uri)) {
			// add at the beginning of the playlist
			mPlayList.addFirst(song.getId());
			mPlayList.setPosition(0);
			// update metadata
			notifyChange(CHANGED_QUEUE);
			// setup player & start
			setNextTrack(false);
			play();
		} else {
			// restore track information after error
			openCurrentAndNext();
		}
	}

	/**
	 * Returns the audio session ID
	 *
	 * @return The current audio session ID
	 */
	int getAudioSessionId() {
		return audioSessionId;
	}

	/**
	 * select & play a track in the current playback list
	 *
	 * @param index The playback position of the track
	 */
	void setQueuePosition(int index) {
		mPlayList.setPosition(index);
		openCurrentAndNext();
		play();
	}

	/**
	 * Returns the current position in the playback list
	 *
	 * @return The playback position of the track
	 */
	int getQueuePosition() {
		return mPlayList.getPosition();
	}

	/**
	 * Returns the queue
	 *
	 * @return The queue containing song IDs
	 */
	long[] getQueue() {
		return mPlayList.getItems();
	}

	/**
	 * Returns the current position in time of the current track
	 *
	 * @return The current playback position in milliseconds
	 */
	long getPosition() {
		return mPlayer.getPosition();
	}

	/**
	 * Returns the duration of the current track
	 *
	 * @return duration in milliseconds
	 */
	long getDuration() {
		return mPlayer.getDuration();
	}

	/**
	 * Sets the shuffle mode
	 *
	 * @param shuffleMode The shuffle mode to use
	 */
	void setShuffleMode(int shuffleMode) {
		if (mShuffleMode != shuffleMode || mPlayList.isEmpty()) {
			// shuffle all available songs
			if (shuffleMode == SHUFFLE_AUTO) {
				if (makeShuffleList(true)) {
					setRepeatMode(REPEAT_ALL);
					mSession.setShuffleMode(PlaybackStateCompat.SHUFFLE_MODE_ALL);
					// start playback at random position
					mPlayList.setPosition(mShuffleList.next());
					openCurrentAndNext();
					play();
				}
			}
			// shuffle current playlist
			else if (shuffleMode == SHUFFLE_NORMAL) {
				if (makeShuffleList(false)) {
					setRepeatMode(REPEAT_ALL);
					mSession.setShuffleMode(PlaybackStateCompat.SHUFFLE_MODE_ALL);
					setNextTrack(false);
				}
			}
			// disable shuffle
			else if (shuffleMode == SHUFFLE_NONE) {
				mSession.setShuffleMode(PlaybackStateCompat.SHUFFLE_MODE_NONE);
				mShuffleMode = SHUFFLE_NONE;
				mShuffleList.clear();
				setNextTrack(false);
			}
			notifyChange(CHANGED_SHUFFLEMODE);
			savePlaybackList(false);
		}
	}

	/**
	 * Sets the repeat mode
	 *
	 * @param repeatMode The repeat mode to use
	 */
	void setRepeatMode(int repeatMode) {
		if (mRepeatMode != repeatMode) {
			if (repeatMode == REPEAT_ALL) {
				mRepeatMode = REPEAT_ALL;
				mSession.setRepeatMode(PlaybackStateCompat.REPEAT_MODE_ALL);
			} else if (repeatMode == REPEAT_CURRENT) {
				mRepeatMode = REPEAT_CURRENT;
				mSession.setRepeatMode(PlaybackStateCompat.REPEAT_MODE_ONE);
				setShuffleMode(SHUFFLE_NONE);
			} else if (repeatMode == REPEAT_NONE) {
				mRepeatMode = REPEAT_NONE;
				mSession.setRepeatMode(PlaybackStateCompat.REPEAT_MODE_NONE);
			}
			setNextTrack(false);
			savePlaybackList(false);
			notifyChange(CHANGED_REPEATMODE);
		}
	}

	/**
	 * Opens a list for playback
	 *
	 * @param list     The list of tracks to open
	 * @param position The position to start playback at
	 */
	void open(long[] list, int position) {
		stop();
		if (mShuffleMode == SHUFFLE_AUTO) {
			mShuffleMode = SHUFFLE_NORMAL;
		}
		mPlayList.setItems(list);
		mPlayList.setPosition(position);
		mShuffleList.clearHistory();
		notifyChange(CHANGED_QUEUE);
		if (list.length > 0) {
			openCurrentAndNext();
			play();
		} else {
			clearCurrentTrackInformation();
		}
	}

	/**
	 * Queues a new list for playback
	 *
	 * @param list   The list to queue
	 * @param action The action to take {@link #MOVE_NEXT,#MOVE_LAST}
	 */
	void enqueue(long[] list, int action) {
		if (action == MOVE_NEXT) {
			mPlayList.addItemsToNext(list);
			setShuffleMode(SHUFFLE_NONE);
		} else if (action == MOVE_LAST) {
			mPlayList.addItems(list);
		}
		if (!mPlayList.isEmpty() && list.length > 0) {
			if (mPlayList.getPosition() < 0) {
				mPlayList.setPosition(0);
				openCurrentAndNext();
			} else {
				setNextTrack(false);
			}
		}
		notifyChange(CHANGED_QUEUE);
	}

	/**
	 * Moves an item in the queue from one position to another
	 *
	 * @param from The position the item is currently at
	 * @param to   The position the item is being moved to
	 */
	void moveQueueItem(int from, int to) {
		mPlayList.move(from, to);
		notifyChange(CHANGED_QUEUE);
	}

	/**
	 * remove single track from queue at specific position
	 *
	 * @param pos position of the track in the queue
	 */
	void removeQueueTrack(int pos) {
		if (pos >= 0 && pos < mPlayList.size()) {
			// remove selected item
			mPlayList.remove(pos);
			// stop playback if there is no track selected
			if (mPlayList.getPosition() < 0) {
				stop();
				// select next track if any
				if (!mPlayList.isEmpty()) {
					mPlayList.setPosition(Math.min(pos, mPlayList.size() - 1));
					openCurrentAndNext();
				} else {
					clearCurrentTrackInformation();
				}
			}
			// notify that queue changed
			notifyChange(CHANGED_QUEUE);
		}
	}

	/**
	 * clear the current queue and stop playback
	 */
	void clearQueue() {
		stop();
		mPlayList.clear();
		mShuffleList.clear();
		clearCurrentTrackInformation();
		notifyChange(CHANGED_QUEUE);
	}

	/**
	 *
	 */
	void stopForeground() {
		ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE);
		shutdownHandler.stop();
		isForeground = false;
	}

	/**
	 * releases playback service and removes notification/playback controls
	 *
	 * @param force true to force shutdown this service
	 */
	void releaseService(boolean force) {
		if (!isPlaying() || force) {
			AudioManagerCompat.abandonAudioFocusRequest(mAudio, focusRequest);
			ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE);
			if (!mServiceInUse || force) {
				if (stopSelfResult(mServiceStartId)) {
					Log.d(TAG, "service stopped");
				}
			}
		}
	}

	/**
	 * enable/disable player fade effects
	 */
	public void setCrossfade(boolean enable) {
		mPlayer.setFadeEffect(enable);
	}

	/**
	 * update current track metadata of the media session (used to update player control notification)
	 */
	private void updateMetadata() {
		MediaMetadataCompat.Builder builder = new MediaMetadataCompat.Builder();
		Song song = currentSong;
		Album album = currentAlbum;
		if (song != null) {
			builder.putString(MediaMetadataCompat.METADATA_KEY_TITLE, song.getName());
			builder.putString(MediaMetadataCompat.METADATA_KEY_ARTIST, song.getArtist());
			builder.putString(MediaMetadataCompat.METADATA_KEY_ALBUM, song.getAlbum());
			builder.putLong(MediaMetadataCompat.METADATA_KEY_DURATION, song.getDuration());
		}
		if (album != null) {
			builder.putString(MediaMetadataCompat.METADATA_KEY_DATE, album.getRelease());
			builder.putLong(MediaMetadataCompat.METADATA_KEY_NUM_TRACKS, album.getTrackCount());
			builder.putBitmap(MediaMetadataCompat.METADATA_KEY_ART, imageFetcher.getAlbumArtwork(album));
		}
		mSession.setMetadata(builder.build());
		mNotificationHelper.updateNotification();
	}

	/**
	 * update playback state of the media session (used to update player control notification)
	 */
	private void updatePlaybackState() {
		PlaybackStateCompat.Builder builder = new PlaybackStateCompat.Builder();
		builder.setState(mPlayer.isPlaying() ? PlaybackStateCompat.STATE_PLAYING : PlaybackStateCompat.STATE_PAUSED, mPlayer.getPosition(), 1.0f);
		builder.setActions(PlaybackStateCompat.ACTION_PLAY_PAUSE | PlaybackStateCompat.ACTION_STOP |
				PlaybackStateCompat.ACTION_SKIP_TO_NEXT | PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS |
				PlaybackStateCompat.ACTION_PLAY_FROM_URI | PlaybackStateCompat.ACTION_SEEK_TO |
				PlaybackStateCompat.ACTION_SET_SHUFFLE_MODE | PlaybackStateCompat.ACTION_SET_REPEAT_MODE);
		mSession.setPlaybackState(builder.build());
		mNotificationHelper.updateNotification();
	}

	/**
	 *
	 */
	private int getCurrentCardId() {
		int mCardId = -1;
		Cursor mCursor = CursorFactory.makeCardCursor(this);
		if (mCursor != null) {
			if (mCursor.moveToFirst()) {
				mCardId = mCursor.getInt(0);
				Log.d(TAG, "CardId=" + mCardId);
			}
			mCursor.close();
		}
		return mCardId;
	}

	/**
	 * update track & album cursor using Uri
	 *
	 * @param uri uri of the audio track
	 */
	private void updateTrackInformation(@NonNull Uri uri) {
		Cursor cursor = null;
		// get information from MediaStore directly
		if (uri.toString().startsWith(Media.EXTERNAL_CONTENT_URI.toString())) {
			cursor = CursorFactory.makeTrackCursor(this, uri, true);
		}
		// use file path to get information
		else if (uri.getScheme() != null && uri.getScheme().startsWith("file")) {
			cursor = CursorFactory.makeTrackCursor(this, uri.getPath());
		}
		// use absolute file path
		else if (uri.getPath() != null && uri.getPath().startsWith("/root/")) {
			String path = uri.getPath().substring(5);
			cursor = CursorFactory.makeTrackCursor(this, path);
		}
		// use audio ID to get information
		else if (uri.getLastPathSegment() != null && uri.getLastPathSegment().matches("audio:\\d{1,18}")) {
			long id = Long.parseLong(uri.getLastPathSegment().substring(6));
			cursor = CursorFactory.makeTrackCursor(this, id);
		}
		// use generic document ID
		else if (uri.getLastPathSegment() != null && uri.getLastPathSegment().matches("\\d{1,18}")) {
			long id = Long.parseLong(uri.getLastPathSegment());
			cursor = CursorFactory.makeTrackCursor(this, id);
		}
		// fallback, use content resolver to get file information
		if (cursor == null) {
			Cursor fileCursor = CursorFactory.makeTrackCursor(this, uri, false);
			if (fileCursor != null) {
				if (fileCursor.moveToFirst()) {
					// find track by document ID
					int idxName = fileCursor.getColumnIndex(FileColumns.DOCUMENT_ID);
					if (idxName >= 0) {
						String name = fileCursor.getString(idxName);
						if (name != null) {
							int divider = name.indexOf(":");
							if (divider >= 0 && name.length() > 1) {
								name = name.substring(divider + 1);
							}
							// check if suffix is a document ID
							if (name.matches("\\d{1,18}")) {
								cursor = CursorFactory.makeTrackCursor(this, Long.parseLong(name));
							}
							// try to open as relative file path
							if (cursor == null) {
								cursor = CursorFactory.makeTrackCursor(this, name);
							}
						}
					}
					// find track by file path
					if (cursor == null) {
						idxName = fileCursor.getColumnIndex(FileColumns.DATA);
						if (idxName >= 0) {
							String name = fileCursor.getString(idxName);
							cursor = CursorFactory.makeTrackCursor(this, name);
						}
					}
					fileCursor.close();
				}
			}
		}
		if (cursor != null) {
			updateTrackInformation(cursor);
		} else {
			clearCurrentTrackInformation();
			Log.e(TAG, "failed to open track! uri=\"" + uri + "\"");
		}
	}

	/**
	 * update track information using song ID
	 *
	 * @param id ID of the track to update
	 */
	private void updateTrackInformation(long id) {
		if (id != -1L) {
			Cursor cursor = CursorFactory.makeTrackCursor(this, id);
			updateTrackInformation(cursor);
		} else {
			clearCurrentTrackInformation();
		}
	}

	/**
	 * read track information from cursor then close
	 *
	 * @param cursor cursor with track information
	 */
	private void updateTrackInformation(@Nullable Cursor cursor) {
		Song song = null;
		Album album = null;
		try {
			if (cursor != null) {
				if (cursor.moveToFirst()) {
					long songId = cursor.getLong(0);
					String songName = cursor.getString(1);
					String artistName = cursor.getString(2);
					String albumName = cursor.getString(3);
					long length = cursor.getLong(4);
					long artistId = cursor.getLong(5);
					long albumId = cursor.getLong(6);
					String path = cursor.getString(7);
					song = new Song(songId, artistId, albumId, songName, artistName, albumName, length, path);
				}
				cursor.close();
			}
			if (song != null) {
				cursor = CursorFactory.makeAlbumCursor(this, song.getAlbumId());
				if (cursor != null) {
					if (cursor.moveToFirst()) {
						long id = cursor.getLong(cursor.getColumnIndexOrThrow(Media._ID));
						String name = cursor.getString(cursor.getColumnIndexOrThrow(AlbumColumns.ALBUM));
						String artist = cursor.getString(cursor.getColumnIndexOrThrow(AlbumColumns.ARTIST));
						int count = cursor.getInt(cursor.getColumnIndexOrThrow(AlbumColumns.NUMBER_OF_SONGS));
						String year = cursor.getString(cursor.getColumnIndexOrThrow(AlbumColumns.FIRST_YEAR));
						album = new Album(id, name, artist, count, year, true);
					}
					cursor.close();
				}
			}
			currentAlbum = album;
			currentSong = song;
			notifyChange(CHANGED_META);
		} catch (Exception exception) {
			Log.e(TAG, "failed to set track information");
		}
	}

	/**
	 * clear information about the current selected track
	 */
	private void clearCurrentTrackInformation() {
		currentAlbum = null;
		currentSong = null;
		notifyChange(CHANGED_META);
	}

	/**
	 * prepare current track of the queue for playback and update track information
	 * if an error occurs try the next tracks
	 */
	private void openCurrentAndNext() {
		// reset current playback
		if (mPlayer.isPlaying())
			mPlayer.pause(true);
		if (!mPlayList.isEmpty() && mPlayList.getPosition() >= 0) {
			for (int retry = 0; retry < RETRY_COUNT; retry++) {
				// try to open current track
				long trackId = mPlayList.getSelected();
				if (trackId != -1L) {
					Uri uri = Uri.parse(Media.EXTERNAL_CONTENT_URI + "/" + trackId);
					if (mPlayer.setDataSource(getApplicationContext(), uri)) {
						updateTrackInformation(trackId);
						setNextTrack(false);
						return;
					}
				} else {
					Log.w(TAG, "openCurrentTrack(): Invalid track ID!");
					clearCurrentTrackInformation();
					return;
				}
				// go to next track if an error occurred
				int newPos = incrementPosition(mPlayList.getPosition(), false);
				mPlayList.setPosition(newPos);
			}
			Log.w(TAG, "openCurrentTrack(): Failed to open track!");
		} else {
			Log.w(TAG, "openCurrentTrack(): Playlist invalid: " + mPlayList);
		}
		clearCurrentTrackInformation();
	}

	/**
	 * Initializes the next track to be played and sets the next track position
	 *
	 * @param force true to force to next track (ignore repeat state or end of playlist)
	 */
	private void setNextTrack(boolean force) {
		int nextPos = mPlayList.getPosition();
		// search for the next playable track
		for (int i = 0; i < RETRY_COUNT; i++) {
			nextPos = incrementPosition(nextPos, force);
			long trackId = mPlayList.get(nextPos);
			if (trackId != -1L) {
				Uri uri = Uri.parse(Media.EXTERNAL_CONTENT_URI + "/" + trackId);
				if (mPlayer.setNextDataSource(getApplicationContext(), uri)) {
					// stop searching if the next track was set successfully
					mPlayList.setNextPosition(nextPos);
					return;
				}
			} else {
				break;
			}
		}
		// if no track was found, stop player after playback end
		mPlayer.setNextDataSource(getApplicationContext(), null);
		mPlayList.setNextPosition(-1);
	}

	/**
	 * increment current play position of the queue
	 *
	 * @param force True to force the player onto the track next, false otherwise.
	 * @return The next position to play.
	 */
	private int incrementPosition(int pos, boolean force) {
		// return current play position
		if (!force && mRepeatMode == REPEAT_CURRENT) {
			return Math.max(pos, 0);
		}
		switch (mShuffleMode) {
			// shuffle current tracks in the queue
			case SHUFFLE_NORMAL:
				// only add current track to history when moving to another track
				if (force && pos >= 0) {
					mShuffleList.addHistory(pos);
				}
				// reset shuffle list after reaching the end or refreshing
				if (mShuffleList.size() != mPlayList.size()) {
					// create a new shuffle list. if fail, prevent playing
					if (!makeShuffleList(false)) {
						return -1;
					}
				}
				// get index of the new track
				return mShuffleList.next();

			// Party shuffle
			case SHUFFLE_AUTO:
				makeShuffleList(true);
				return pos + 1;

			default:
				if (pos >= mPlayList.size() - 1) {
					if (mRepeatMode == REPEAT_NONE && !force) {
						return -1;
					}
					if (mRepeatMode == REPEAT_ALL || force) {
						return 0;
					}
					return -1;
				} else {
					return pos + 1;
				}
		}
	}

	/**
	 * decrement current play position in the queue
	 *
	 * @param pos position to decrement
	 * @return play position
	 */
	private int decrementPosition(int pos) {
		// use last played track position
		if (mShuffleMode == SHUFFLE_NORMAL || mShuffleMode == SHUFFLE_AUTO) {
			// Go to previously-played track and remove it from the history
			int lastIndex = mShuffleList.undoHistory();
			if (lastIndex >= 0) {
				return lastIndex;
			}
		}
		// decrement position
		if (pos > 0) {
			return pos - 1;
		} else if (pos == 0) {
			return mPlayList.size() - 1;
		}
		return -1;
	}

	/**
	 * Creates a shuffled playlist
	 *
	 * @param partyShuffle true to create a party shuffle list with all available tracks
	 *                     false to shuffle current queue
	 *
	 * @return true if shuffle list was created successfully, false otherwise
	 */
	private boolean makeShuffleList(boolean partyShuffle) {
		try {
			if (partyShuffle) {
				Cursor cursor = CursorFactory.makeTrackCursor(this);
				if (cursor != null) {
					if (cursor.moveToFirst()) {
						long[] ids = new long[cursor.getCount()];
						for (int i = 0; i < ids.length; i++) {
							ids[i] = cursor.getLong(0);
							if (!cursor.moveToNext()) {
								break;
							}
						}
						mPlayList.setItems(ids);
					}
					cursor.close();
				}
			}
			if (mPlayList.isEmpty()) {
				mShuffleMode = SHUFFLE_NONE;
			} else {
				mShuffleList.shuffle(mPlayList.size());
				savePlaybackList(true);
				mShuffleMode = SHUFFLE_NORMAL;
				return true;
			}
		} catch (RuntimeException e) {
			Log.e(TAG, "makeShuffleList()", e);
		}
		return false;
	}

	/**
	 * Saves the queue
	 *
	 * @param full true to save also the playback list and history
	 */
	private void savePlaybackList(boolean full) {
		if (full) {
			int cardId = getCurrentCardId();
			playlistStore.setPlaylist(PlaylistStore.PLAYLIST_TYPE_PLAYBACK, cardId, mPlayList.getItems());
			if (mShuffleMode != SHUFFLE_NONE) {
				playlistStore.setPlaylist(PlaylistStore.PLAYLIST_TYPE_HISTORY, cardId, mShuffleList.getHistory());
			}
		}
		playerSettings.setCursorPosition(mPlayList.getPosition());
		if (mPlayer.initialized()) {
			playerSettings.setSeekPosition(mPlayer.getPosition());
		}
		playerSettings.setRepeatAndShuffleMode(mRepeatMode, mShuffleMode);
	}

	/**
	 * initialize the playback list
	 */
	private void initPlaybackList() {
		mRepeatMode = playerSettings.getRepeatMode();
		mShuffleMode = playerSettings.getShuffleMode();
		int cardId = getCurrentCardId();

		// init playback list
		long[] ids = playlistStore.getPlaylist(PlaylistStore.PLAYLIST_TYPE_PLAYBACK, cardId);
		mPlayList.setItems(ids);
		mPlayList.setPosition(playerSettings.getCursorPosition());

		// init playback/shuffle
		if (!mPlayList.isEmpty()) {
			// init shuffle list
			if (mShuffleMode == SHUFFLE_NORMAL) {
				long[] history = playlistStore.getPlaylist(PlaylistStore.PLAYLIST_TYPE_HISTORY, cardId);
				mShuffleList.setHistory(history);
			} else {
				mShuffleMode = SHUFFLE_NONE;
			}
			// init playback
			openCurrentAndNext();
			mPlayer.setPosition(playerSettings.getSeekPosition());
		} else {
			// reset playback/shuffle
			clearCurrentTrackInformation();
			mShuffleList.clear();
		}
		updatePlaybackState();
	}

	/**
	 * used by widgets or other intents to change playback state
	 *
	 * @return true if command changes the playback status
	 */
	private boolean handleCommandIntent(Intent intent) {
		String action = intent.getAction();
		// go to next track
		if (ACTION_NEXT.equals(action)) {
			gotoNext();
		}
		// go to previous track
		else if (ACTION_PREVIOUS.equals(action)) {
			gotoPrev();
		}
		// pause/play track
		else if (ACTION_TOGGLEPAUSE.equals(action)) {
			if (mPlayer.isPlaying()) {
				pause(false);
			} else {
				play();
			}
		}
		// stop track/dismiss notification
		else if (ACTION_STOP.equals(action)) {
			stop();
			releaseService(false);
			return false;
		}
		// set 'repeat' mode
		else if (ACTION_REPEAT.equals(action)) {
			if (mRepeatMode == REPEAT_NONE) {
				setRepeatMode(REPEAT_ALL);
			} else if (mRepeatMode == REPEAT_ALL) {
				setRepeatMode(REPEAT_CURRENT);
			} else {
				setRepeatMode(REPEAT_NONE);
			}
			return false;
		}
		// set 'shuffle' mode
		else if (ACTION_SHUFFLE.equals(action)) {
			if (mShuffleMode == SHUFFLE_NONE) {
				setShuffleMode(SHUFFLE_NORMAL);
			} else if (mShuffleMode == SHUFFLE_NORMAL || mShuffleMode == SHUFFLE_AUTO) {
				setShuffleMode(SHUFFLE_NONE);
			}
			return false;
		}
		// update widget
		else if (ACTION_WIDGET_UPDATE.equals(action)) {
			notifyChange(CHANGED_WIDGET);
		}
		return true;
	}

	/**
	 * send broadcast to update all installed widgets
	 *
	 * @param intent Intent containing playback information
	 */
	private void updateWidgets(Intent intent) {
		for (Class<?> widget : WIDGETS) {
			int[] ids = widgetManager.getAppWidgetIds(new ComponentName(getApplicationContext(), widget));
			if (ids.length > 0) {
				Intent widgetIntent = new Intent(intent);
				widgetIntent.setClass(getApplicationContext(), widget);
				widgetIntent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
				widgetIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids);
				sendBroadcast(widgetIntent);
			}
		}
	}

	/**
	 * Notify the change-receivers that something has changed.
	 *
	 * @param what what changed e.g. {@link #CHANGED_PLAYSTATE,#CHANGED_META}
	 */
	private void notifyChange(String what) {
		Song song = currentSong;
		Album album = currentAlbum;
		// send broadcast to activities and widgets
		Intent intent = new Intent(what);
		intent.putExtra(KEY_IS_PLAYING, mPlayer.isPlaying());
		intent.putExtra(KEY_SHUFFLE, mShuffleMode);
		intent.putExtra(KEY_REPEAT, mRepeatMode);
		if (song != null && album != null) {
			intent.putExtra(KEY_SONG, (Serializable) song);
			intent.putExtra(KEY_ALBUM, (Serializable) album);
		}
		sendBroadcast(intent);
		// handle playback updates
		switch (what) {
			case CHANGED_META:
				updateMetadata();
				// Increase the play count for favorite songs.
				if (song != null && album != null) {
					popularStore.addSong(song);
					recentStore.addAlbum(album);
				}
				playerSettings.setCursorPosition(mPlayList.getPosition());
				updateWidgets(intent);
				break;

			case CHANGED_PLAYSTATE:
				updatePlaybackState();
				if (isForeground && !mPlayer.isPlaying()) {
					shutdownHandler.start();
				} else {
					shutdownHandler.stop();
				}
				if (mPlayer.initialized() && !mPlayer.isPlaying()) {
					playerSettings.setSeekPosition(mPlayer.getPosition());
				}
				updateWidgets(intent);
				break;

			case CHANGED_QUEUE:
				savePlaybackList(true);
				updatePlaybackState();
				if (mPlayer.isPlaying()) {
					setNextTrack(false);
				}
				break;

			case CHANGED_SEEK:
				updatePlaybackState();
				playerSettings.setSeekPosition(mPlayer.getPosition());
				break;

			case CHANGED_WIDGET:
			case CHANGED_REPEATMODE:
			case CHANGED_SHUFFLEMODE:
				updateWidgets(intent);
				// fall through

			default:
				savePlaybackList(false);
				break;
		}
	}
}