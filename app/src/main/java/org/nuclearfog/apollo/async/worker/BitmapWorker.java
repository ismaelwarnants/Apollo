package org.nuclearfog.apollo.async.worker;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;

import org.nuclearfog.apollo.R;
import org.nuclearfog.apollo.async.AsyncExecutor;
import org.nuclearfog.apollo.async.worker.BitmapWorker.Param;
import org.nuclearfog.apollo.async.worker.BitmapWorker.Result;
import org.nuclearfog.apollo.cache.ImageFetcher;
import org.nuclearfog.apollo.utils.BitmapUtils;
import org.nuclearfog.apollo.utils.Constants.ImageType;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.net.URL;

/**
 * Async worker to download image artworks
 *
 * @author nuclearfog
 */
public class BitmapWorker extends AsyncExecutor<Param, Result> {

	/**
	 * Default transition drawable fade time
	 */
	private static final int FADE_IN_TIME = 200;

	private WeakReference<ImageFetcher> callback;
	private ImageType mImageType;


	public BitmapWorker(ImageFetcher worker, ImageType mImageType) {
		super(null);
		callback = new WeakReference<>(worker);
		this.mImageType = mImageType;
	}


	@Override
	protected Result doInBackground(Param param) {
		ImageFetcher imageFetcher = callback.get();
		if (imageFetcher == null)
			return null;

		// First, check the disk cache for the image
		Bitmap bitmap = imageFetcher.getCachedBitmap(param.cacheKey);

		// second, check the media store for any album art
		if (bitmap == null && mImageType == ImageType.ALBUM && param.albumId != 0) {
			bitmap = imageFetcher.getAlbumArtwork(param.albumId);
			if (bitmap != null) {
				imageFetcher.addImageToCache(bitmap, param.cacheKey);
			}
		}

		// Third, by now we need to download the image
		if (bitmap == null) {
			// Now define what the artist name, album name, and url are.
			String mUrl = imageFetcher.downloadImage(mImageType, param.mArtistName, param.mAlbumName);
			try {
				if (mUrl != null)
					bitmap = BitmapFactory.decodeStream(new URL(mUrl).openConnection().getInputStream());
				if (bitmap != null) {
					imageFetcher.addImageToCache(bitmap, param.cacheKey);
				}
			} catch (IOException e) {
				// proceed without bitmap
			}
		}

		// Fourth, add the new image to the cache and create drawables
		if (bitmap != null) {
			// create drawables
			Drawable layerOne = new ColorDrawable(imageFetcher.getContext().getResources().getColor(R.color.transparent));
			BitmapDrawable layerTwo = new BitmapDrawable(imageFetcher.getContext().getResources(), bitmap);
			layerTwo.setFilterBitmap(false);
			layerTwo.setDither(false);
			TransitionDrawable result = new TransitionDrawable(new Drawable[]{layerOne, layerTwo});
			result.setCrossFadeEnabled(true);
			result.startTransition(FADE_IN_TIME);

			Bitmap blur = BitmapUtils.createBlurredBitmap(bitmap);
			BitmapDrawable layerBlur = new BitmapDrawable(imageFetcher.getContext().getResources(), blur);
			return new Result(result, layerBlur);
		}
		return null;
	}

	/**
	 *
	 */
	public static class Param {

		final String cacheKey, mArtistName, mAlbumName;
		final long albumId;

		public Param(String cacheKey, String mAlbumName, String mArtistName, long albumId) {
			this.cacheKey = cacheKey;
			this.mAlbumName = mAlbumName;
			this.mArtistName = mArtistName;
			this.albumId = albumId;
		}
	}

	/**
	 *
	 */
	public static class Result {

		public final Drawable drawable, blurredDrawable;

		Result(Drawable drawable, Drawable blurredDrawable) {
			this.drawable = drawable;
			this.blurredDrawable = blurredDrawable;
		}
	}
}