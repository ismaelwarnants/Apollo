package org.nuclearfog.apollo.ui.adapters.listview;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;

import org.nuclearfog.apollo.R;
import org.nuclearfog.apollo.cache.ImageFetcher;
import org.nuclearfog.apollo.model.Artist;
import org.nuclearfog.apollo.ui.adapters.listview.holder.MusicHolder;
import org.nuclearfog.apollo.ui.fragments.phone.ArtistFragment;
import org.nuclearfog.apollo.utils.ApolloUtils;
import org.nuclearfog.apollo.utils.Constants;
import org.nuclearfog.apollo.utils.StringUtils;

/**
 * This adapter is used to display all artists to {@link ArtistFragment}
 *
 * @author Andrew Neal (andrewdneal@gmail.com)
 * @author nuclearfog
 */
public class ArtistAdapter extends AlphabeticalAdapter<Artist> {

	/**
	 * Image cache and image fetcher
	 */
	private ImageFetcher mImageFetcher;

	/**
	 * The resource Id of the layout to inflate
	 */
	private int mLayoutId;

	/**
	 * Loads line three and the background image if the user decides to.
	 */
	private boolean mLoadExtraData = false;

	/**
	 * {@inheritDoc}
	 */
	public ArtistAdapter(Context context, int columns, int layoutId) {
		super(context, columns, 0);
		// Get the layout Id
		mLayoutId = layoutId;
		// Initialize the cache & image fetcher
		mImageFetcher = new ImageFetcher(context);
	}

	/**
	 * {@inheritDoc}
	 */
	@NonNull
	@Override
	public View getView(int position, View convertView, @NonNull ViewGroup parent) {
		// Recycle ViewHolder's items
		MusicHolder holder;
		if (convertView == null) {
			LayoutInflater inflater = LayoutInflater.from(parent.getContext());
			convertView = inflater.inflate(mLayoutId, parent, false);
			holder = new MusicHolder(convertView);
			if (holder.mLineThree != null && !mLoadExtraData) {
				holder.mLineThree.setVisibility(View.GONE);
			}
			convertView.setTag(holder);
		} else {
			holder = (MusicHolder) convertView.getTag();
		}
		Artist artist = getItem(position);
		if (artist != null) {
			// Number of albums (line two)
			String numAlbums = StringUtils.makeLabel(getContext(), R.plurals.Nalbums, artist.getAlbumCount());
			// Set each artist name (line one)
			holder.mLineOne.setText(artist.getName());
			// Set the number of albums (line two)
			holder.mLineTwo.setText(numAlbums);
			// Asynchronously load the artist image into the adapter
			mImageFetcher.loadArtistImage(artist.getId(), holder.mImage);
			if (mLoadExtraData) {
				// Number of songs (line three)
				String numTracks = StringUtils.makeLabel(getContext(), R.plurals.Nsongs, artist.getTrackCount());
				// Set the number of songs (line three)
				if (holder.mLineThree != null)
					holder.mLineThree.setText(numTracks);
				// register artist art click listener
				ApolloUtils.registerItemViewListener(holder.mImage, parent, position, artist.getId());
			}
			if (artist.isVisible()) {
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
		Artist artist = getItem(position);
		if (artist != null)
			return artist.getId();
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

	/**
	 * enable extra information
	 */
	public void setLoadExtraData() {
		mLoadExtraData = true;
	}
}
