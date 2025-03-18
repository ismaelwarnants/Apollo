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

import java.net.URL;

/**
 * Async loader to download artwork bitmaps
 *
 * @author nuclearfog
 */
public class ArtworkDownloader extends AsyncExecutor<Param, Bitmap> {

	private static final String TAG = "ArtworkDownloader";


	public ArtworkDownloader(Context context) {
		super(context);
	}


	@Override
	protected Bitmap doInBackground(Param param) {
		try {
			if (getContext() != null) {
				ImageFetcher imageFetcher = new ImageFetcher(getContext());
				Artwork artwork = MusicBrainz.getImage(param.mbid);
				if (artwork != null) {
					Bitmap bitmap = null;
					String url = artwork.getThumbnailUrl();
					if (url != null)
						bitmap = BitmapFactory.decodeStream(new URL(url).openConnection().getInputStream());
					if (bitmap != null) {
						imageFetcher.addImageToCache(bitmap, param.cacheKey);
						return bitmap;
					}
				}
			}
		} catch (Exception exception) {
			Log.e(TAG, "could not download image!", exception);
		}
		return null;
	}


	public static class Param {

		String cacheKey, mbid;

		public Param(String cacheKey, String mbid) {
			this.cacheKey = cacheKey;
			this.mbid = mbid;
		}
	}
}