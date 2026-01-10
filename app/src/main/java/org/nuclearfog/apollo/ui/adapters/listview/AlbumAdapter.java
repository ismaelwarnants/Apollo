package org.nuclearfog.apollo.ui.adapters.listview;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.nuclearfog.apollo.R;
import org.nuclearfog.apollo.cache.ImageFetcher;
import org.nuclearfog.apollo.model.Album;
import org.nuclearfog.apollo.model.Music;
import org.nuclearfog.apollo.ui.adapters.listview.holder.MusicHolder;
import org.nuclearfog.apollo.ui.fragments.phone.AlbumFragment;
import org.nuclearfog.apollo.ui.fragments.phone.RecentFragment;
import org.nuclearfog.apollo.utils.ApolloUtils;
import org.nuclearfog.apollo.utils.Constants;
import org.nuclearfog.apollo.utils.StringUtils;

/**
 * This adapter is used to display all of the albums on a user's
 * device for {@link RecentFragment} and {@link AlbumFragment}.
 *
 * @author Andrew Neal (andrewdneal@gmail.com)
 * @author nuclearfog
 */
public class AlbumAdapter extends AlphabeticalAdapter<Album> {

	/**
	 * Image cache and image fetcher
	 */
	private ImageFetcher mImageFetcher;

	/**
	 * The resource Id of the layout to inflate
	 */
	private int mLayoutId;

	/**
	 * {@inheritDoc}
	 */
	public AlbumAdapter(Context context, int columns, @LayoutRes int mLayoutId) {
		super(context, columns, mLayoutId);
		this.mLayoutId = mLayoutId;
		// Initialize the cache & image fetcher
		mImageFetcher = new ImageFetcher(context);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	@NonNull
	public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
		// Recycle ViewHolder's items
		MusicHolder holder;
		if (convertView == null) {
			LayoutInflater inflater = LayoutInflater.from(parent.getContext());
			convertView = inflater.inflate(mLayoutId, parent, false);
			holder = new MusicHolder(convertView);
			convertView.setTag(holder);
		} else {
			holder = (MusicHolder) convertView.getTag();
		}
		// Retrieve the data holder
		Album album = getItem(position);
		if (album != null) {
			// Set each album name (line one)
			holder.mLineOne.setText(album.getName());
			// Set the artist name (line two)
			holder.mLineTwo.setText(album.getArtist());
			// Asynchronously load the album images into the adapter
			mImageFetcher.loadAlbumImage(album.getId(), holder.mImage);
			// List view only items
			if (mLayoutId == R.layout.list_item_detailed) {
				// Set the number of songs (line three)
				if (holder.mLineThree != null) {
					String count = StringUtils.makeLabel(getContext(), R.plurals.Nsongs, album.getTrackCount());
					holder.mLineThree.setText(count);
				}
				// register album art click listener
				ApolloUtils.registerItemViewListener(holder.mImage, parent, position, album.getId());
			}
			if (album.isVisible()) {
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
		Music item = getItem(position);
		if (item != null)
			return item.getId();
		return super.getItemId(position);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean hasStableIds() {
		return true;
	}

	/**
	 * @param pause True to temporarily pause the disk cache, false otherwise.
	 */
	public void setPauseDiskCache(boolean pause) {
		if (mImageFetcher != null) {
			mImageFetcher.setPauseDiskCache(pause);
		}
	}

	/**
	 * Flushes the disk cache.
	 */
	public void flush() {
		mImageFetcher.clear();
	}
}