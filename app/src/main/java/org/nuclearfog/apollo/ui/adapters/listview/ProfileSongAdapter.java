/*
 * Copyright (C) 2012 Andrew Neal Licensed under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law
 * or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */

package org.nuclearfog.apollo.ui.adapters.listview;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.nuclearfog.apollo.R;
import org.nuclearfog.apollo.model.Song;
import org.nuclearfog.apollo.ui.adapters.listview.holder.MusicHolder;
import org.nuclearfog.apollo.ui.fragments.profile.LastAddedSongFragment;
import org.nuclearfog.apollo.ui.views.ProfileTabCarousel;
import org.nuclearfog.apollo.utils.ApolloUtils;
import org.nuclearfog.apollo.utils.StringUtils;

/**
 * This adapter is used to display the songs for a particular
 * artist, album, playlist, or genre for {@link org.nuclearfog.apollo.ui.fragments.profile.ArtistSongFragment},
 * {@link org.nuclearfog.apollo.ui.fragments.profile.AlbumSongFragment},{@link org.nuclearfog.apollo.ui.fragments.profile.PlaylistSongFragment},
 * {@link org.nuclearfog.apollo.ui.fragments.profile.GenreSongFragment},{@link org.nuclearfog.apollo.ui.fragments.profile.FavoriteSongFragment},
 * {@link LastAddedSongFragment}.
 *
 * @author Andrew Neal (andrewdneal@gmail.com)
 * @author nuclearfog
 */
public class ProfileSongAdapter extends AlphabeticalAdapter<Song> {

	/**
	 * The header view
	 */
	private static final int ITEM_VIEW_TYPE_HEADER = 0;

	/**
	 * * The data in the list.
	 */
	private static final int ITEM_VIEW_TYPE_MUSIC = 1;

	/**
	 * Count of the view header
	 */
	public static final int HEADER_COUNT = 1;

	/**
	 * item layout
	 */
	private static final int LAYOUT = R.layout.list_item_simple;

	/**
	 * Display setting for the second line in a song fragment
	 */
	private DisplaySetting mDisplaySetting;

	/**
	 * flag to set drag and drop icon
	 */
	private boolean enableDnD;

	/**
	 * true to put an invisible placeholder in the first place
	 */
	private boolean enableHeader;

	/**
	 * @param context The {@link Context} to use
	 * @param setting defines the content of the second line
	 */
	public ProfileSongAdapter(Context context, DisplaySetting setting, boolean enableDrag) {
		super(context, LAYOUT);
		this.mDisplaySetting = setting;
		this.enableDnD = enableDrag;
		this.enableHeader = !ApolloUtils.isLandscape(context);
	}

	/**
	 * {@inheritDoc}
	 */
	@NonNull
	@Override
	public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
		// Return a faux header at position 0
		if (enableHeader && position == 0) {
			ProfileTabCarousel mHeader = new ProfileTabCarousel(parent.getContext());
			mHeader.setVisibility(View.INVISIBLE);
			return mHeader;
		}
		// Recycle MusicHolder's items
		MusicHolder holder;
		if (convertView == null) {
			LayoutInflater inflater = LayoutInflater.from(parent.getContext());
			convertView = inflater.inflate(LAYOUT, parent, false);
			holder = new MusicHolder(convertView);
			// Hide the third line of text
			holder.mLineThree.setVisibility(View.GONE);
			convertView.setTag(holder);
			if (enableDnD) {
				View dragDropView = convertView.findViewById(R.id.edit_track_list_item_handle);
				dragDropView.setVisibility(View.VISIBLE);
			}
		} else {
			holder = (MusicHolder) convertView.getTag();
		}
		// Retrieve the album
		Song song = getItem(position);
		if (song != null) {
			// Set each track name (line one)
			holder.mLineOne.setText(song.getName());
			// Set the line two
			if (mDisplaySetting == DisplaySetting.DISPLAY_ALBUM_SETTING) {
				holder.mLineOneRight.setVisibility(View.GONE);
				holder.mLineTwo.setText(StringUtils.makeTimeString(getContext(), song.getDuration()));
			} else if (mDisplaySetting == DisplaySetting.DISPLAY_PLAYLIST_SETTING) {
				if (song.getDuration() < 0L) {
					holder.mLineOneRight.setVisibility(View.GONE);
				} else {
					holder.mLineOneRight.setVisibility(View.VISIBLE);
					holder.mLineOneRight.setText(StringUtils.makeTimeString(getContext(), song.getDuration()));
				}
				String sb = song.getArtist() + " - " + song.getAlbum();
				holder.mLineTwo.setText(sb);
			} else {
				holder.mLineOneRight.setVisibility(View.VISIBLE);
				holder.mLineOneRight.setText(StringUtils.makeTimeString(getContext(), song.getDuration()));
				holder.mLineTwo.setText(song.getAlbum());
			}
		}
		return convertView;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int getCount() {
		if (enableHeader)
			return super.getCount() + HEADER_COUNT;
		return super.getCount();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean isEmpty() {
		return super.getCount() == 0;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int getItemViewType(int position) {
		if (position == 0)
			return ITEM_VIEW_TYPE_HEADER;
		return ITEM_VIEW_TYPE_MUSIC;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int getViewTypeCount() {
		if (enableHeader)
			return 2;
		return 1;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void insert(@Nullable Song song, int index) {
		super.insert(song, index - HEADER_COUNT);
	}

	/**
	 * {@inheritDoc}
	 */
	@Nullable
	@Override
	public Song getItem(int position) {
		if (!enableHeader)
			return super.getItem(position);
		if (position >= HEADER_COUNT)
			return super.getItem(position - HEADER_COUNT);
		return null;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public long getItemId(int position) {
		Song song = getItem(position);
		if (song != null) {
			return song.getId();
		}
		return super.getItemId(position);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean hasStableIds() {
		return true;
	}


	public enum DisplaySetting {

		/**
		 * Default display setting: title/album
		 */
		DISPLAY_DEFAULT_SETTING,

		/**
		 * Playlist display setting: title/artist-album
		 */
		DISPLAY_PLAYLIST_SETTING,

		/**
		 * Album display setting: title/duration
		 */
		DISPLAY_ALBUM_SETTING
	}
}