package org.nuclearfog.apollo.async.worker;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import org.nuclearfog.apollo.async.AsyncExecutor;
import org.nuclearfog.apollo.async.worker.ArtworkDownloader.Param;
import org.nuclearfog.apollo.cache.ImageCache;
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

	private ImageCache imageCache;


	public ArtworkDownloader(Context context) {
		super(context);
		imageCache = ImageCache.getInstance(context);
	}


	@Override
	protected Bitmap doInBackground(Param param) {
		try {
			Artwork artwork = MusicBrainz.getImage(param.mbid);
			if (artwork != null) {
				String url = artwork.getThumbnailUrl();
				Bitmap bitmap = BitmapFactory.decodeStream(new URL(url).openConnection().getInputStream());
				if (bitmap != null) {
					imageCache.addBitmapToCache(param.cacheKey, bitmap);
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

		String cacheKey, mbid;

		public Param(String cacheKey, String mbid) {
			this.cacheKey = cacheKey;
			this.mbid = mbid;
		}
	}
}