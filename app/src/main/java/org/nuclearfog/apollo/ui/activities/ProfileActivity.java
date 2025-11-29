package org.nuclearfog.apollo.ui.activities;

import android.app.SearchManager;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.provider.MediaStore.Audio;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.ViewModelProvider;
import androidx.viewpager.widget.ViewPager;
import androidx.viewpager.widget.ViewPager.OnPageChangeListener;

import org.nuclearfog.apollo.R;
import org.nuclearfog.apollo.async.AsyncExecutor.AsyncCallback;
import org.nuclearfog.apollo.async.loader.AlbumSongLoader;
import org.nuclearfog.apollo.async.loader.ArtistSongLoader;
import org.nuclearfog.apollo.async.loader.FavoriteSongLoader;
import org.nuclearfog.apollo.async.loader.FolderSongLoader;
import org.nuclearfog.apollo.async.loader.GenreSongLoader;
import org.nuclearfog.apollo.async.loader.LastAddedLoader;
import org.nuclearfog.apollo.async.loader.PlaylistSongLoader;
import org.nuclearfog.apollo.async.loader.PopularSongLoader;
import org.nuclearfog.apollo.async.worker.ArtworkDownloader;
import org.nuclearfog.apollo.cache.ImageFetcher;
import org.nuclearfog.apollo.model.Playlist;
import org.nuclearfog.apollo.model.Song;
import org.nuclearfog.apollo.store.PopularStore;
import org.nuclearfog.apollo.ui.adapters.viewpager.ProfileAdapter;
import org.nuclearfog.apollo.ui.dialogs.ImageSelectorDialog;
import org.nuclearfog.apollo.ui.dialogs.ImageSelectorDialog.OnItemSelectedListener;
import org.nuclearfog.apollo.ui.dialogs.PhotoSelectionDialog;
import org.nuclearfog.apollo.ui.dialogs.PhotoSelectionDialog.OnOptionSelectedListener;
import org.nuclearfog.apollo.ui.fragments.profile.LastAddedSongFragment;
import org.nuclearfog.apollo.ui.fragments.profile.ProfileFragment;
import org.nuclearfog.apollo.ui.views.ProfileTabCarousel;
import org.nuclearfog.apollo.ui.views.ProfileTabCarousel.OnTabChangeListener;
import org.nuclearfog.apollo.utils.ApolloUtils;
import org.nuclearfog.apollo.utils.Constants;
import org.nuclearfog.apollo.utils.Constants.ImageType;
import org.nuclearfog.apollo.utils.FragmentViewModel;
import org.nuclearfog.apollo.utils.MusicUtils;
import org.nuclearfog.apollo.utils.NavUtils;
import org.nuclearfog.apollo.utils.PreferenceUtils;
import org.nuclearfog.apollo.utils.SortOrder;
import org.nuclearfog.apollo.utils.ThemeUtils;

import java.util.List;

/**
 * This activity shows detailed information to albums, artists, playlists and more using the {@link ProfileFragment}
 *
 * @author Andrew Neal (andrewdneal@gmail.com)
 * @author nuclearfog
 * @see ProfileFragment
 */
public class ProfileActivity extends ActivityBase implements AsyncCallback<Bitmap>,
		OnPageChangeListener, OnItemSelectedListener, OnTabChangeListener, OnOptionSelectedListener {

	private static final String TAG = "ProfileActivity";

	/**
	 * mime type of the {@link org.nuclearfog.apollo.ui.fragments.profile.FolderSongFragment}
	 */
	public static final String PAGE_FOLDERS = "page_folders";
	/**
	 * mime type of the {@link org.nuclearfog.apollo.ui.fragments.profile.FavoriteSongFragment}
	 */
	public static final String PAGE_FAVORITES = "page_fav";
	/**
	 * mime type of the {@link LastAddedSongFragment}
	 */
	public static final String PAGE_LAST_ADDED = "last_added";
	/**
	 * mime type of the {@link LastAddedSongFragment}
	 */
	public static final String PAGE_MOST_PLAYED = "page_most";

	private ActivityResultLauncher<PickVisualMediaRequest> imagePickerLauncher = registerForActivityResult(new ActivityResultContracts.PickVisualMedia(), this::onImageSelected);


	private AsyncCallback<List<Song>> onShuffleSongs = this::onShuffleSongs;

	/**
	 * View pager
	 */
	private ViewPager mViewPager;

	/**
	 * Profile header carousel
	 */
	private ProfileTabCarousel mTabCarousel;

	/**
	 * content type to show on this activity
	 */
	private Type type;

	/**
	 * ID used for albums, artist genres etc
	 */
	private long[] ids = {0};

	/**
	 * MIME type of the profile
	 */
	private String mType = "";

	/**
	 * release year of an album
	 */
	private String year = "";

	/**
	 * Artist name of an album
	 */
	private String mArtistName = "";

	/**
	 * The main profile title
	 */
	private String mProfileName = "";

	/**
	 * folder path of the music files
	 */
	private String folderPath = "";

	/**
	 * Image cache
	 */
	private ImageFetcher mImageFetcher;
	private ArtistSongLoader artistSongLoader;
	private AlbumSongLoader albumSongLoader;
	private GenreSongLoader genreSongLoader;
	private PlaylistSongLoader playlistSongLoader;
	private FavoriteSongLoader favoriteSongLoader;
	private LastAddedLoader lastAddedLoader;
	private PopularSongLoader popularSongLoader;
	private FolderSongLoader folderSongLoader;

	private PreferenceUtils mPreferences;

	private FragmentViewModel viewModel;

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected int getContentView() {
		return R.layout.activity_profile_base;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void initialize() {
		Toolbar toolbar = findViewById(R.id.activity_profile_base_toolbar);
		mTabCarousel = findViewById(R.id.activity_profile_base_tab_carousel);
		mViewPager = findViewById(R.id.activity_profile_base_pager);
		// init fragment callback
		viewModel = new ViewModelProvider(this).get(FragmentViewModel.class);
		// Get the preferences
		mPreferences = PreferenceUtils.getInstance(this);
		// Initialize the image fetcher
		mImageFetcher = new ImageFetcher(this);
		artistSongLoader = new ArtistSongLoader(this);
		albumSongLoader = new AlbumSongLoader(this);
		genreSongLoader = new GenreSongLoader(this);
		playlistSongLoader = new PlaylistSongLoader(this);
		favoriteSongLoader = new FavoriteSongLoader(this);
		lastAddedLoader = new LastAddedLoader(this);
		popularSongLoader = new PopularSongLoader(this);
		folderSongLoader = new FolderSongLoader(this);
		// set toolbar
		setSupportActionBar(toolbar);
		// Get the MIME type
		if (getIntent().getExtras() != null) {
			// Get the ID
			if (getIntent().getExtras().containsKey(Constants.IDS)) {
				ids = ApolloUtils.readSerializedIDs(getIntent().getExtras().getString(Constants.IDS, ""));
			} else {
				ids = new long[]{getIntent().getExtras().getLong(Constants.ID)};
			}
			// get mime type
			mType = getIntent().getExtras().getString(Constants.MIME_TYPE, "");
			// Get the profile title
			mProfileName = getIntent().getExtras().getString(Constants.NAME, "");
			// Get the artist name
			mArtistName = getIntent().getExtras().getString(Constants.ARTIST_NAME, "");
			// get album yeas
			year = getIntent().getExtras().getString(Constants.ALBUM_YEAR, "");
			// get folder name if defined
			folderPath = getIntent().getExtras().getString(Constants.FOLDER, "");
		}
		type = Type.fromString(mType);
		initViews();
		// Initialize the pager adapter
		ProfileAdapter mPagerAdapter = new ProfileAdapter(this, getIntent().getExtras(), type);
		// Attach the adapter
		mViewPager.setAdapter(mPagerAdapter);
		// Offscreen limit
		mViewPager.setOffscreenPageLimit(mPagerAdapter.getCount());
		// Attach the page change listener
		mViewPager.addOnPageChangeListener(this);
		// Attach the carousel listener
		mTabCarousel.setListener(this);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void onPause() {
		super.onPause();
		mImageFetcher.clear();
	}

	/**
	 * {@inheritDoc}
	 */
	@SuppressWarnings("GestureBackNavigation,deprecation")
	@Override
	public void onBackPressed() {
		if (mTabCarousel.getCurrentTab() == 0) {
			super.onBackPressed();
		} else {
			mTabCarousel.reset();
			mViewPager.setCurrentItem(0);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void onDestroy() {
		artistSongLoader.cancel();
		albumSongLoader.cancel();
		genreSongLoader.cancel();
		playlistSongLoader.cancel();
		favoriteSongLoader.cancel();
		lastAddedLoader.cancel();
		popularSongLoader.cancel();
		folderSongLoader.cancel();
		super.onDestroy();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		// Theme the add to home screen icon
		MenuItem shuffle = menu.findItem(R.id.menu_shuffle);
		if (type == Type.FAVORITE || type == Type.LAST_ADDED || type == Type.PLAYLIST || type == Type.POPULAR || type == Type.FOLDER) {
			shuffle.setTitle(R.string.menu_play_all);
		} else {
			shuffle.setTitle(R.string.menu_shuffle);
		}
		return super.onPrepareOptionsMenu(menu);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean onCreateOptionsMenu(@NonNull Menu menu) {
		// Pin to Home screen
		getMenuInflater().inflate(R.menu.add_to_homescreen, menu);
		// Shuffle
		getMenuInflater().inflate(R.menu.shuffle, menu);
		// Sort orders
		if (type == Type.ARTIST) {
			if (!ApolloUtils.isLandscape(this)) {
				if (mViewPager.getCurrentItem() == ProfileAdapter.IDX_ARTIST_SONG) {
					getMenuInflater().inflate(R.menu.artist_song_sort_by, menu);
				} else if (mViewPager.getCurrentItem() == ProfileAdapter.IDX_ARTIST_ALBUM) {
					getMenuInflater().inflate(R.menu.artist_album_sort_by, menu);
				}
			}
		} else if (type == Type.ALBUM) {
			getMenuInflater().inflate(R.menu.album_song_sort_by, menu);
		} else if (type == Type.POPULAR) {
			getMenuInflater().inflate(R.menu.popular_songs_clear, menu);
		} else if (type == Type.FOLDER) {
			getMenuInflater().inflate(R.menu.folder_songs_sort_by, menu);
		}
		return true;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// return to home
		if (item.getItemId() == android.R.id.home) {
			// If an album profile, go up to the artist profile
			if (type == Type.ALBUM) {
				NavUtils.openArtistProfile(this, mArtistName);
				finish();
			} else {
				// Otherwise just go back
				finish();
			}
		}
		// add item to home screen
		else if (item.getItemId() == R.id.menu_add_to_homescreen) {
			// Place the artist, album, genre, or playlist onto the Home
			// screen. Definitely one of my favorite features.
			if (type == Type.ARTIST) {
				ApolloUtils.createShortcutIntent(mArtistName, mArtistName, mType, this, ids);
			} else if (type == Type.FOLDER) {
				ApolloUtils.createShortcutIntent(folderPath, mArtistName, mType, this, ids);
			} else {
				ApolloUtils.createShortcutIntent(mProfileName, mArtistName, mType, this, ids);
			}
		}
		// shuffle tracks
		else if (item.getItemId() == R.id.menu_shuffle) {
			switch (type) {
				case ARTIST:
					artistSongLoader.execute(ids[0], onShuffleSongs);
					break;

				case ALBUM:
					albumSongLoader.execute(ids[0], onShuffleSongs);
					break;

				case GENRE:
					String genreIds = ApolloUtils.serializeIDs(ids);
					genreSongLoader.execute(genreIds, onShuffleSongs);
					break;

				case PLAYLIST:
					playlistSongLoader.execute(ids[0], onShuffleSongs);
					break;

				case FAVORITE:
					favoriteSongLoader.execute(null, onShuffleSongs);
					break;

				case LAST_ADDED:
					lastAddedLoader.execute(null, onShuffleSongs);
					break;

				case POPULAR:
					popularSongLoader.execute(null, onShuffleSongs);
					break;

				case FOLDER:
					folderSongLoader.execute(folderPath, onShuffleSongs);
					break;
			}
		}
		// sort alphabetical
		else if (item.getItemId() == R.id.menu_sort_by_az) {
			switch (type) {
				case ARTIST:
					if (mViewPager.getCurrentItem() == ProfileAdapter.IDX_ARTIST_SONG) {
						mPreferences.setArtistSongSortOrder(SortOrder.ArtistSongSortOrder.SONG_A_Z);
					} else if (mViewPager.getCurrentItem() == ProfileAdapter.IDX_ARTIST_ALBUM) {
						mPreferences.setArtistAlbumSortOrder(SortOrder.ArtistAlbumSortOrder.ALBUM_A_Z);
					}
					break;

				case ALBUM:
					mPreferences.setAlbumSongSortOrder(SortOrder.AlbumSongSortOrder.SONG_Z_A);
					break;

				case FOLDER:
					mPreferences.setFolderSongSortOrder(SortOrder.FolderSongSortOrder.SONG_A_Z);
					break;
			}
			viewModel.notify(ProfileFragment.REFRESH);
		}
		// sort alphabetical reverse
		else if (item.getItemId() == R.id.menu_sort_by_za) {
			switch (type) {
				case ARTIST:
					if (mViewPager.getCurrentItem() == ProfileAdapter.IDX_ARTIST_SONG) {
						mPreferences.setArtistSongSortOrder(SortOrder.ArtistSongSortOrder.SONG_Z_A);
					} else if (mViewPager.getCurrentItem() == ProfileAdapter.IDX_ARTIST_ALBUM) {
						mPreferences.setArtistAlbumSortOrder(SortOrder.ArtistAlbumSortOrder.ALBUM_Z_A);
					}
					break;

				case ALBUM:
					mPreferences.setAlbumSongSortOrder(SortOrder.AlbumSongSortOrder.SONG_Z_A);
					break;

				case FOLDER:
					mPreferences.setFolderSongSortOrder(SortOrder.FolderSongSortOrder.SONG_Z_A);
					break;
			}
			viewModel.notify(ProfileFragment.REFRESH);
		}
		// sort by album name
		else if (item.getItemId() == R.id.menu_sort_by_album) {
			mPreferences.setArtistSongSortOrder(SortOrder.ArtistSongSortOrder.SONG_ALBUM);
			viewModel.notify(ProfileFragment.REFRESH);
		}
		// sort by release date
		else if (item.getItemId() == R.id.menu_sort_by_year) {
			if (mViewPager.getCurrentItem() == ProfileAdapter.IDX_ARTIST_SONG) {
				mPreferences.setArtistSongSortOrder(SortOrder.ArtistSongSortOrder.SONG_YEAR);
			} else if (mViewPager.getCurrentItem() == ProfileAdapter.IDX_ARTIST_ALBUM) {
				mPreferences.setArtistAlbumSortOrder(SortOrder.ArtistAlbumSortOrder.ALBUM_YEAR);
			}
			viewModel.notify(ProfileFragment.REFRESH);
		}
		// sort by track duration
		else if (item.getItemId() == R.id.menu_sort_by_duration) {
			if (type == Type.ARTIST) {
				mPreferences.setArtistSongSortOrder(SortOrder.ArtistSongSortOrder.SONG_DURATION);
			} else if (type == Type.ALBUM) {
				mPreferences.setAlbumSongSortOrder(SortOrder.AlbumSongSortOrder.SONG_DURATION);
			} else if (type == Type.FOLDER) {
				mPreferences.setFolderSongSortOrder(SortOrder.FolderSongSortOrder.SONG_DURATION);
			}
			viewModel.notify(ProfileFragment.REFRESH);
		}
		// sort by date added
		else if (item.getItemId() == R.id.menu_sort_by_date_added) {
			mPreferences.setArtistSongSortOrder(SortOrder.ArtistSongSortOrder.SONG_DATE);
			viewModel.notify(ProfileFragment.REFRESH);
		}
		// sort by default order
		else if (item.getItemId() == R.id.menu_sort_by_track_list) {
			if (type == Type.FOLDER) {
				mPreferences.setFolderSongSortOrder(SortOrder.FolderSongSortOrder.SONG_TRACK_LIST);
			} else {
				mPreferences.setAlbumSongSortOrder(SortOrder.AlbumSongSortOrder.SONG_TRACK_LIST);
			}
			viewModel.notify(ProfileFragment.REFRESH);
		}
		// sort by file name
		else if (item.getItemId() == R.id.menu_sort_by_filename) {
			if (type == Type.ARTIST) {
				mPreferences.setArtistSongSortOrder(SortOrder.ArtistSongSortOrder.SONG_FILENAME);
			} else if (type == Type.ALBUM) {
				mPreferences.setAlbumSongSortOrder(SortOrder.AlbumSongSortOrder.SONG_FILENAME);
			} else if (type == Type.FOLDER) {
				mPreferences.setFolderSongSortOrder(SortOrder.FolderSongSortOrder.SONG_FILENAME);
			}
			viewModel.notify(ProfileFragment.REFRESH);
		}
		// sort by track count
		else if (item.getItemId() == R.id.menu_sort_by_number_of_songs) {
			if (type == Type.ARTIST) {
				mPreferences.setArtistAlbumSortOrder(SortOrder.ArtistAlbumSortOrder.ALBUM_TRACK_COUNT);
				viewModel.notify(ProfileFragment.REFRESH);
			}
		}
		// clear popular playlist
		else if (item.getItemId() == R.id.menu_clear_popular) {
			PopularStore.getInstance(this).removeAll();
			viewModel.notify(ProfileFragment.REFRESH);
		} else {
			return super.onOptionsItemSelected(item);
		}
		return true;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent intent) {
		super.onActivityResult(requestCode, resultCode, intent);
		if (requestCode == MusicUtils.REQUEST_DELETE_FILES && resultCode == RESULT_OK) {
			MusicUtils.refresh(this);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onItemSelected(String mbid) {
		ArtworkDownloader loaderAsync = new ArtworkDownloader(this);
		if (type == Type.ALBUM) {
			ArtworkDownloader.Param param = new ArtworkDownloader.Param(ImageType.ALBUM, ids[0], mbid);
			loaderAsync.execute(param, this);
		} else if (type == Type.ARTIST) {
			ArtworkDownloader.Param param = new ArtworkDownloader.Param(ImageType.ARTIST, ids[0], mbid);
			loaderAsync.execute(param, this);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
		if (!mViewPager.isFakeDragging()) {
			int scrollToX = (int) ((position + positionOffset) * mTabCarousel.getAllowedHorizontalScrollLength());
			mTabCarousel.scrollTo(scrollToX, 0);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onPageSelected(int position) {
		mTabCarousel.setCurrentTab(position);
		if (type == Type.ARTIST) {
			viewModel.notify(ProfileFragment.SCROLL_TOP);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onPageScrollStateChanged(int state) {
		if (state == ViewPager.SCROLL_STATE_IDLE) {
			mTabCarousel.restoreYCoordinate(75, mViewPager.getCurrentItem());
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onTouchDown() {
		mViewPager.beginFakeDrag();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onTouchUp() {
		if (mViewPager.isFakeDragging()) {
			mViewPager.endFakeDrag();
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onScrollChanged(int l, int oldL) {
		if (mViewPager.isFakeDragging()) {
			mViewPager.fakeDragBy(oldL - l);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onTabSelected(int position) {
		mViewPager.setCurrentItem(position);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onArtworkSelected() {
		if (type == Type.ARTIST) {
			PhotoSelectionDialog.show(getSupportFragmentManager(), mArtistName, PhotoSelectionDialog.ARTIST);
		} else if (type == Type.ALBUM) {
			PhotoSelectionDialog.show(getSupportFragmentManager(), mProfileName, PhotoSelectionDialog.ALBUM);
		} else {
			PhotoSelectionDialog.show(getSupportFragmentManager(), mProfileName, PhotoSelectionDialog.OTHER);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void onRefresh() {
		viewModel.notify(ProfileFragment.REFRESH);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void onMetaChanged() {
		viewModel.notify(ProfileFragment.SHOW_CURRENT);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onResult(@NonNull Bitmap bitmap) {
		mTabCarousel.setAlbumArt(bitmap);
	}

	/**
	 * Starts an activity for result that returns an image from the Gallery.
	 */
	@Override
	public void selectNewPhoto() {
		// First remove the old image
		removeFromCache();
		// open system image selector
		imagePickerLauncher.launch(new PickVisualMediaRequest.Builder()
				.setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE).build());
	}

	/**
	 * Fetch for the artist or album art, otherwise sets the default header image.
	 */
	@Override
	public void selectOldPhoto() {
		// First remove the old image
		removeFromCache();
		// Apply the old photo
		if (type == Type.ARTIST) {
			mTabCarousel.setArtistProfileHeader(ids[0]);
		} else if (type == Type.ALBUM) {
			mTabCarousel.setAlbumProfileHeader(ids[0]);
		} else {
			mTabCarousel.setDefault();
		}
	}

	/**
	 * When the user chooses {@code #fetchArtwork()} while viewing an album
	 * profile, the image is, most likely, reverted back to the locally found
	 * artwork. This is specifically for fetching the image from MusicBrainz.
	 */
	@Override
	public void fetchArtwork() {
		ImageSelectorDialog.open(getSupportFragmentManager(), mProfileName, mArtistName);
	}

	/**
	 * Searches for the artist or album
	 */
	@Override
	public void searchWeb() {
		String query;
		if (type == Type.ARTIST) {
			query = mArtistName;
		} else if (type == Type.ALBUM) {
			query = mProfileName + " " + mArtistName;
		} else {
			query = mProfileName;
		}
		Intent webSearch = new Intent(Intent.ACTION_WEB_SEARCH);
		webSearch.putExtra(SearchManager.QUERY, query);
		try {
			startActivity(webSearch);
		} catch (ActivityNotFoundException e) {
			Log.w(TAG, "couldn't open browser!");
		}
	}

	/**
	 * callback used to set selected image
	 *
	 * @param imageUri local image path or null if no image is selected
	 */
	private void onImageSelected(@Nullable Uri imageUri) {
		if (imageUri != null) {
			mTabCarousel.setAlbumArt(imageUri);
			setProfileImage(imageUri);
		}
	}

	/**
	 * initialize fragment views and toolbar
	 */
	private void initViews() {
		ActionBar actionBar = getSupportActionBar();
		boolean displayHome = false;
		String actionbarTitle = mProfileName;
		String actionbarSubTitle = "";

		switch (type) {
			case ALBUM:
				mTabCarousel.setAlbumProfileHeader(ids[0]);
				actionbarSubTitle = year;
				break;

			case ARTIST:
				mTabCarousel.setArtistProfileHeader(ids[0]);
				actionbarTitle = mArtistName;
				displayHome = true;
				break;

			case FOLDER:
				mTabCarousel.setFolderProfileHeader(folderPath);
				break;

			case GENRE:
				mTabCarousel.setGenreProfileHeader(ids);
				break;

			case PLAYLIST:
				mTabCarousel.setPlaylistProfileHeader(ids[0]);
				break;

			case FAVORITE:
				mTabCarousel.setPlaylistProfileHeader(Playlist.FAVORITE_ID);
				break;

			case LAST_ADDED:
				mTabCarousel.setPlaylistProfileHeader(Playlist.LAST_ADDED_ID);
				break;

			case POPULAR:
				mTabCarousel.setPlaylistProfileHeader(Playlist.POPULAR_ID);
				break;
		}
		// disable carousel view on landscape mode
		if (ApolloUtils.isLandscape(this)) {
			mTabCarousel.setVisibility(View.GONE);
		}
		if (actionBar != null) {
			ThemeUtils themeUtils = new ThemeUtils(this);
			themeUtils.themeActionBar(actionBar, actionbarTitle);
			if (!actionbarSubTitle.isEmpty())
				themeUtils.setSubtitle(actionBar, actionbarSubTitle);
			actionBar.setDisplayHomeAsUpEnabled(displayHome);
		}
	}

	/**
	 * Removes the header image from the cache.
	 */
	private void removeFromCache() {
		setProfileImage(null);
	}

	/**
	 * set image for the current profile
	 *
	 * @param uri local uri of the image or null to remove the current image
	 */
	private void setProfileImage(@Nullable Uri uri) {
		switch (type) {
			case ARTIST:
				mImageFetcher.setArtistImage(ids[0], uri);
				break;

			case ALBUM:
				mImageFetcher.setAlbumImage(ids[0], uri);
				break;

			case FOLDER:
				mImageFetcher.setFolderImage(folderPath, uri);
				break;

			case GENRE:
				mImageFetcher.setGenreImage(ids, uri);
				break;

			case PLAYLIST:
				mImageFetcher.setPlaylistImage(ids[0], uri);
				break;

			case FAVORITE:
				mImageFetcher.setPlaylistImage(Playlist.FAVORITE_ID, uri);
				break;

			case POPULAR:
				mImageFetcher.setPlaylistImage(Playlist.POPULAR_ID, uri);
				break;

			case LAST_ADDED:
				mImageFetcher.setPlaylistImage(Playlist.LAST_ADDED_ID, uri);
				break;
		}
	}

	/**
	 * called to play asynchronously loaded songs
	 */
	private void onShuffleSongs(List<Song> songs) {
		long[] ids = MusicUtils.getIDsFromSongList(songs);
		MusicUtils.playAll(this, ids, 0, true);
	}

	/**
	 * constants defining fragment type
	 */
	public enum Type {
		ARTIST,
		ALBUM,
		GENRE,
		PLAYLIST,
		FOLDER,
		FAVORITE,
		LAST_ADDED,
		POPULAR;

		@SuppressWarnings("deprecation")
		static Type fromString(String typeStr) {
			switch (typeStr) {
				case Audio.Artists.CONTENT_TYPE:
					return ARTIST;
				case Audio.Albums.CONTENT_TYPE:
					return ALBUM;
				case Audio.Genres.CONTENT_TYPE:
					return GENRE;
				case Audio.Playlists.CONTENT_TYPE:
					return PLAYLIST;
				case PAGE_FOLDERS:
					return FOLDER;
				case PAGE_FAVORITES:
					return FAVORITE;
				case PAGE_MOST_PLAYED:
					return POPULAR;
				case PAGE_LAST_ADDED:
				default:
					return LAST_ADDED;
			}
		}
	}
}