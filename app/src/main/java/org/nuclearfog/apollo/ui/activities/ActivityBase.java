package org.nuclearfog.apollo.ui.activities;

import android.app.SearchManager;
import android.app.SearchableInfo;
import android.content.Context;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.SearchView.OnQueryTextListener;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import org.nuclearfog.apollo.R;
import org.nuclearfog.apollo.cache.ImageFetcher;
import org.nuclearfog.apollo.model.Album;
import org.nuclearfog.apollo.model.Song;
import org.nuclearfog.apollo.receiver.PlaybackBroadcastReceiver;
import org.nuclearfog.apollo.receiver.PlaybackBroadcastReceiver.PlayStatusListener;
import org.nuclearfog.apollo.store.preferences.AppPreferences;
import org.nuclearfog.apollo.ui.views.PlayPauseButton;
import org.nuclearfog.apollo.ui.views.ShuffleRepeatButton;
import org.nuclearfog.apollo.utils.AnimatorUtils;
import org.nuclearfog.apollo.utils.ApolloUtils;
import org.nuclearfog.apollo.utils.Constants;
import org.nuclearfog.apollo.utils.MusicUtils;
import org.nuclearfog.apollo.utils.NavUtils;
import org.nuclearfog.apollo.utils.ServiceBinder.ServiceBinderCallback;
import org.nuclearfog.apollo.utils.ThemeUtils;

/**
 * A base {@link AppCompatActivity} used to update the bottom bar and bind to Apollo's service.
 *
 * @author Andrew Neal (andrewdneal@gmail.com)
 * @author nuclearfog
 */
public abstract class ActivityBase extends AppCompatActivity implements ServiceBinderCallback, OnClickListener, OnQueryTextListener, PlayStatusListener {

	/**
	 * request code for permission result
	 */
	private static final int REQ_CHECK_PERM = 0x1139398F;

	/**
	 * Play and pause button (BAB)
	 */
	private PlayPauseButton mPlayPauseButton;
	/**
	 * Shuffle & Repeat button (BAB)
	 */
	private ShuffleRepeatButton mShuffleButton, mRepeatButton;

	/**
	 * Track name (BAB)
	 */
	private TextView mTrackName;
	/**
	 * Artist name (BAB)
	 */
	private TextView mArtistName;
	/**
	 * Album art (BAB)
	 */
	private ImageView mAlbumArt;

	private HorizontalScrollView playbackControls;
	/**
	 * Broadcast receiver
	 */
	private PlaybackBroadcastReceiver mPlaybackStatus;

	private ImageFetcher imageFetcher;

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected final void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setVolumeControlStream(AudioManager.STREAM_MUSIC);
		setContentView(getContentView());
		View v = getWindow().getDecorView().getRootView();
		// Play and pause button
		mPlayPauseButton = findViewById(R.id.action_button_play);
		// Shuffle button
		mShuffleButton = findViewById(R.id.action_button_shuffle);
		// Repeat button
		mRepeatButton = findViewById(R.id.action_button_repeat);
		// Track name
		mTrackName = findViewById(R.id.bottom_action_bar_line_one);
		// Artist name
		mArtistName = findViewById(R.id.bottom_action_bar_line_two);
		// Album art
		mAlbumArt = findViewById(R.id.bottom_action_bar_album_art);
		// media controls
		playbackControls = findViewById(R.id.bottom_action_bar_scrollview);
		// next track button
		View previousButton = findViewById(R.id.action_button_previous);
		// previous track button
		View nextButton = findViewById(R.id.action_button_next);
		// background of bottom action bar
		View bottomActionBar = findViewById(R.id.bottom_action_bar_background);

		mPlaybackStatus = new PlaybackBroadcastReceiver(this);
		ThemeUtils mTheme = new ThemeUtils(this);
		imageFetcher = new ImageFetcher(this);

		// set bottom action bar color
		mTheme.setBackgroundColor(bottomActionBar);
		// set background
		mTheme.setBackground(v);
		ApolloUtils.setWakelock(this);

		previousButton.setOnClickListener(this);
		nextButton.setOnClickListener(this);
		bottomActionBar.setOnClickListener(this);
		mPlayPauseButton.setOnClickListener(this);
		mShuffleButton.setOnClickListener(this);
		mRepeatButton.setOnClickListener(this);
		mAlbumArt.setOnClickListener(this);

		// check permissions before initialization
		if (ApolloUtils.permissionsGranted(this)) {
			// initialize sub-class
			initialize();
		} else {
			ActivityCompat.requestPermissions(this, Constants.PERMISSIONS, REQ_CHECK_PERM);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void onStart() {
		super.onStart();
		// register play state callback
		ContextCompat.registerReceiver(this, mPlaybackStatus, mPlaybackStatus.getFilter(), ContextCompat.RECEIVER_EXPORTED);
		// check permissions before initialization
		if (ApolloUtils.permissionsGranted(this)) {
			// Bind Apollo's service
			MusicUtils.bindToService(this, this);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void onStop() {
		// Unregister the receiver
		unregisterReceiver(mPlaybackStatus);
		// Unbind from the service
		MusicUtils.unbindFromService(this);
		super.onStop();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onServiceConnected() {
		// fade in playback controls
		if (playbackControls.getVisibility() != View.VISIBLE) {
			AnimatorUtils.fade(playbackControls, true);
			playbackControls.scrollTo(0, 0);
		}
		// Set the playback drawables
		updatePlaybackControls();
		// Current info
		updateBottomActionBarInfo();
		// Update the favorites icon
		invalidateOptionsMenu();
		// enable/disable fade effect
		MusicUtils.setCrossfade(this, AppPreferences.getInstance(this).crossfadeEnabled());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public final void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
		super.onRequestPermissionsResult(requestCode, permissions, grantResults);
		// check if permissions are granted
		if (requestCode == REQ_CHECK_PERM && grantResults.length > 0) {
			for (int grantResult : grantResults) {
				if (grantResult == PackageManager.PERMISSION_DENIED) {
					Toast.makeText(getApplicationContext(), R.string.error_permission_denied, Toast.LENGTH_LONG).show();
					finish();
					return;
				}
			}
			// show battery optimization dialog
			ApolloUtils.openBatteryOptimizationDialog(this);
			// initialize subclass
			initialize();
			// Bind Apollo's service
			MusicUtils.bindToService(this, this);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean onCreateOptionsMenu(@NonNull Menu menu) {
		// Search view
		getMenuInflater().inflate(R.menu.search, menu);
		// Settings
		getMenuInflater().inflate(R.menu.activity_base, menu);
		SearchView searchView = (SearchView) menu.findItem(R.id.menu_search).getActionView();
		if (searchView != null) {
			// Add voice search
			SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
			SearchableInfo searchableInfo = searchManager.getSearchableInfo(getComponentName());
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
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == R.id.menu_settings) {
			NavUtils.openSettings(this);
			return true;
		} else if (item.getItemId() == R.id.menu_close) {
			NavUtils.closeApp(this);
			return true;
		}
		return false;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onClick(View v) {
		// album art clicked
		if (v.getId() == R.id.bottom_action_bar_album_art) {
			Album album = MusicUtils.getCurrentAlbum(this);
			if (album != null) {
				NavUtils.openAlbumProfile(this, album);
			} else {
				MusicUtils.shuffleAll(this);
			}
		}
		// background clicked (right side of album art)
		else if (v.getId() == R.id.bottom_action_bar_background) {
			Song song = MusicUtils.getCurrentTrack(this);
			if (song != null) {
				NavUtils.openAudioPlayer(this);
			} else {
				MusicUtils.shuffleAll(this);
			}
		}
		// repeat button clicked
		else if (v.getId() == R.id.action_button_repeat) {
			mRepeatButton.updateButtonState(MusicUtils.cycleRepeat(this));
		}
		// shuffle button clicked
		else if (v.getId() == R.id.action_button_shuffle) {
			mShuffleButton.updateButtonState(MusicUtils.cycleShuffle(this));
		}
		// play button clicked
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


	@Override
	public boolean onQueryTextSubmit(String query) {
		// Open the search activity
		NavUtils.openSearch(this, query);
		return true;
	}


	@Override
	public boolean onQueryTextChange(String newText) {
		// Nothing to do
		return false;
	}


	@Override
	public final void onMetaChange() {
		// Current info
		updateBottomActionBarInfo();
		// Update the favorites icon
		invalidateOptionsMenu();
		onMetaChanged();
	}


	@Override
	public final void onStateChange() {
		// Set the play and pause image
		mPlayPauseButton.updateState(MusicUtils.isPlaying(this));
	}


	@Override
	public final void onModeChange() {
		// Set the repeat image
		mRepeatButton.updateButtonState(MusicUtils.getRepeatMode(this));
		// Set the shuffle image
		mShuffleButton.updateButtonState(MusicUtils.getShuffleMode(this));
	}


	@Override
	public final void refresh() {
		onRefresh();
	}

	/**
	 * Sets the track name, album name, and album art.
	 */
	private void updateBottomActionBarInfo() {
		Song song = MusicUtils.getCurrentTrack(this);
		Album album = MusicUtils.getCurrentAlbum(this);
		// set current track information
		if (song != null) {
			mTrackName.setText(song.getName());
			mArtistName.setText(song.getArtist());
		} else {
			mTrackName.setText("");
			mArtistName.setText("");
		}
		// Set the album art
		if (album != null) {
			imageFetcher.loadAlbumImage(album.getId(), mAlbumArt);
		} else {
			mAlbumArt.setImageResource(0);
		}
	}

	/**
	 * Sets the correct drawable states for the playback controls.
	 */
	private void updatePlaybackControls() {
		// Set the play and pause image
		mPlayPauseButton.updateState(MusicUtils.isPlaying(this));
		// Set the shuffle image
		mShuffleButton.updateButtonState(MusicUtils.getShuffleMode(this));
		// Set the repeat image
		mRepeatButton.updateButtonState(MusicUtils.getRepeatMode(this));
	}

	/**
	 * get content view to use
	 *
	 * @return layout resource ID
	 */
	protected abstract int getContentView();

	/**
	 * initialize activity
	 */
	protected abstract void initialize();

	/**
	 * notify sub classes to reload information
	 */
	protected abstract void onRefresh();

	/**
	 * notify sub classes that meta information changed
	 */
	protected abstract void onMetaChanged();
}