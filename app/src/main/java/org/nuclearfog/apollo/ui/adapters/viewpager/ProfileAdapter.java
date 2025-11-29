package org.nuclearfog.apollo.ui.adapters.viewpager;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentStatePagerAdapter;

import org.nuclearfog.apollo.ui.activities.ProfileActivity;
import org.nuclearfog.apollo.ui.activities.ProfileActivity.Type;
import org.nuclearfog.apollo.ui.fragments.profile.AlbumSongFragment;
import org.nuclearfog.apollo.ui.fragments.profile.ArtistAlbumFragment;
import org.nuclearfog.apollo.ui.fragments.profile.ArtistSongFragment;
import org.nuclearfog.apollo.ui.fragments.profile.FavoriteSongFragment;
import org.nuclearfog.apollo.ui.fragments.profile.FolderSongFragment;
import org.nuclearfog.apollo.ui.fragments.profile.GenreSongFragment;
import org.nuclearfog.apollo.ui.fragments.profile.LastAddedSongFragment;
import org.nuclearfog.apollo.ui.fragments.profile.PlaylistSongFragment;
import org.nuclearfog.apollo.ui.fragments.profile.PopularSongFragment;
import org.nuclearfog.apollo.utils.ApolloUtils;

/**
 * {@link ProfileActivity} viewpager adapter
 *
 * @author nuclearfog
 */
@SuppressWarnings("deprecation")
public class ProfileAdapter extends FragmentStatePagerAdapter {

	/**
	 * index of the artist song fragment
	 */
	public static final int IDX_ARTIST_SONG = 0;

	/**
	 * index of the artist album fragment
	 */
	public static final int IDX_ARTIST_ALBUM = 1;

	private Type type;
	private Bundle args;
	private boolean landscape;

	/**
	 *
	 */
	public ProfileAdapter(FragmentActivity activity, @Nullable Bundle args, Type type) {
		super(activity.getSupportFragmentManager(), BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);
		landscape = ApolloUtils.isLandscape(activity);
		this.type = type;
		this.args = args;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public float getPageWidth(int position) {
		if (landscape && getCount() > 1)
			return .5f;
		return 1f;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	@NonNull
	public Fragment getItem(int position) {
		Fragment fragment;
		switch (type) {
			case ALBUM:
				fragment = new AlbumSongFragment();
				break;

			case GENRE:
				fragment = new GenreSongFragment();
				break;

			case FOLDER:
				fragment = new FolderSongFragment();
				break;

			case FAVORITE:
				fragment = new FavoriteSongFragment();
				break;

			case PLAYLIST:
				fragment = new PlaylistSongFragment();
				break;

			case LAST_ADDED:
				fragment = new LastAddedSongFragment();
				break;

			case POPULAR:
				fragment = new PopularSongFragment();
				break;

			case ARTIST:
				if (position == IDX_ARTIST_SONG) {
					fragment = new ArtistSongFragment();
					break;
				} else if (position == IDX_ARTIST_ALBUM) {
					fragment = new ArtistAlbumFragment();
					break;
				}

			default:
				fragment = new Fragment();
				break;
		}
		fragment.setArguments(args);
		return fragment;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int getCount() {
		switch (type) {
			case ALBUM:
			case GENRE:
			case FOLDER:
			case FAVORITE:
			case PLAYLIST:
			case LAST_ADDED:
			case POPULAR:
				return 1;

			case ARTIST:
				return 2;

			default:
				return 0;
		}
	}
}