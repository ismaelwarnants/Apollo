package org.nuclearfog.apollo.ui.adapters.listview;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.nuclearfog.apollo.R;
import org.nuclearfog.apollo.model.Song;
import org.nuclearfog.apollo.store.preferences.AppPreferences;
import org.nuclearfog.apollo.ui.adapters.listview.holder.MusicHolder;
import org.nuclearfog.apollo.ui.fragments.phone.SongFragment;
import org.nuclearfog.apollo.utils.Constants;
import org.nuclearfog.apollo.utils.StringUtils;

/**
 * This adapter is used to display all of the songs on a user's
 * device for {@link SongFragment}. It is also used to show the queue in
 * {@link org.nuclearfog.apollo.ui.fragments.QueueFragment}.
 *
 * @author Andrew Neal (andrewdneal@gmail.com)
 * @author nuclearfog
 */
public class SongAdapter extends AlphabeticalAdapter<Song> {

	/**
	 * item layout
	 */
	private static final int LAYOUT = R.layout.list_item_simple;

	/**
	 * transparency mask for a RGB color
	 */
	private static final int TRANSPARENCY_MASK = 0x40FFFFFF;

	/**
	 * current item position of the current track
	 */
	private int nowplayingPos = -1;

	/**
	 * background color of the selected track
	 */
	private int selectedColor;

	/**
	 * flag to enable drag and drop icon
	 */
	private boolean enableDnD;

	/**
	 * @param enableDrag true to enable drag & drop feature
	 */
	public SongAdapter(Context context, boolean enableDrag) {
		super(context, LAYOUT);
		AppPreferences prefs = AppPreferences.getInstance(context);
		selectedColor = prefs.getThemeColor() & TRANSPARENCY_MASK;
		enableDnD = enableDrag;
	}

	/**
	 * {@inheritDoc}
	 */
	@NonNull
	@Override
	public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
		// Recycle ViewHolder's items
		MusicHolder holder;
		if (convertView == null) {
			convertView = LayoutInflater.from(parent.getContext()).inflate(LAYOUT, parent, false);
			if (enableDnD)
				convertView.findViewById(R.id.edit_track_list_item_handle).setVisibility(View.VISIBLE);
			holder = new MusicHolder(convertView);
			// Hide the third line of text
			holder.mLineThree.setVisibility(View.GONE);
			convertView.setTag(holder);
		} else {
			holder = (MusicHolder) convertView.getTag();
		}
		// set background color
		if (position == nowplayingPos) {
			convertView.setBackgroundColor(selectedColor);
		} else {
			convertView.setBackgroundColor(0);
		}
		// Retrieve the data holder
		Song song = getItem(position);
		if (song != null) {
			// Set each song name (line one)
			holder.mLineOne.setText(song.getName());
			// Set the song duration (line one, right)
			holder.mLineOneRight.setText(StringUtils.makeTimeString(getContext(), (int) song.getDuration()));
			// Set the album name (line two)
			holder.mLineTwo.setText(song.getArtist());
			if (song.isVisible()) {
				convertView.setAlpha(1.0f);
			} else {
				convertView.setAlpha(Constants.OPACITY_HIDDEN);
			}
		}
		return convertView;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public long getItemId(int position) {
		Song song = getItem(position);
		if (song != null)
			return song.getId();
		return super.getItemId(position);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void remove(@Nullable Song song) {
		int pos = getPosition(song);
		if (pos < nowplayingPos)
			nowplayingPos--;
		super.remove(song);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void insert(Song song, int to) {
		if (to <= nowplayingPos)
			nowplayingPos++;
		super.insert(song, to);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean hasStableIds() {
		return true;
	}

	/**
	 * moves the track item to another position
	 *
	 * @param from index where the track is located
	 * @param to   index where the track should be moved
	 */
	@MainThread
	public void moveTrack(int from, int to) {
		if (from != to) {
			if (from != nowplayingPos) {
				// move tracks around selected track
				Song mSong = getItem(from);
				remove(mSong);
				insert(mSong, to);
			} else {
				// move selected track to new position
				Song mSong = getItem(from);
				remove(mSong);
				insert(mSong, to);
				nowplayingPos = to;
			}
		} else {
			// nothing changed, revert layout changes
			notifyDataSetChanged();
		}
	}

	/**
	 * set current track ID
	 *
	 * @param pos position of the current track
	 */
	public void setCurrentTrackPos(int pos) {
		nowplayingPos = pos;
		notifyDataSetChanged();
	}
}