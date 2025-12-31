package org.nuclearfog.apollo.ui.activities;

import android.os.Bundle;
import android.provider.MediaStore;

import androidx.appcompat.app.AppCompatActivity;

import org.nuclearfog.apollo.async.AsyncExecutor.AsyncCallback;
import org.nuclearfog.apollo.async.loader.AlbumSongLoader;
import org.nuclearfog.apollo.async.loader.ArtistSongLoader;
import org.nuclearfog.apollo.async.loader.FavoriteSongLoader;
import org.nuclearfog.apollo.async.loader.FolderSongLoader;
import org.nuclearfog.apollo.async.loader.GenreSongLoader;
import org.nuclearfog.apollo.async.loader.LastAddedLoader;
import org.nuclearfog.apollo.async.loader.PlaylistSongLoader;
import org.nuclearfog.apollo.async.loader.PopularSongLoader;
import org.nuclearfog.apollo.model.Song;
import org.nuclearfog.apollo.utils.ApolloUtils;
import org.nuclearfog.apollo.utils.Constants;
import org.nuclearfog.apollo.utils.MusicUtils;
import org.nuclearfog.apollo.utils.NavUtils;
import org.nuclearfog.apollo.utils.ServiceBinder.ServiceBinderCallback;

import java.util.List;

/**
 * This class is opened when the user touches a Home screen shortcut or album
 * art in an app-widget, and then carries out the proper action. It is also
 * responsible for processing voice queries and playing the spoken artist,
 * album, song, playlist, or genre.
 *
 * @author Andrew Neal (andrewdneal@gmail.com)
 * @author nuclearfog
 */
public class ShortcutActivity extends AppCompatActivity implements ServiceBinderCallback {

	/**
	 * If true, this class will begin playback and open
	 * {@link AudioPlayerActivity}, false will close the class after playback,
	 * which is what happens when a user starts playing something from an
	 * app-widget
	 */
	public static final String OPEN_AUDIO_PLAYER = null;

	private AsyncCallback<List<Song>> onPlaySongs = this::onPlaySongs;
	private AsyncCallback<List<Song>> onShuffleSongs = this::onShuffleSongs;

	private boolean shouldOpenAudioPlayer;

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// Initialize the intent
		shouldOpenAudioPlayer = getIntent().getBooleanExtra(OPEN_AUDIO_PLAYER, true);
		// go to home activity if there is any missing permission
		if (!ApolloUtils.permissionsGranted(this)) {
			NavUtils.goHome(this);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void onStart() {
		super.onStart();
		// bind activity to service
		MusicUtils.bindToService(this, this);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void onStop() {
		// Unbind from the service
		MusicUtils.unbindFromService(this);
		super.onStop();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	@SuppressWarnings("deprecation")
	public void onServiceConnected() {
		String requestedMimeType = getIntent().getStringExtra(Constants.MIME_TYPE);
		long id = getIntent().getLongExtra(Constants.ID, -1L);
		if (requestedMimeType == null) {
			return;
		}
		switch (requestedMimeType) {
			case MediaStore.Audio.Artists.CONTENT_TYPE:
				// Get the artist song list
				ArtistSongLoader artistSongLoader = new ArtistSongLoader(this);
				artistSongLoader.execute(id, onShuffleSongs);
				break;

			case MediaStore.Audio.Albums.CONTENT_TYPE:
				// Get the album song list
				AlbumSongLoader albumSongLoader = new AlbumSongLoader(this);
				albumSongLoader.execute(id, onShuffleSongs);
				break;

			case MediaStore.Audio.Genres.CONTENT_TYPE:
				// Get the genre song list
				String ids = getIntent().getStringExtra(Constants.IDS);
				GenreSongLoader genreSongLoader = new GenreSongLoader(this);
				genreSongLoader.execute(ids, onShuffleSongs);
				break;

			case MediaStore.Audio.Playlists.CONTENT_TYPE:
				// Get the playlist song list
				PlaylistSongLoader playlistLoader = new PlaylistSongLoader(this);
				playlistLoader.execute(id, onPlaySongs);
				break;

			case ProfileActivity.PAGE_FAVORITES:
				// Get the Favorites song list
				FavoriteSongLoader favoriteLoader = new FavoriteSongLoader(this);
				favoriteLoader.execute(null, onPlaySongs);
				break;

			case ProfileActivity.PAGE_MOST_PLAYED:
				// Get the popular song list
				PopularSongLoader popularLoader = new PopularSongLoader(this);
				popularLoader.execute(null, onPlaySongs);
				break;

			case ProfileActivity.PAGE_FOLDERS:
				// get folder path
				String folder = getIntent().getStringExtra(Constants.NAME);
				// Get folder song list
				FolderSongLoader folderSongLoader = new FolderSongLoader(this);
				folderSongLoader.execute(folder, onPlaySongs);
				break;

			case ProfileActivity.PAGE_LAST_ADDED:
				// Get the Last added song list
				LastAddedLoader lastAddedLoader = new LastAddedLoader(this);
				lastAddedLoader.execute(null, onPlaySongs);
				break;
		}
	}

	/**
	 * set song ID list after loading asynchronously
	 *
	 * @param songs list of songs
	 */
	private void onPlaySongs(List<Song> songs) {
		MusicUtils.playAll(this, songs, false);
		redirectToPlayerActivity();
	}

	/**
	 * set song ID list after loading asynchronously
	 *
	 * @param songs list of songs
	 */
	private void onShuffleSongs(List<Song> songs) {
		MusicUtils.playAll(this, songs, true);
		redirectToPlayerActivity();
	}

	/**
	 * open {@link AudioPlayerActivity} and close this activity
	 */
	private void redirectToPlayerActivity() {
		if (shouldOpenAudioPlayer) {
			NavUtils.openAudioPlayer(this);
		}
		finish();
	}
}