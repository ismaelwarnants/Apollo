package org.nuclearfog.apollo.ui.adapters.listview;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.nuclearfog.apollo.R;
import org.nuclearfog.apollo.cache.ImageFetcher;
import org.nuclearfog.apollo.lookup.entities.AlbumMB;


public class AlbumArtAdapter extends ArrayAdapter<AlbumMB> {

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
		ImageView albumart = convertView.findViewById(R.id.list_item_albumart_image);
		TextView title = convertView.findViewById(R.id.list_item_albumart_title);
		TextView artist = convertView.findViewById(R.id.list_item_albumart_artist);
		if (album != null) {
			String artistname = "";
			if (album.getArtist() != null)
				artistname = album.getArtist().getName();
			mImageFetcher.loadAlbumImage(artistname, album.getName(), 0L, albumart);
			title.setText(album.getName());
			artist.setText(album.getArtist().getName());
		}
		return convertView;
	}


	/**
	 * @param pause True to temporarily pause the disk cache, false otherwise.
	 */
	public void setPauseDiskCache(boolean pause) {
		mImageFetcher.setPauseDiskCache(pause);
	}
}