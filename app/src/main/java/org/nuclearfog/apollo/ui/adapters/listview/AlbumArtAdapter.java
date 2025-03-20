package org.nuclearfog.apollo.ui.adapters.listview;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.nuclearfog.apollo.R;
import org.nuclearfog.apollo.cache.ImageFetcher;
import org.nuclearfog.apollo.lookup.entities.AlbumMB;

/**
 * ListView adapter used to show a list of album artworks and descriptions
 *
 * @author nuclearfog
 */
public class AlbumArtAdapter extends AlphabeticalAdapter<AlbumMB> {

	private ImageFetcher mImageFetcher;


	public AlbumArtAdapter(@NonNull Context context) {
		super(context, R.layout.list_item_albumart);
		mImageFetcher = new ImageFetcher(context);
	}

	/**
	 * {@inheritDoc}
	 */
	@NonNull
	@Override
	public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
		AlbumMB album = getItem(position);
		if (convertView == null) {
			convertView = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_albumart, parent, false);
		}
		ImageView artwork = convertView.findViewById(R.id.list_item_albumart_image);
		TextView title = convertView.findViewById(R.id.list_item_albumart_title);
		TextView artist = convertView.findViewById(R.id.list_item_albumart_artist);
		if (album != null) {
			mImageFetcher.loadArtworkImage(album.getId(), artwork);
			title.setText(album.getName());
			if (album.getArtist() != null) {
				artist.setText(album.getArtist().getName());
			}
		}
		return convertView;
	}

	/**
	 * Flushes the disk cache.
	 */
	public void flush() {
		mImageFetcher.clear();
	}
}