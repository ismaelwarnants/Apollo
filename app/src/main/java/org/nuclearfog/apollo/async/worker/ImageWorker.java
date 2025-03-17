package org.nuclearfog.apollo.async.worker;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.nuclearfog.apollo.async.AsyncExecutor;
import org.nuclearfog.apollo.async.worker.ImageWorker.Param;
import org.nuclearfog.apollo.async.worker.ImageWorker.Result;
import org.nuclearfog.apollo.cache.ImageFetcher;
import org.nuclearfog.apollo.utils.Constants.ImageType;
import org.nuclearfog.apollo.utils.ImageUtils;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.net.URL;

/**
 * Async worker to download image artworks
 *
 * @author nuclearfog
 */
public class ImageWorker extends AsyncExecutor<Param, Result> {

	private static final String TAG = "ImageWorker";

	private WeakReference<ImageFetcher> callback;
	private ImageType mImageType;


	public ImageWorker(ImageFetcher worker, ImageType mImageType) {
		super(null);
		callback = new WeakReference<>(worker);
		this.mImageType = mImageType;
	}


	@Override
	protected Result doInBackground(Param param) {
		ImageFetcher imageFetcher = callback.get();
		if (imageFetcher == null) {
			return null;
		}

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
			String mUrl;
			// Now define what the artist name, album name, and url are.
			if (param.mbid != null) {
				mUrl = imageFetcher.downloadImage(param.mbid);
			} else {
				mUrl = imageFetcher.downloadImage(mImageType, param.mArtistName, param.mAlbumName);
			}
			try {
				if (mUrl != null)
					bitmap = BitmapFactory.decodeStream(new URL(mUrl).openConnection().getInputStream());
				if (bitmap != null) {
					imageFetcher.addImageToCache(bitmap, param.cacheKey);
				}
			} catch (IOException e) {
				Log.w(TAG, "could not download image!");
				// proceed without bitmap
			}
		}

		// Fourth, add the new image to the cache and create drawables
		if (bitmap != null) {
			Drawable result = ImageUtils.createTransitionDrawable(imageFetcher.getResources(), bitmap);
			Drawable layerBlur = ImageUtils.createBlurredDrawable(imageFetcher.getResources(), bitmap);
			return new Result(result, layerBlur);
		}
		return null;
	}

	/**
	 *
	 */
	public static class Param {

		String cacheKey;
		@Nullable
		String mbid, mArtistName, mAlbumName;
		long albumId;

		public Param(String cacheKey, @NonNull String mAlbumName, @NonNull String mArtistName, long albumId) {
			this.cacheKey = cacheKey;
			this.mAlbumName = mAlbumName;
			this.mArtistName = mArtistName;
			this.albumId = albumId;
		}

		public Param(String cacheKey, @NonNull String mbid) {
			this.cacheKey = cacheKey;
			this.mbid = mbid;
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