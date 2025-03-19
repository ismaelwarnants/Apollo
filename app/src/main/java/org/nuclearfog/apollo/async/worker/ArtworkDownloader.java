package org.nuclearfog.apollo.async.worker;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import org.nuclearfog.apollo.async.AsyncExecutor;
import org.nuclearfog.apollo.async.worker.ArtworkDownloader.Param;
import org.nuclearfog.apollo.cache.ImageFetcher;
import org.nuclearfog.apollo.lookup.MusicBrainz;
import org.nuclearfog.apollo.lookup.entities.Artwork;
import org.nuclearfog.apollo.utils.Constants.ImageType;

import java.net.URL;

/**
 * Async loader to download artwork bitmaps
 *
 * @author nuclearfog
 */
public class ArtworkDownloader extends AsyncExecutor<Param, Bitmap> {

	private static final String TAG = "ArtworkDownloader";

	private ImageFetcher imgFetcher;

	public ArtworkDownloader(Context context) {
		super(context);
		imgFetcher = new ImageFetcher(context);
	}


	@Override
	protected Bitmap doInBackground(Param param) {
		try {
			Artwork artwork = MusicBrainz.getImage(param.mbid);
			if (artwork != null) {
				String url = artwork.getThumbnailUrl();
				Bitmap bitmap = BitmapFactory.decodeStream(new URL(url).openConnection().getInputStream());
				if (bitmap != null) {
					if (param.type == ImageType.ARTIST) {
						imgFetcher.setArtistImage(param.id, bitmap);
					} else if (param.type == ImageType.ALBUM) {
						imgFetcher.setAlbumImage(param.id, bitmap);
					}
					return bitmap;
				}
			}
		} catch (Exception exception) {
			Log.e(TAG, "could not download image!", exception);
		}
		return null;
	}

	/**
	 *
	 */
	public static class Param {

		String mbid;
		long id;
		ImageType type;

		public Param(ImageType type, long id, String mbid) {
			this.type = type;
			this.mbid = mbid;
			this.id = id;
		}
	}
}