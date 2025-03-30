package org.nuclearfog.apollo.ui.adapters.viewpager;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;

import org.nuclearfog.apollo.R;
import org.nuclearfog.apollo.ui.fragments.phone.AlbumFragment;
import org.nuclearfog.apollo.ui.fragments.phone.ArtistFragment;
import org.nuclearfog.apollo.ui.fragments.phone.FolderFragment;
import org.nuclearfog.apollo.ui.fragments.phone.GenreFragment;
import org.nuclearfog.apollo.ui.fragments.phone.PlaylistFragment;
import org.nuclearfog.apollo.ui.fragments.phone.RecentFragment;
import org.nuclearfog.apollo.ui.fragments.phone.SongFragment;

import java.util.Locale;

/**
 * ViewPager adapter to show fragments for {@link org.nuclearfog.apollo.ui.fragments.phone.MusicBrowserPhoneFragment}
 *
 * @author nuclearfog
 */
@SuppressWarnings("deprecation")
public class MusicBrowserAdapter extends FragmentStatePagerAdapter {

	public static final int IDX_PLAYLIST = 0;
	public static final int IDX_RECENT = 1;
	public static final int IDX_ARTIST = 2;
	public static final int IDX_ALBUM = 3;
	public static final int IDX_TRACKS = 4;
	public static final int IDX_GENRE = 5;
	public static final int IDX_FOLDER = 6;

	private static final int PAGE_COUNT = 7;

	private String[] titles;

	/**
	 *
	 */
	public MusicBrowserAdapter(Context context, FragmentManager fm) {
		super(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);
		titles = context.getResources().getStringArray(R.array.page_titles);
	}

	/**
	 * {@inheritDoc}
	 */
	@NonNull
	@Override
	public Fragment getItem(int position) {
		switch (position) {
			default:
			case IDX_PLAYLIST:
				return new PlaylistFragment();

			case IDX_RECENT:
				return new RecentFragment();

			case IDX_ARTIST:
				return new ArtistFragment();

			case IDX_ALBUM:
				return new AlbumFragment();

			case IDX_TRACKS:
				return new SongFragment();

			case IDX_GENRE:
				return new GenreFragment();

			case IDX_FOLDER:
				return new FolderFragment();
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int getCount() {
		return PAGE_COUNT;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public CharSequence getPageTitle(int position) {
		return titles[position].toUpperCase(Locale.getDefault());
	}
}