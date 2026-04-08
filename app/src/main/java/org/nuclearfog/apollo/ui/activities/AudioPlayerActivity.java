package org.nuclearfog.apollo.ui.activities;

import android.app.SearchManager;
import android.app.SearchableInfo;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore.Audio.Albums;
import android.provider.MediaStore.Audio.Artists;
import android.provider.MediaStore.Audio.Playlists;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.SearchView.OnQueryTextListener;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.FragmentContainerView;
import androidx.lifecycle.ViewModelProvider;

import org.nuclearfog.apollo.BuildConfig;
import org.nuclearfog.apollo.R;
import org.nuclearfog.apollo.async.AsyncExecutor.AsyncCallback;
import org.nuclearfog.apollo.async.loader.AlbumSongLoader;
import org.nuclearfog.apollo.async.loader.ArtistSongLoader;
import org.nuclearfog.apollo.async.loader.PlaylistSongLoader;
import org.nuclearfog.apollo.cache.ImageFetcher;
import org.nuclearfog.apollo.model.Album;
import org.nuclearfog.apollo.model.Song;
import org.nuclearfog.apollo.receiver.PlaybackBroadcastReceiver;
import org.nuclearfog.apollo.receiver.PlaybackBroadcastReceiver.PlayStatusListener;
import org.nuclearfog.apollo.store.FavoritesStore;
import org.nuclearfog.apollo.store.preferences.AppPreferences;
import org.nuclearfog.apollo.ui.fragments.QueueFragment;
import org.nuclearfog.apollo.ui.views.ForwardRewindButton;
import org.nuclearfog.apollo.ui.views.ForwardRewindButton.RepeatListener;
import org.nuclearfog.apollo.ui.views.PlayPauseButton;
import org.nuclearfog.apollo.ui.views.PlayerSeekbar;
import org.nuclearfog.apollo.ui.views.PlayerSeekbar.OnPlayerSeekListener;
import org.nuclearfog.apollo.ui.views.ShuffleRepeatButton;
import org.nuclearfog.apollo.utils.AnimatorUtils;
import org.nuclearfog.apollo.utils.ApolloUtils;
import org.nuclearfog.apollo.utils.FragmentViewModel;
import org.nuclearfog.apollo.utils.MusicUtils;
import org.nuclearfog.apollo.utils.NavUtils;
import org.nuclearfog.apollo.utils.ServiceBinder.ServiceBinderCallback;
import org.nuclearfog.apollo.utils.ThemeUtils;

import java.io.File;
import java.util.List;

/**
 * Apollo's "now playing" interface.
 *
 * @author Andrew Neal (andrewdneal@gmail.com)
 * @author nuclearfog
 */
public class AudioPlayerActivity extends AppCompatActivity implements ServiceBinderCallback, OnPlayerSeekListener,
		OnQueryTextListener, OnClickListener, RepeatListener, PlayStatusListener {

	private static final String TAG = "AudioPlayerActivity";

	/**
	 * intent used to open audio player
	 */
	public static final String ACTION_OPEN_PLAYER = BuildConfig.APPLICATION_ID + ".AUDIO_PLAYER";

	/**
	 * key to enable/disable "back press" handling. Setting
	 * true openes the home activity when pressing the back button
	 */
	public static final String KEY_BACK_PRESS_CLOSE = "close_activity";

	/**
	 * MIME type for sharing songs
	 */
	private static final String MIME_AUDIO = "audio/*";

	/**
	 *
	 */
	private static final String KEY_QUEUE_VISIBILITY = "queue_visibility";

	/**
	 * seek jump limit when using fast-forward/rewind in milliseconds
	 * The speed factor is calculated by multiplying this value with {@link ForwardRewindButton#S_INTERVAL}
	 */
	private static final long SCAN_MAX_TIME = 5000;
	/**
	 * callback used to play songs after being loaded asynchronously
	 */
	private AsyncCallback<List<Song>> onPlaySongs = this::onPlaySongs;
	/**
	 * Play & pause button
	 */
	private PlayPauseButton mPlayPauseButton;
	/**
	 * Repeat & Shuffle button
	 */
	private ShuffleRepeatButton mRepeatButton, mShuffleButton;
	/**
	 * Track name
	 */
	private TextView mTrackName;
	/**
	 * Artist name
	 */
	private TextView mArtistName;
	/**
	 * Album art
	 */
	private ImageView mAlbumArt;

	private View controls, albumArtBorder1;

	@Nullable
	private View albumArtBorder2;
	/**
	 * Tiny artwork
	 */
	private ImageView mAlbumArtSmall;
	/**
	 * Queue switch
	 */
	private ImageView mQueueSwitch;
	/**
	 * playback seekbar
	 */
	private PlayerSeekbar playerSeekbar;
	/**
	 * Broadcast receiver
	 */
	private PlaybackBroadcastReceiver mPlaybackStatus;

	private FragmentContainerView queueContainer;
	/**
	 * Image cache
	 */
	private ImageFetcher mImageFetcher;
	/**
	 * Theme resources
	 */
	private ThemeUtils mResources;

	private AppPreferences mPrefs;

	private FragmentViewModel viewModel;

	private PlaylistSongLoader playlistSongLoader;
	private ArtistSongLoader artistSongLoader;
	private AlbumSongLoader albumSongLoader;

	/**
	 * intent set if player should play audio file
	 */
	@Nullable
	private Intent playerIntent = null;

	private int playPos = 0;

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setVolumeControlStream(AudioManager.STREAM_MUSIC);
		// init views
		setContentView(R.layout.activity_player_base);
		mPlayPauseButton = findViewById(R.id.action_button_play);
		mShuffleButton = findViewById(R.id.action_button_shuffle);
		mRepeatButton = findViewById(R.id.action_button_repeat);
		View root = findViewById(R.id.player_root);
		Toolbar toolbar = findViewById(R.id.player_toolbar);
		ForwardRewindButton mPreviousButton = findViewById(R.id.action_button_previous);
		ForwardRewindButton mNextButton = findViewById(R.id.action_button_next);
		queueContainer = findViewById(R.id.audio_player_queue_fragment);
		controls = findViewById(R.id.audio_player_controls);
		mTrackName = findViewById(R.id.audio_player_track_name);
		mArtistName = findViewById(R.id.audio_player_artist_name);
		mAlbumArt = findViewById(R.id.audio_player_album_art);
		albumArtBorder1 = findViewById(R.id.audio_player_album_border);
		albumArtBorder2 = findViewById(R.id.audio_player_album_border_bottom);
		mAlbumArtSmall = findViewById(R.id.audio_player_switch_album_art);
		mQueueSwitch = findViewById(R.id.audio_player_switch_queue);
		mQueueSwitch.setImageResource(R.drawable.btn_switch_queue);
		playerSeekbar = findViewById(R.id.player_progress);
		//
		mResources = new ThemeUtils(this);
		mPrefs = AppPreferences.getInstance(this);
		playlistSongLoader = new PlaylistSongLoader(this);
		artistSongLoader = new ArtistSongLoader(this);
		albumSongLoader = new AlbumSongLoader(this);
		mImageFetcher = new ImageFetcher(this);
		mPlaybackStatus = new PlaybackBroadcastReceiver(this);
		viewModel = new ViewModelProvider(this).get(FragmentViewModel.class);
		playerIntent = getIntent();
		// Theme the action bar
		if (toolbar != null)
			setSupportActionBar(toolbar);
		ActionBar actionBar = getSupportActionBar();
		if (actionBar != null) {
			mResources.themeActionBar(actionBar, R.string.app_name);
			actionBar.setDisplayHomeAsUpEnabled(true);
		}
		// set activity background
		mResources.setBackground(root);
		controls.setVisibility(View.INVISIBLE);
		// go to home activity if there is any missing permission
		if (!ApolloUtils.permissionsGranted(this)) {
			NavUtils.goHome(this);
			return;
		}
		// set visibility of the queue layout after screen rotation
		if (savedInstanceState != null) {
			boolean queueVisible = savedInstanceState.getBoolean(KEY_QUEUE_VISIBILITY, false);
			if (queueVisible) {
				setQueueVisibility(true);
				setQueueTrack();
			}
		}
		ApolloUtils.setWakelock(this);

		mPreviousButton.setRepeatListener(this);
		mNextButton.setRepeatListener(this);
		mPreviousButton.setOnClickListener(this);
		mNextButton.setOnClickListener(this);
		mShuffleButton.setOnClickListener(this);
		mRepeatButton.setOnClickListener(this);
		mPlayPauseButton.setOnClickListener(this);
		playerSeekbar.setOnPlayerSeekListener(this);
		mTrackName.setOnClickListener(this);
		mArtistName.setOnClickListener(this);
		mQueueSwitch.setOnClickListener(this);
		mAlbumArtSmall.setOnClickListener(this);
		if (playerIntent != null && playerIntent.getBooleanExtra(KEY_BACK_PRESS_CLOSE, true)) {
			getOnBackPressedDispatcher().addCallback(new OnBackPressedCallback(true) {
				@Override
				public void handleOnBackPressed() {
					NavUtils.goHome(AudioPlayerActivity.this);
				}
			});
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onNewIntent(Intent intent) {
		playerIntent = intent;
		super.onNewIntent(intent);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void onStart() {
		super.onStart();
		// register playback status receiver
		ContextCompat.registerReceiver(this, mPlaybackStatus, mPlaybackStatus.getFilter(), ContextCompat.RECEIVER_EXPORTED);
		// bind activity to service
		MusicUtils.bindToService(this, this);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void onStop() {
		// Unregister the receiver
		unregisterReceiver(mPlaybackStatus);
		MusicUtils.unbindFromService(this);
		mImageFetcher.clear();
		super.onStop();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void onSaveInstanceState(@NonNull Bundle savedInstanceState) {
		savedInstanceState.putBoolean(KEY_QUEUE_VISIBILITY, queueContainer.getVisibility() == View.VISIBLE);
		super.onSaveInstanceState(savedInstanceState);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void onDestroy() {
		playerSeekbar.release();
		albumSongLoader.cancel();
		artistSongLoader.cancel();
		playlistSongLoader.cancel();
		super.onDestroy();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean onCreateOptionsMenu(@NonNull Menu menu) {
		// Search view
		getMenuInflater().inflate(R.menu.search, menu);
		// Favorite action
		getMenuInflater().inflate(R.menu.favorite, menu);
		// Shuffle all
		getMenuInflater().inflate(R.menu.shuffle, menu);
		// Share, ringtone, and equalizer
		getMenuInflater().inflate(R.menu.audio_player, menu);
		// Settings
		getMenuInflater().inflate(R.menu.activity_base, menu);
		// Theme the search icon
		MenuItem searchAction = menu.findItem(R.id.menu_search);
		SearchView searchView = (SearchView) searchAction.getActionView();
		// Add voice search
		SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
		SearchableInfo searchableInfo = searchManager.getSearchableInfo(getComponentName());
		if (searchView != null) {
			searchView.setSearchableInfo(searchableInfo);
			// Perform the search
			searchView.setOnQueryTextListener(this);
		}
		return true;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean onPrepareOptionsMenu(@NonNull Menu menu) {
		MenuItem favorite = menu.findItem(R.id.menu_favorite);
		// Add fav icon
		Song song = MusicUtils.getCurrentTrack(this);
		if (song != null) {
			boolean exits = FavoritesStore.getInstance(this).exists(song.getId());
			mResources.setFavoriteIcon(favorite, exits);
		}
		return super.onPrepareOptionsMenu(menu);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean onOptionsItemSelected(@NonNull MenuItem item) {
		int vId = item.getItemId();
		if (vId == android.R.id.home) {
			// Go back to the home activity
			NavUtils.goHome(this);
		} else if (vId == R.id.menu_shuffle) {
			// Shuffle all the songs
			MusicUtils.shuffleAll(this);
		} else if (vId == R.id.menu_favorite) {
			// Toggle the current track as a favorite and update the menu item
			Song song = MusicUtils.getCurrentTrack(this);
			if (song != null) {
				FavoritesStore favoriteStore = FavoritesStore.getInstance(this);
				if (favoriteStore.exists(song.getId()))
					favoriteStore.removeFavorite(song.getId());
				else
					favoriteStore.addFavorite(song);
				invalidateOptionsMenu();
			}
		} else if (vId == R.id.menu_audio_player_ringtone) {
			// Set the current track as a ringtone
			Song song = MusicUtils.getCurrentTrack(this);
			if (song != null) {
				MusicUtils.setRingtone(this, song.getId());
			}
		} else if (vId == R.id.menu_audio_player_share) {
			// Share the current meta data
			shareCurrentTrack();
		} else if (vId == R.id.menu_audio_player_equalizer) {
			if (mPrefs.isExternalAudioFxPreferred() && ApolloUtils.isEqualizerInstalled(this)) {
				// Sound effects
				ApolloUtils.notifyExternalEqualizer(this, MusicUtils.getAudioSessionId(this));
				NavUtils.openEffectsPanel(this);
			} else {
				Intent intent = new Intent(this, AudioFxActivity.class);
				intent.putExtra(AudioFxActivity.KEY_SESSION_ID, MusicUtils.getAudioSessionId(this));
				startActivity(intent);
			}
		} else if (vId == R.id.menu_settings) {
			// Settings
			NavUtils.openSettings(this);
		} else if (vId == R.id.menu_audio_player_delete) {
			// Delete current song
			Song currentSong = MusicUtils.getCurrentTrack(this);
			if (currentSong != null) {
				MusicUtils.openDeleteDialog(this, currentSong.getName(), currentSong.getId());
			}
		} else if (item.getItemId() == R.id.menu_close) {
			NavUtils.closeApp(this);
		} else {
			return super.onOptionsItemSelected(item);
		}
		return true;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onServiceConnected() {
		// Check whether we were asked to start any playback
		startPlayback();
		// Set the playback drawables
		updatePlaybackControls();
		// Update the favorites icon
		invalidateOptionsMenu();
		// refresh queue after connected
		refreshQueue();
		// enable/disable fade effect
		MusicUtils.setFadeEffect(this);
		// initialize audio effects for media playback
		MusicUtils.initAudioEffects(this);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (requestCode == MusicUtils.REQUEST_DELETE_FILES && resultCode == RESULT_OK) {
			MusicUtils.refresh(this);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean onQueryTextSubmit(String query) {
		// Open the search activity
		NavUtils.openSearch(this, query);
		return true;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean onQueryTextChange(String newText) {
		// Nothing to do
		return false;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onClick(@NonNull View v) {
		if (v.getId() == R.id.audio_player_artist_name || v.getId() == R.id.audio_player_track_name) {
			Album album = MusicUtils.getCurrentAlbum(this);
			if (album != null) {
				NavUtils.openAlbumProfile(this, album);
			}
		}
		// Show the queue, hide the artwork
		else if (v.getId() == R.id.audio_player_switch_queue) {
			setQueueVisibility(true);
			setQueueTrack();
		}
		// Show the artwork, hide the queue
		else if (v.getId() == R.id.audio_player_switch_album_art) {
			setQueueVisibility(false);
		}
		// repeat button clicked
		else if (v.getId() == R.id.action_button_repeat) {
			int mode = MusicUtils.cycleRepeat(this);
			mRepeatButton.updateButtonState(mode);
		}
		// shuffle button clicked
		else if (v.getId() == R.id.action_button_shuffle) {
			int mode = MusicUtils.cycleShuffle(this);
			mShuffleButton.updateButtonState(mode);
		}
		// play/pause button clicked
		else if (v.getId() == R.id.action_button_play) {
			MusicUtils.togglePlayPause(this);
		}
		// go to previous track
		else if (v.getId() == R.id.action_button_previous) {
			MusicUtils.previous(this);
		}
		// go to next track
		else if (v.getId() == R.id.action_button_next) {
			MusicUtils.next(this);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onRepeat(@NonNull View v, int repeatCount) {
		if (v.getId() == R.id.action_button_previous) {
			if (repeatCount > 0)
				scan(false);
		} else if (v.getId() == R.id.action_button_next) {
			if (repeatCount > 0)
				scan(true);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onMetaChange() {
		// Current info
		updatePlaybackControls();
		// Update the favorites icon
		invalidateOptionsMenu();
		// jump to current track
		setQueueTrack();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onStateChange() {
		// Set the play and pause image
		boolean playing = MusicUtils.isPlaying(this);
		long position = MusicUtils.getPositionMillis(this);
		mPlayPauseButton.updateState(playing);
		playerSeekbar.setPlayStatus(playing);
		playerSeekbar.setCurrentTime(position);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onModeChange() {
		// Set the repeat image
		mRepeatButton.updateButtonState(MusicUtils.getRepeatMode(this));
		// Set the shuffle image
		mShuffleButton.updateButtonState(MusicUtils.getShuffleMode(this));
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onSeek(long position) {
		MusicUtils.seek(this, position);
		playerSeekbar.seek(MusicUtils.getPositionMillis(this));
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void refresh() {
	}

	/**
	 *
	 */
	private long parseIdFromIntent(@NonNull Intent intent, String longKey, String stringKey) {
		long id = intent.getLongExtra(longKey, -1L);
		if (id == -1L) {
			String idString = intent.getStringExtra(stringKey);
			if (idString != null) {
				try {
					id = Long.parseLong(idString);
				} catch (NumberFormatException e) {
					// ignore
				}
			}
		}
		return id;
	}

	/**
	 * Checks whether the passed intent contains a playback request,
	 * and starts playback if that's the case
	 */
	@SuppressWarnings("deprecation")
	private void startPlayback() {
		if (playerIntent != null) {
			Uri uri = playerIntent.getData();
			String mimeType = playerIntent.getType();
			// open file
			if (uri != null && !uri.toString().isEmpty()) {
				MusicUtils.playFile(this, uri);
				refreshQueue();
			}
			// open playlist
			else if (Playlists.CONTENT_TYPE.equals(mimeType)) {
				long id = parseIdFromIntent(playerIntent, "playlistId", "playlist");
				if (id != -1L) {
					playPos = 0;
					playlistSongLoader.execute(id, onPlaySongs);
				}
			}
			// open album
			else if (Albums.CONTENT_TYPE.equals(mimeType)) {
				long id = parseIdFromIntent(playerIntent, "albumId", "album");
				if (id != -1L) {
					playPos = playerIntent.getIntExtra("position", 0);
					albumSongLoader.execute(id, onPlaySongs);
				}
			}
			// open artist
			else if (Artists.CONTENT_TYPE.equals(mimeType)) {
				long id = parseIdFromIntent(playerIntent, "artistId", "artist");
				if (id != -1L) {
					playPos = playerIntent.getIntExtra("position", 0);
					artistSongLoader.execute(id, onPlaySongs);
				}
			}
			// reset intent if consumed
			playerIntent = null;
		}
	}

	/**
	 * set queue layout visibility
	 *
	 * @param visible true to make queue layout visible, false to show album artwork
	 */
	private void setQueueVisibility(boolean visible) {
		AnimatorUtils.fade(queueContainer, visible);
		// switch buttons
		if (visible) {
			mAlbumArtSmall.setVisibility(View.VISIBLE);
			mQueueSwitch.setVisibility(View.INVISIBLE);
		} else {
			mAlbumArtSmall.setVisibility(View.INVISIBLE);
			mQueueSwitch.setVisibility(View.VISIBLE);
		}
		// Fade out the artwork
		AnimatorUtils.fade(albumArtBorder1, !visible);
		if (albumArtBorder2 != null)
			AnimatorUtils.fade(albumArtBorder2, !visible);
		AnimatorUtils.fade(mAlbumArt, !visible);
	}

	/**
	 * Sets the correct drawable states for the playback controls.
	 */
	private void updatePlaybackControls() {
		Song song = MusicUtils.getCurrentTrack(this);
		Album album = MusicUtils.getCurrentAlbum(this);
		boolean isPlaying = MusicUtils.isPlaying(this);
		// fade in player control after initialization
		if (controls.getVisibility() != View.VISIBLE && controls.getAnimation() == null) {
			AnimatorUtils.fade(controls, true);
		}
		// Set the play and pause image
		mPlayPauseButton.updateState(isPlaying);
		// Set the shuffle image
		mShuffleButton.updateButtonState(MusicUtils.getShuffleMode(this));
		// Set the repeat image
		mRepeatButton.updateButtonState(MusicUtils.getRepeatMode(this));
		playerSeekbar.setTotalTime(MusicUtils.getDurationMillis(this));
		playerSeekbar.setCurrentTime(MusicUtils.getPositionMillis(this));
		playerSeekbar.setPlayStatus(isPlaying);
		// update track information
		if (song != null && album != null) {
			// Set the track name
			mTrackName.setText(song.getName());
			// Set the artist name
			mArtistName.setText(song.getArtist());
			// Set the album art
			mImageFetcher.loadAlbumImage(album.getId(), mAlbumArt);
			// Set the small artwork
			mImageFetcher.loadAlbumImage(album.getId(), mAlbumArtSmall);
		}
	}

	/**
	 * Used to scan backwards in time through the current track
	 *
	 * @param forward true to scan forward
	 */
	private void scan(boolean forward) {
		long duration = MusicUtils.getDurationMillis(this);
		long position = MusicUtils.getPositionMillis(this);
		long jump = Math.min(duration / 32L, SCAN_MAX_TIME);
		if (forward) {
			position = Math.min(duration, position + jump);
		} else {
			position = Math.max(0, position - jump);
		}
		onSeek(position);
	}

	/**
	 * Used to shared what the user is currently listening to
	 */
	private void shareCurrentTrack() {
		Song currentTrack = MusicUtils.getCurrentTrack(this);
		if (currentTrack != null) {
			try {
				File file = new File(currentTrack.getPath());
				Uri fileUri = FileProvider.getUriForFile(this, BuildConfig.APPLICATION_ID, file);
				Intent shareIntent = new Intent();
				shareIntent.setAction(Intent.ACTION_SEND);
				shareIntent.setDataAndType(fileUri, MIME_AUDIO);
				shareIntent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
				shareIntent.putExtra(Intent.EXTRA_STREAM, fileUri);
				if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.LOLLIPOP_MR1)
					shareIntent.setClipData(ClipData.newRawUri("", fileUri));
				startActivity(Intent.createChooser(shareIntent, getString(R.string.share_track_using)));
			} catch (Exception exception) {
				Log.e(TAG, "couldn't share track!", exception);
			}
		}
	}

	/**
	 * reload queue tracks
	 */
	private void refreshQueue() {
		viewModel.notify(QueueFragment.REFRESH);
	}

	/**
	 * set current track in the queue
	 */
	private void setQueueTrack() {
		viewModel.notify(QueueFragment.META_CHANGED);
	}

	/**
	 * called after songs loaded asynchronously to play
	 */
	private void onPlaySongs(List<Song> songs) {
		if (!songs.isEmpty()) {
			MusicUtils.playAll(this, songs, playPos);
			refreshQueue();
			playPos = 0;
		}
	}
}