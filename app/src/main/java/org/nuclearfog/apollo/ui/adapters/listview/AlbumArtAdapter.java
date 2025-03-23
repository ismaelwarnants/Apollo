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

	private static final int LAYOUT_ID = R.layout.list_item_normal;

	private ImageFetcher mImageFetcher;

	/**
	 *
	 */
	public AlbumArtAdapter(Context context) {
		super(context, LAYOUT_ID);
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
			convertView = LayoutInflater.from(parent.getContext()).inflate(LAYOUT_ID, parent, false);
			ImageView background = convertView.findViewById(R.id.list_item_background);
			background.setVisibility(View.INVISIBLE);
		}
		ImageView artwork = convertView.findViewById(R.id.image);
		TextView title = convertView.findViewById(R.id.line_one);
		TextView artist = convertView.findViewById(R.id.line_two);
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