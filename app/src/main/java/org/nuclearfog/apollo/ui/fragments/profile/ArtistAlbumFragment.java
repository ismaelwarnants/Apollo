package org.nuclearfog.apollo.ui.fragments.profile;

import android.os.Bundle;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AdapterView.AdapterContextMenuInfo;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.nuclearfog.apollo.R;
import org.nuclearfog.apollo.async.AsyncExecutor.AsyncCallback;
import org.nuclearfog.apollo.async.loader.AlbumSongLoader;
import org.nuclearfog.apollo.async.loader.ArtistAlbumLoader;
import org.nuclearfog.apollo.model.Album;
import org.nuclearfog.apollo.model.Song;
import org.nuclearfog.apollo.ui.adapters.listview.ArtistAlbumAdapter;
import org.nuclearfog.apollo.ui.dialogs.PlaylistDialog;
import org.nuclearfog.apollo.ui.views.dragdrop.VerticalScrollController.ScrollableHeader;
import org.nuclearfog.apollo.utils.Constants;
import org.nuclearfog.apollo.utils.ContextMenuItems;
import org.nuclearfog.apollo.utils.MusicUtils;
import org.nuclearfog.apollo.utils.NavUtils;

import java.util.List;

/**
 * This class is used to display all of the albums from a particular artist.
 *
 * @author Andrew Neal (andrewdneal@gmail.com)
 * @author nuclearfog
 */
public class ArtistAlbumFragment extends ProfileFragment implements AsyncCallback<List<Album>>, ScrollableHeader {

	/**
	 * Used to keep context menu items from bleeding into other fragments
	 */
	private static final int GROUP_ID = 0x6CEDC429;

	private AsyncCallback<List<Song>> onPlaySongs = this::onPlaySongs;
	private AsyncCallback<List<Song>> onAddToQueue = this::onAddToQueue;
	private AsyncCallback<List<Song>> onAddToNewPlaylist = this::onAddToNewPlaylist;
	private AsyncCallback<List<Song>> onAddToExistingPlaylist = this::onAddToExistingPlaylist;
	private AsyncCallback<List<Song>> onSongsDelete = this::onSongsDelete;

	/**
	 * The adapter for the grid
	 */
	private ArtistAlbumAdapter mAdapter;
	private ArtistAlbumLoader artistAlbumLoader;
	private AlbumSongLoader albumSongLoader;

	/**
	 * context menu selection
	 */
	@Nullable
	private Album mAlbum;
	private long artistId;
	private long selectedPlaylistId;


	@Override
	protected void init(Bundle param) {
		// init loader
		artistAlbumLoader = new ArtistAlbumLoader(requireContext());
		albumSongLoader = new AlbumSongLoader(requireContext());
		// set adapter
		mAdapter = new ArtistAlbumAdapter(requireContext());
		// sets empty list text
		setEmptyText(R.string.empty_artist_albums);
		// set adapter
		setAdapter(mAdapter);
		// Start the loader
		if (param != null) {
			artistId = param.getLong(Constants.ID);
			artistAlbumLoader.execute(artistId, this);
		}
	}


	@Override
	public void onDestroy() {
		artistAlbumLoader.cancel();
		albumSongLoader.cancel();
		super.onDestroy();
	}


	@Override
	protected void onItemClick(View v, int pos, long id) {
		if (v.getId() == R.id.image) {
			// Album art was clicked
			albumSongLoader.execute(id, onPlaySongs);
		} else {
			// open Album
			Album album = mAdapter.getItem(pos);
			if (album != null) {
				NavUtils.openAlbumProfile(requireActivity(), album);
				requireActivity().finish();
			}
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onPause() {
		super.onPause();
		mAdapter.flush();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onCreateContextMenu(@NonNull ContextMenu menu, @NonNull View v, ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		if (menuInfo instanceof AdapterContextMenuInfo) {
			// Get the position of the selected item
			AdapterContextMenuInfo info = (AdapterContextMenuInfo) menuInfo;
			// Create a new album
			mAlbum = mAdapter.getItem(info.position);
			// Create a list of the album's songs
			if (mAlbum != null) {
				// Play the album
				menu.add(GROUP_ID, ContextMenuItems.PLAY_SELECTION, Menu.NONE, R.string.context_menu_play_selection);
				// Add the album to the queue
				menu.add(GROUP_ID, ContextMenuItems.ADD_TO_QUEUE, Menu.NONE, R.string.add_to_queue);
				// Add the album to a playlist
				SubMenu subMenu = menu.addSubMenu(GROUP_ID, ContextMenuItems.ADD_TO_PLAYLIST, Menu.NONE, R.string.add_to_playlist);
				MusicUtils.makePlaylistMenu(requireContext(), GROUP_ID, subMenu, false);
				// Delete the album
				menu.add(GROUP_ID, ContextMenuItems.DELETE, Menu.NONE, R.string.context_menu_delete);
			}
		} else {
			mAlbum = null;
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean onContextItemSelected(@NonNull MenuItem item) {
		// Avoid leaking context menu selections
		if (item.getGroupId() == GROUP_ID && mAlbum != null) {

			switch (item.getItemId()) {
				case ContextMenuItems.PLAY_SELECTION:
					albumSongLoader.execute(mAlbum.getId(), onPlaySongs);
					return true;

				case ContextMenuItems.ADD_TO_QUEUE:
					albumSongLoader.execute(mAlbum.getId(), onAddToQueue);
					return true;

				case ContextMenuItems.NEW_PLAYLIST:
					albumSongLoader.execute(mAlbum.getId(), onAddToNewPlaylist);
					return true;

				case ContextMenuItems.PLAYLIST_SELECTED:
					if (item.getIntent() != null) {
						selectedPlaylistId = item.getIntent().getLongExtra(Constants.PLAYLIST_ID, -1L);
						if (selectedPlaylistId != -1) {
							albumSongLoader.execute(mAlbum.getId(), onAddToExistingPlaylist);
						}
					}
					return true;

				case ContextMenuItems.DELETE:
					albumSongLoader.execute(mAlbum.getId(), onSongsDelete);
					return true;
			}
		}
		return false;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onResult(@NonNull List<Album> albums) {
		if (isAdded()) {
			// Start fresh
			mAdapter.clear();
			// Add the data to the adapter
			for (Album album : albums) {
				mAdapter.add(album);
			}
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void moveToCurrent() {
		// not used
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void refresh() {
		mAdapter.clear();
		artistAlbumLoader.execute(artistId, this);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onScrollStateChanged(int scrollState) {
		boolean pauseCache = scrollState == AbsListView.OnScrollListener.SCROLL_STATE_FLING
				|| scrollState == AbsListView.OnScrollListener.SCROLL_STATE_TOUCH_SCROLL;
		mAdapter.setPauseDiskCache(pauseCache);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void drop(int from, int to) {
		// not used
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void remove(int index) {
		// not used
	}

	/**
	 * play loaded songs
	 */
	private void onPlaySongs(List<Song> songs) {
		MusicUtils.playAll(requireActivity(), songs, false);
	}

	/**
	 * add loaded songs to queue
	 */
	private void onAddToQueue(List<Song> songs) {
		MusicUtils.addToQueue(requireActivity(), songs);
	}

	/**
	 * create a new playlist with the loaded songs
	 */
	private void onAddToNewPlaylist(List<Song> songs) {
		PlaylistDialog.show(getParentFragmentManager(), PlaylistDialog.CREATE, 0, songs, null);
	}

	/**
	 * save the loaded songs into an existing playlist
	 */
	private void onAddToExistingPlaylist(List<Song> songs) {
		MusicUtils.addToPlaylist(requireActivity(), selectedPlaylistId, songs);
	}

	/**
	 * delete the loaded songs
	 */
	private void onSongsDelete(List<Song> songs) {
		String name = mAlbum != null ? mAlbum.getName() : "";
		MusicUtils.openDeleteDialog(requireActivity(), name, songs);
	}
}