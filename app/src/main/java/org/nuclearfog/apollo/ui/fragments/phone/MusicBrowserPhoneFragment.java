package org.nuclearfog.apollo.ui.fragments.phone;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.MenuProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.viewpager.widget.ViewPager;
import androidx.viewpager.widget.ViewPager.OnPageChangeListener;

import org.nuclearfog.apollo.R;
import org.nuclearfog.apollo.model.Song;
import org.nuclearfog.apollo.store.FavoritesStore;
import org.nuclearfog.apollo.store.preferences.AppPreferences;
import org.nuclearfog.apollo.ui.adapters.viewpager.MusicBrowserAdapter;
import org.nuclearfog.apollo.ui.views.TitlePageIndicator;
import org.nuclearfog.apollo.ui.views.TitlePageIndicator.OnCenterItemClickListener;
import org.nuclearfog.apollo.utils.ApolloUtils;
import org.nuclearfog.apollo.utils.FragmentViewModel;
import org.nuclearfog.apollo.utils.MusicUtils;
import org.nuclearfog.apollo.utils.SortOrder;
import org.nuclearfog.apollo.utils.ThemeUtils;

/**
 * This class is used to hold the {@link ViewPager} used for swiping between the
 * playlists, recent, artists, albums, songs, and genre {@link Fragment} for phones.
 *
 * @author Andrew Neal (andrewdneal@gmail.com)
 * @author nuclearfog
 */
public class MusicBrowserPhoneFragment extends Fragment implements OnPageChangeListener, OnCenterItemClickListener, MenuProvider {

	/**
	 *
	 */
	public static final String TAG = "MusicBrowserPhoneFragment";

	/**
	 *
	 */
	public static final String REFRESH = TAG + ".refresh";

	/**
	 *
	 */
	public static final String META_CHANGED = TAG + ".meta_changed";

	/**
	 * Pager
	 */
	private ViewPager mViewPager;

	/**
	 * Theme resources
	 */
	private ThemeUtils mResources;

	/**
	 *
	 */
	private AppPreferences mPreferences;

	/**
	 * viewmodel used to communicate with sub fragments
	 */
	private FragmentViewModel viewModel;

	/**
	 * {@inheritDoc}
	 */
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.fragment_music_browser_phone, container, false);
		TitlePageIndicator pageIndicator = rootView.findViewById(R.id.fragment_home_phone_pager_titles);
		mViewPager = rootView.findViewById(R.id.fragment_home_phone_pager);

		MusicBrowserAdapter adapter = new MusicBrowserAdapter(this);
		mPreferences = AppPreferences.getInstance(requireContext());
		viewModel = new ViewModelProvider(requireActivity()).get(FragmentViewModel.class);
		mResources = new ThemeUtils(requireContext());

		mViewPager.setAdapter(adapter);
		mViewPager.setOffscreenPageLimit(adapter.getCount());
		mViewPager.setCurrentItem(mPreferences.getStartPage());
		pageIndicator.setViewPager(mViewPager);

		requireActivity().addMenuProvider(this);
		pageIndicator.setOnCenterItemClickListener(this);
		mViewPager.addOnPageChangeListener(this);
		return rootView;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onStop() {
		// Save the last page the use was on
		mPreferences.setStartPage(mViewPager.getCurrentItem());
		super.onStop();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
		// Favorite action
		inflater.inflate(R.menu.favorite, menu);
		// Shuffle all
		inflater.inflate(R.menu.shuffle, menu);
		// enable sort/view options
		if (!ApolloUtils.isLandscape(getContext())) {
			switch (mViewPager.getCurrentItem()) {
				case MusicBrowserAdapter.IDX_RECENT:
					inflater.inflate(R.menu.view_as, menu);
					break;

				case MusicBrowserAdapter.IDX_ARTIST:
					inflater.inflate(R.menu.artist_sort_by, menu);
					inflater.inflate(R.menu.view_as, menu);
					break;

				case MusicBrowserAdapter.IDX_TRACKS:
					inflater.inflate(R.menu.song_sort_by, menu);
					inflater.inflate(R.menu.view_as, menu);
					menu.findItem(R.id.menu_view_as_grid).setVisible(false);
					break;

				case MusicBrowserAdapter.IDX_ALBUM:
					inflater.inflate(R.menu.album_sort_by, menu);
					inflater.inflate(R.menu.view_as, menu);
					break;
			}
		}
		inflater.inflate(R.menu.item_visibility, menu);
		// setup menu entries
		MenuItem favorite = menu.findItem(R.id.menu_favorite);
		MenuItem showHidden = menu.findItem(R.id.menu_show_hidden);
		if (showHidden != null) {
			showHidden.setChecked(mPreferences.getExcludeTracks());
		}
		Song song = MusicUtils.getCurrentTrack(getActivity());
		if (song != null) {
			FavoritesStore favoritesStore = FavoritesStore.getInstance(requireContext());
			boolean isFavorite = favoritesStore.exists(song.getId());
			mResources.setFavoriteIcon(favorite, isFavorite);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean onMenuItemSelected(@NonNull MenuItem item) {
		// Shuffle all the songs
		if (item.getItemId() == R.id.menu_shuffle) {
			MusicUtils.shuffleAll(requireActivity());
		}
		// Toggle the current track as a favorite and update the menu item
		else if (item.getItemId() == R.id.menu_favorite) {
			Song song = MusicUtils.getCurrentTrack(requireActivity());
			if (song != null) {
				FavoritesStore favoritesStore = FavoritesStore.getInstance(requireContext());
				if (favoritesStore.exists(song.getId())) {
					favoritesStore.removeFavorite(song.getId());
				} else {
					favoritesStore.addFavorite(song);
				}
				requireActivity().invalidateOptionsMenu();
			}
		}
		// sort track/album/artist list alphabetical
		else if (item.getItemId() == R.id.menu_sort_by_az) {
			if (mViewPager.getCurrentItem() == MusicBrowserAdapter.IDX_ARTIST) {
				mPreferences.setArtistSortOrder(SortOrder.ArtistSortOrder.ARTIST_A_Z);
				viewModel.notify(ArtistFragment.REFRESH);
			} else if (mViewPager.getCurrentItem() == MusicBrowserAdapter.IDX_ALBUM) {
				mPreferences.setAlbumSortOrder(SortOrder.AlbumSortOrder.ALBUM_A_Z);
				viewModel.notify(AlbumFragment.REFRESH);
			} else if (mViewPager.getCurrentItem() == MusicBrowserAdapter.IDX_TRACKS) {
				mPreferences.setSongSortOrder(SortOrder.SongSortOrder.SONG_A_Z);
				viewModel.notify(SongFragment.REFRESH);
			}
		}
		// sort track/album/artist list alphabetical reverse
		else if (item.getItemId() == R.id.menu_sort_by_za) {
			if (mViewPager.getCurrentItem() == MusicBrowserAdapter.IDX_ARTIST) {
				mPreferences.setArtistSortOrder(SortOrder.ArtistSortOrder.ARTIST_Z_A);
				viewModel.notify(ArtistFragment.REFRESH);
			} else if (mViewPager.getCurrentItem() == MusicBrowserAdapter.IDX_ALBUM) {
				mPreferences.setAlbumSortOrder(SortOrder.AlbumSortOrder.ALBUM_Z_A);
				viewModel.notify(AlbumFragment.REFRESH);
			} else if (mViewPager.getCurrentItem() == MusicBrowserAdapter.IDX_TRACKS) {
				mPreferences.setSongSortOrder(SortOrder.SongSortOrder.SONG_Z_A);
				viewModel.notify(SongFragment.REFRESH);
			}
		}
		// sort albums/tracks by artist name
		else if (item.getItemId() == R.id.menu_sort_by_artist) {
			if (mViewPager.getCurrentItem() == MusicBrowserAdapter.IDX_ALBUM) {
				mPreferences.setAlbumSortOrder(SortOrder.AlbumSortOrder.ALBUM_ARTIST);
				viewModel.notify(AlbumFragment.REFRESH);
			} else if (mViewPager.getCurrentItem() == MusicBrowserAdapter.IDX_TRACKS) {
				mPreferences.setSongSortOrder(SortOrder.SongSortOrder.SONG_ARTIST);
				viewModel.notify(SongFragment.REFRESH);
			}
		}
		// sort tracks by album name
		else if (item.getItemId() == R.id.menu_sort_by_album) {
			if (mViewPager.getCurrentItem() == MusicBrowserAdapter.IDX_TRACKS) {
				mPreferences.setSongSortOrder(SortOrder.SongSortOrder.SONG_ALBUM);
				viewModel.notify(SongFragment.REFRESH);
			}
		}
		// sort albums/tracks by release date
		else if (item.getItemId() == R.id.menu_sort_by_year) {
			if (mViewPager.getCurrentItem() == MusicBrowserAdapter.IDX_ALBUM) {
				mPreferences.setAlbumSortOrder(SortOrder.AlbumSortOrder.ALBUM_YEAR);
				viewModel.notify(AlbumFragment.REFRESH);
			} else if (mViewPager.getCurrentItem() == MusicBrowserAdapter.IDX_TRACKS) {
				mPreferences.setSongSortOrder(SortOrder.SongSortOrder.SONG_YEAR);
				viewModel.notify(SongFragment.REFRESH);
			}
		}
		// sort tracks by duration
		else if (item.getItemId() == R.id.menu_sort_by_duration) {
			if (mViewPager.getCurrentItem() == MusicBrowserAdapter.IDX_TRACKS) {
				mPreferences.setSongSortOrder(SortOrder.SongSortOrder.SONG_DURATION);
				viewModel.notify(SongFragment.REFRESH);
			}
		}
		// sort artists/albums by song count
		else if (item.getItemId() == R.id.menu_sort_by_number_of_songs) {
			if (mViewPager.getCurrentItem() == MusicBrowserAdapter.IDX_ARTIST) {
				mPreferences.setArtistSortOrder(SortOrder.ArtistSortOrder.ARTIST_NUMBER_OF_SONGS);
				viewModel.notify(ArtistFragment.REFRESH);
			} else if (mViewPager.getCurrentItem() == MusicBrowserAdapter.IDX_ALBUM) {
				mPreferences.setAlbumSortOrder(SortOrder.AlbumSortOrder.ALBUM_NUMBER_OF_SONGS);
				viewModel.notify(AlbumFragment.REFRESH);
			}
		}
		// sort artists by album count
		else if (item.getItemId() == R.id.menu_sort_by_number_of_albums) {
			if (mViewPager.getCurrentItem() == MusicBrowserAdapter.IDX_ARTIST) {
				mPreferences.setArtistSortOrder(SortOrder.ArtistSortOrder.ARTIST_NUMBER_OF_ALBUMS);
				viewModel.notify(ArtistFragment.REFRESH);
			}
		}
		// sort tracks by file name
		else if (item.getItemId() == R.id.menu_sort_by_filename) {
			if (mViewPager.getCurrentItem() == MusicBrowserAdapter.IDX_TRACKS) {
				mPreferences.setSongSortOrder(SortOrder.SongSortOrder.SONG_FILENAME);
				viewModel.notify(SongFragment.REFRESH);
			}
		}
		// set simple item view
		else if (item.getItemId() == R.id.menu_view_as_simple) {
			if (mViewPager.getCurrentItem() == MusicBrowserAdapter.IDX_RECENT) {
				mPreferences.setRecentLayout(AppPreferences.LAYOUT_SIMPLE);
				viewModel.notify(RecentFragment.REFRESH);
			} else if (mViewPager.getCurrentItem() == MusicBrowserAdapter.IDX_ARTIST) {
				mPreferences.setArtistLayout(AppPreferences.LAYOUT_SIMPLE);
				viewModel.notify(ArtistFragment.REFRESH);
			} else if (mViewPager.getCurrentItem() == MusicBrowserAdapter.IDX_ALBUM) {
				mPreferences.setAlbumLayout(AppPreferences.LAYOUT_SIMPLE);
				viewModel.notify(AlbumFragment.REFRESH);
			} else if (mViewPager.getCurrentItem() == MusicBrowserAdapter.IDX_TRACKS) {
				mPreferences.setTrackLayout(AppPreferences.LAYOUT_SIMPLE);
				viewModel.notify(SongFragment.REFRESH);
			}
		}
		// set detailed item view
		else if (item.getItemId() == R.id.menu_view_as_detailed) {
			if (mViewPager.getCurrentItem() == MusicBrowserAdapter.IDX_RECENT) {
				mPreferences.setRecentLayout(AppPreferences.LAYOUT_DETAILED);
				viewModel.notify(RecentFragment.REFRESH);
			} else if (mViewPager.getCurrentItem() == MusicBrowserAdapter.IDX_ARTIST) {
				mPreferences.setArtistLayout(AppPreferences.LAYOUT_DETAILED);
				viewModel.notify(ArtistFragment.REFRESH);
			} else if (mViewPager.getCurrentItem() == MusicBrowserAdapter.IDX_ALBUM) {
				mPreferences.setAlbumLayout(AppPreferences.LAYOUT_DETAILED);
				viewModel.notify(AlbumFragment.REFRESH);
			} else if (mViewPager.getCurrentItem() == MusicBrowserAdapter.IDX_TRACKS) {
				mPreferences.setTrackLayout(AppPreferences.LAYOUT_DETAILED);
				viewModel.notify(SongFragment.REFRESH);
			}
		}
		// set grid item view
		else if (item.getItemId() == R.id.menu_view_as_grid) {
			if (mViewPager.getCurrentItem() == MusicBrowserAdapter.IDX_RECENT) {
				mPreferences.setRecentLayout(AppPreferences.LAYOUT_GRID);
				viewModel.notify(RecentFragment.REFRESH);
			} else if (mViewPager.getCurrentItem() == MusicBrowserAdapter.IDX_ARTIST) {
				mPreferences.setArtistLayout(AppPreferences.LAYOUT_GRID);
				viewModel.notify(ArtistFragment.REFRESH);
			} else if (mViewPager.getCurrentItem() == MusicBrowserAdapter.IDX_ALBUM) {
				mPreferences.setAlbumLayout(AppPreferences.LAYOUT_GRID);
				viewModel.notify(AlbumFragment.REFRESH);
			}
		}
		// toggle hidden track visibility
		else if (item.getItemId() == R.id.menu_show_hidden) {
			boolean isChecked = !item.isChecked();
			item.setChecked(isChecked);
			mPreferences.setExcludeTracks(isChecked);
			viewModel.notify(REFRESH);
		} else {
			return false;
		}
		return true;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onPageSelected(int position) {
		// update options menu when changing tab
		requireActivity().invalidateOptionsMenu();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onPageScrollStateChanged(int state) {
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onCenterItemClick(int position) {
		switch (position) {
			case MusicBrowserAdapter.IDX_ALBUM:
				viewModel.notify(AlbumFragment.SCROLL_TOP);
				break;

			case MusicBrowserAdapter.IDX_ARTIST:
				viewModel.notify(ArtistFragment.SCROLL_TOP);
				break;

			case MusicBrowserAdapter.IDX_TRACKS:
				viewModel.notify(SongFragment.SCROLL_TOP);
				break;

			case MusicBrowserAdapter.IDX_RECENT:
				viewModel.notify(RecentFragment.SCROLL_TOP);
				break;
		}
	}
}