/*
 * Copyright (C) 2012 Andrew Neal Licensed under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law
 * or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */

package org.nuclearfog.apollo.cache;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.res.ResourcesCompat;

import org.nuclearfog.apollo.R;
import org.nuclearfog.apollo.model.Album;
import org.nuclearfog.apollo.utils.Constants.ImageType;
import org.nuclearfog.apollo.utils.ImageUtils;
import org.nuclearfog.apollo.utils.StringUtils;

import java.io.IOException;

/**
 * This class wraps up completing some arbitrary long running work when loading
 * a {@link Bitmap} to an {@link ImageView}. It handles things like using a
 * memory and disk cache, running the work in a background thread and setting a
 * placeholder image.
 *
 * @author nuclearfog
 */
public class ImageFetcher {

	private static final String TAG = "ImageWorker";

	/**
	 * size of the artist/album art of the notification image
	 */
	private static final int NOTIFICATION_SIZE = 200;


	/**
	 * The Context to use
	 */
	private Context mContext;

	/**
	 * Disk and memory caches
	 */
	private ImageCache mImageCache;

	/**
	 *
	 */
	public ImageFetcher(Context context) {
		mContext = context.getApplicationContext();
		mImageCache = ImageCache.getInstance(mContext);
	}

	/**
	 * flush() is called to synchronize up other methods that are accessing the
	 * cache first
	 */
	public void flush() {
		mImageCache.flush();
	}

	/**
	 * @param pause True to temporarily pause the disk cache, false otherwise.
	 */
	public void setPauseDiskCache(boolean pause) {
		mImageCache.setPauseDiskCache(pause);
	}

	/**
	 * Download album/artist artwork from online database using MBID
	 *
	 * @param mbid      MusicBrainz ID
	 * @param imageView imageview to show the artwork
	 */
	public void loadArtworkImage(String mbid, ImageView imageView) {
		String key = StringUtils.generateCacheKey(ImageType.ARTWORK, mbid);
		setDefaultImage(imageView);
		if (executePotentialWork(key, imageView) && !mImageCache.isDiskCachePaused()) {
			// Otherwise run the worker task
			ImageAsyncTag asyncTag = new ImageAsyncTag(mContext, key, imageView);
			imageView.setTag(asyncTag);
			asyncTag.run(ImageType.ARTWORK, mbid);
		}
	}

	/**
	 * get album image from cache
	 *
	 * @param albumId local MediaStore ID of the album
	 * @return album image bitmap
	 */
	public Bitmap getAlbumImage(long albumId) {
		String key = StringUtils.generateCacheKey(ImageType.ALBUM, albumId);
		Bitmap bitmap = mImageCache.getCachedBitmap(key);
		if (bitmap == null)
			return getDefaultArtwork();
		return bitmap;
	}

	/**
	 * add album image to cache
	 *
	 * @param albumId local MediaStore ID of the album
	 * @param uri     local Uri to image file or null to remove from cache
	 */
	public void setAlbumImage(long albumId, @Nullable Uri uri) {
		String key = StringUtils.generateCacheKey(ImageType.ALBUM, albumId);
		addImageToCache(key, uri);
	}

	/**
	 * add album image to cache
	 *
	 * @param albumId local MediaStore ID of the album
	 * @param bitmap  image bitmap or null to remove existing entry from cache
	 */
	public void setAlbumImage(long albumId, @Nullable Bitmap bitmap) {
		String key = StringUtils.generateCacheKey(ImageType.ALBUM, albumId);
		addImageToCache(key, bitmap);
	}

	/**
	 * load album images asynchronously into the imageview(s)
	 *
	 * @param albumId    local MediaStore ID of the album
	 * @param imageViews imageview(s) to attach the images
	 */
	public void loadAlbumImage(long albumId, ImageView... imageViews) {
		String key = StringUtils.generateCacheKey(ImageType.ALBUM, albumId);
		loadImage(ImageType.ALBUM, key, albumId, imageViews);
	}

	/**
	 * get artist image from cache
	 *
	 * @param artistId local MediaStore ID of the artist
	 * @return artist image bitmap
	 */
	public Bitmap getArtistImage(long artistId) {
		String key = StringUtils.generateCacheKey(ImageType.ARTIST, artistId);
		Bitmap bitmap = mImageCache.getCachedBitmap(key);
		if (bitmap == null)
			return getDefaultArtwork();
		return bitmap;
	}

	/**
	 * add artist image to cache
	 *
	 * @param artistId local MediaStore ID of the artist
	 * @param uri      local Uri to image file or null to remove from cache
	 */
	public void setArtistImage(long artistId, @Nullable Uri uri) {
		String key = StringUtils.generateCacheKey(ImageType.ARTIST, artistId);
		addImageToCache(key, uri);
	}

	/**
	 * add artist image to cache
	 *
	 * @param artistId local MediaStore ID of the artist
	 * @param bitmap   image bitmap or null to remove existing entry from cache
	 */
	public void setArtistImage(long artistId, @Nullable Bitmap bitmap) {
		String key = StringUtils.generateCacheKey(ImageType.ARTIST, artistId);
		addImageToCache(key, bitmap);
	}

	/**
	 * load artist image asynchronously into imageview
	 *
	 * @param artistId  local MediaStore ID of the artist
	 * @param imageView imageview to attach the image
	 */
	public void loadArtistImage(long artistId, ImageView imageView) {
		String key = StringUtils.generateCacheKey(ImageType.ARTIST, artistId);
		loadImage(ImageType.ARTIST, key, artistId, imageView);
	}

	/**
	 * get genre image thumbnail
	 *
	 * @param ids MediaStore IDs of a genre
	 * @return thumbnail bitmap
	 */
	public Bitmap getGenreImage(long[] ids) {
		String key = StringUtils.generateCacheKey(ImageType.GENRE, ids);
		Bitmap bitmap = mImageCache.getCachedBitmap(key);
		if (bitmap == null)
			return getDefaultArtwork();
		return bitmap;
	}

	/**
	 * caches the genre image
	 *
	 * @param ids genre IDs
	 * @param uri local Uri of the image file
	 */
	public void setGenreImage(long[] ids, @Nullable Uri uri) {
		String key = StringUtils.generateCacheKey(ImageType.GENRE, ids);
		if (uri != null) {
			addImageToCache(key, uri);
		} else {
			mImageCache.removeFromCache(key);
		}
	}

	/**
	 * loads the genre image into imageview asynchronously
	 *
	 * @param ids       genre IDs
	 * @param imageView imageview to attach the image
	 */
	public void loadGenreImage(long[] ids, ImageView imageView) {
		String key = StringUtils.generateCacheKey(ImageType.GENRE, ids);
		loadImage(ImageType.GENRE, key, 0, imageView);
	}

	/**
	 * loads the playlist image from cache
	 *
	 * @param id MediaStore playlist ID
	 * @return image bitmap
	 */
	public Bitmap getPlaylistImage(long id) {
		String key = StringUtils.generateCacheKey(ImageType.PLAYLIST, id);
		Bitmap bitmap = mImageCache.getCachedBitmap(key);
		if (bitmap == null)
			return getDefaultArtwork();
		return bitmap;
	}

	/**
	 * caches the playlist image
	 *
	 * @param playlistId ID of the playlist
	 * @param uri        local Uri of the image file
	 */
	public void setPlaylistImage(long playlistId, @Nullable Uri uri) {
		String key = StringUtils.generateCacheKey(ImageType.PLAYLIST, playlistId);
		if (uri != null) {
			addImageToCache(key, uri);
		} else {
			mImageCache.removeFromCache(key);
		}
	}

	/**
	 * loads the playlist image into imageview asynchronously
	 *
	 * @param playlistId ID of the playlist
	 * @param imageView  imageview to attach the image
	 */
	public void loadPlaylistImage(long playlistId, ImageView imageView) {
		String key = StringUtils.generateCacheKey(ImageType.PLAYLIST, playlistId);
		loadImage(ImageType.PLAYLIST, key, playlistId, imageView);
	}

	/**
	 * caches the music folder image
	 *
	 * @param folder music folder path
	 * @param uri    local Uri of the image file
	 */
	public void setFolderImage(String folder, @Nullable Uri uri) {
		String key = StringUtils.generateCacheKey(ImageType.FOLDER, folder);
		if (uri != null) {
			addImageToCache(key, uri);
		} else {
			mImageCache.removeFromCache(key);
		}
	}

	/**
	 * loads the folder image into imageview asynchronously
	 *
	 * @param folder    music folder path
	 * @param imageView imageview to attach the image
	 */
	public void loadFolderImage(String folder, ImageView imageView) {
		String key = StringUtils.generateCacheKey(ImageType.FOLDER, folder);
		loadImage(ImageType.FOLDER, key, 0, imageView);
	}

	/**
	 * get album artwork from app image cache
	 * if not found, return default artwork
	 *
	 * @param album album to get the artwork from
	 * @return a scaled down {@link Bitmap}
	 */
	@NonNull
	public Bitmap getAlbumArtwork(@NonNull Album album) {
		String cacheKey = StringUtils.generateCacheKey(ImageType.ALBUM, album.getId());
		Bitmap artwork = mImageCache.getBitmapFromDiskCache(cacheKey);
		if (artwork == null) {
			artwork = getDefaultArtwork();
		}
		// scale down image
		return Bitmap.createScaledBitmap(artwork, NOTIFICATION_SIZE, NOTIFICATION_SIZE, false);
	}

	/**
	 * set default artwork
	 */
	public void setDefaultImage(ImageView... imageViews) {
		imageViews[0].setImageResource(R.drawable.default_artwork);
		if (imageViews.length > 1) {
			imageViews[1].setImageResource(0);
		}
	}

	/**
	 * Called to fetch the artist or album art.
	 *
	 * @param type       Type of image to load into imageview
	 * @param key        cache key to identify cached images
	 * @param id         MediaStore ID of album/artist used with {@link ImageType#ALBUM,ImageType#ARTIST}
	 * @param imageViews at least one ImageView to set the loaded image
	 */
	private void loadImage(ImageType type, String key, long id, ImageView... imageViews) {
		// reset artwork
		setDefaultImage(imageViews);
		// First, check the cache for the image
		Bitmap lruBitmap = mImageCache.getBitmapFromMemCache(key);
		if (lruBitmap != null) {
			// Bitmap found in memory cache
			imageViews[0].setImageBitmap(lruBitmap);
			// add blurring to the second image if defined
			if (imageViews.length > 1) {
				Bitmap blur = ImageUtils.createBlurredBitmap(lruBitmap);
				imageViews[1].setImageBitmap(blur);
			}
		}
		// check storage for image or download
		else if (executePotentialWork(key, imageViews[0]) && !mImageCache.isDiskCachePaused()) {
			// Otherwise run the worker task
			ImageAsyncTag asyncTag = new ImageAsyncTag(mContext, key, imageViews);
			imageViews[0].setTag(asyncTag);
			asyncTag.run(type, id);
		}
	}

	/**
	 * Returns true if the current work has been canceled or if there was no
	 * work in progress on this image view. Returns false if the work in
	 * progress deals with the same data. The work is not stopped in that case.
	 */
	private boolean executePotentialWork(String key, ImageView imageView) {
		if (imageView != null) {
			Object drawable = imageView.getTag();
			if (drawable instanceof ImageAsyncTag) {
				ImageAsyncTag asyncDrawable = (ImageAsyncTag) drawable;
				// The same work is already in progress
				if (!asyncDrawable.getTag().equals(key)) {
					// cancel worker to load a new image
					asyncDrawable.cancel();
				} else {
					return asyncDrawable.isFinished();
				}
			}
		}
		return true;
	}

	/**
	 * add image to local cache using Uri
	 *
	 * @param uri local Uri to image or null to remove image
	 * @param key cache key used to identify image
	 */
	private void addImageToCache(String key, @Nullable Uri uri) {
		if (uri != null) {
			try {
				Bitmap bitmap = MediaStore.Images.Media.getBitmap(mContext.getContentResolver(), uri);
				addImageToCache(key, bitmap);
			} catch (IOException exception) {
				Log.e(TAG, "could not load local image to cache!", exception);
			}
		} else {
			mImageCache.removeFromCache(key);
		}
	}

	/**
	 * add image to local cache using Uri
	 *
	 * @param bitmap image bitmap to add to cache
	 * @param key    cache key used to identify image
	 */
	private void addImageToCache(String key, @Nullable Bitmap bitmap) {
		if (bitmap != null) {
			mImageCache.addBitmapToCache(key, bitmap);
		} else {
			mImageCache.removeFromCache(key);
		}
	}

	/**
	 * get default artwork if no artwork is found
	 *
	 * @return bitmap of the default artwork
	 */
	@NonNull
	@SuppressWarnings("ConstantConditions")
	private Bitmap getDefaultArtwork() {
		Drawable bitmap = ResourcesCompat.getDrawable(mContext.getResources(), R.drawable.default_artwork, null);
		return ((BitmapDrawable) bitmap).getBitmap();
	}
}