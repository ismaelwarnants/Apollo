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

import android.content.ContentUris;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.res.ResourcesCompat;

import org.nuclearfog.apollo.R;
import org.nuclearfog.apollo.lookup.MusicBrainz;
import org.nuclearfog.apollo.lookup.entities.AlbumMB;
import org.nuclearfog.apollo.lookup.entities.ArtistMB;
import org.nuclearfog.apollo.lookup.entities.Artwork;
import org.nuclearfog.apollo.model.Album;
import org.nuclearfog.apollo.utils.Constants.ImageType;
import org.nuclearfog.apollo.utils.ImageUtils;
import org.nuclearfog.apollo.utils.PreferenceUtils;
import org.nuclearfog.apollo.utils.StringUtils;

import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;

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
	 * The {@link Uri} used to retrieve album art
	 */
	private static final Uri URI_ARTWORK = Uri.parse("content://media/external/audio/albumart");

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
	 * @return application context
	 */
	@NonNull
	public Context getContext() {
		return mContext;
	}

	/**
	 * @return resources associated with the application context
	 */
	public Resources getResources() {
		return mContext.getResources();
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
	 * get album image from cache
	 *
	 * @param artist artist name
	 * @param album  album name
	 * @return artist image bitmap
	 */
	public Bitmap getAlbumImage(String album, String artist) {
		String key = StringUtils.generateCacheKey(ImageType.ALBUM, album, artist);
		Bitmap bitmap = getCachedBitmap(key);
		if (bitmap == null)
			return getDefaultArtwork();
		return bitmap;
	}

	/**
	 * cache the album image
	 *
	 * @param album  album name
	 * @param artist artist name of the album
	 * @param uri    local Uri to image file
	 */
	public void setAlbumImage(String album, String artist, @Nullable Uri uri) {
		String key = StringUtils.generateCacheKey(ImageType.ALBUM, album, artist);
		if (uri != null) {
			addImageToCache(uri, key);
		} else {
			mImageCache.removeFromCache(key);
		}
	}

	/**
	 * load album images asynchronously into the imageview(s)
	 *
	 * @param album      album information to fetch the images from
	 * @param imageViews imageview(s) to attach the images
	 */
	public void loadAlbumImage(@Nullable Album album, ImageView... imageViews) {
		if (imageViews.length > 0) {
			if (album != null) {
				loadAlbumImage(album.getArtist(), album.getName(), album.getId(), imageViews);
			} else {
				setDefaultImage(imageViews);
			}
		}
	}

	/**
	 * load album images asynchronously into the imageview(s)
	 *
	 * @param artist     artist name of the album
	 * @param album      album name
	 * @param id         MediaStore ID of the album
	 * @param imageViews imageview(s) to attach the images
	 */
	public void loadAlbumImage(String artist, String album, long id, ImageView... imageViews) {
		String key = StringUtils.generateCacheKey(ImageType.ALBUM, album, artist);
		loadImage(key, artist, album, id, ImageType.ALBUM, imageViews);
	}

	/**
	 *
	 */
	public void loadArtworkImage(String mbid, ImageView imageView) {
		String key = StringUtils.generateCacheKey(ImageType.ARTWORK, mbid);
		setDefaultImage(imageView);
		if (executePotentialWork(key, imageView) && !mImageCache.isDiskCachePaused()) {
			// Otherwise run the worker task
			ImageAsyncTag asyncTag = new ImageAsyncTag(this, key, ImageType.ARTWORK, imageView);
			imageView.setTag(asyncTag);
			asyncTag.run(mbid);
		}
	}

	/**
	 * get artist image from cache
	 *
	 * @param artist artist name
	 * @return artist image bitmap
	 */
	public Bitmap getArtistImage(String artist) {
		String key = StringUtils.generateCacheKey(ImageType.ARTIST, artist);
		Bitmap bitmap = getCachedBitmap(key);
		if (bitmap == null)
			return getDefaultArtwork();
		return bitmap;
	}

	/**
	 * cache artist image
	 *
	 * @param artist name of the artist
	 * @param uri    local Uri of the image file
	 */
	public void setArtistImage(String artist, @Nullable Uri uri) {
		String key = StringUtils.generateCacheKey(ImageType.ARTIST, artist);
		if (uri != null) {
			addImageToCache(uri, key);
		} else {
			mImageCache.removeFromCache(key);
		}
	}

	/**
	 * load artist image asynchronously into imageview
	 *
	 * @param artist    artist name
	 * @param imageView imageview to attach the image
	 */
	public void loadArtistImage(String artist, ImageView imageView) {
		String key = StringUtils.generateCacheKey(ImageType.ARTIST, artist);
		loadImage(key, artist, "", 0, ImageType.ARTIST, imageView);
	}

	/**
	 * caches the genre image
	 *
	 * @param genre genre name
	 * @param uri   local Uri of the image file
	 */
	public void setGenreImage(String genre, @Nullable Uri uri) {
		String key = StringUtils.generateCacheKey(ImageType.GENRE, genre);
		if (uri != null) {
			addImageToCache(uri, key);
		} else {
			mImageCache.removeFromCache(key);
		}
	}

	/**
	 * loads the genre image into imageview asynchronously
	 *
	 * @param genre     genre name
	 * @param imageView imageview to attach the image
	 */
	public void loadGenreImage(String genre, ImageView imageView) {
		String key = StringUtils.generateCacheKey(ImageType.GENRE, genre);
		loadImage(key, "", "", 0, ImageType.GENRE, imageView);
	}

	/**
	 * caches the playlist image
	 *
	 * @param id  ID of the playlist
	 * @param uri local Uri of the image file
	 */
	public void setPlaylistImage(long id, @Nullable Uri uri) {
		String key = StringUtils.generateCacheKey(ImageType.PLAYLIST, Long.toString(id));
		if (uri != null) {
			addImageToCache(uri, key);
		} else {
			mImageCache.removeFromCache(key);
		}
	}

	/**
	 * loads the genre image into imageview asynchronously
	 *
	 * @param id        ID of the playlist
	 * @param imageView imageview to attach the image
	 */
	public void loadPlaylistImage(long id, ImageView imageView) {
		String key = StringUtils.generateCacheKey(ImageType.PLAYLIST, Long.toString(id));
		loadImage(key, "", "", 0, ImageType.PLAYLIST, imageView);
	}

	/**
	 * caches the music folder image
	 *
	 * @param folder path of the music folder
	 * @param uri    local Uri of the image file
	 */
	public void setFolderImage(String folder, @Nullable Uri uri) {
		String key = StringUtils.generateCacheKey(ImageType.FOLDER, folder);
		if (uri != null) {
			addImageToCache(uri, key);
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
		loadImage(key, "", "", 0, ImageType.FOLDER, imageView);
	}

	/**
	 * add bitmap to cache
	 *
	 * @param bitmap bitmap image to cache or null to remove the image
	 * @param key    cache key used to identify the bitmap
	 */
	public void addImageToCache(@Nullable Bitmap bitmap, String key) {
		if (bitmap != null) {
			mImageCache.addBitmapToCache(key, bitmap);
		} else {
			mImageCache.removeFromCache(key);
		}
	}

	/**
	 * get image bitmap directly from cache using cache key
	 *
	 * @param key cache key to find the image
	 * @return image matching the keyword or null if not found
	 */
	@Nullable
	public Bitmap getCachedBitmap(String key) {
		return mImageCache.getCachedBitmap(key);
	}

	/**
	 * fetch album/artist image using online service
	 *
	 * @param artistName name of the artist/band
	 * @param albumName  name of the album
	 * @param imageType  type of the image {@link ImageType#ALBUM,ImageType#ARTIST}
	 * @return url of the image
	 */
	public String downloadImage(ImageType imageType, String artistName, String albumName) {
		String mbid = null;
		if (imageType == ImageType.ARTIST) {
			if (PreferenceUtils.getInstance(mContext).downloadMissingArtistImages()
					&& !TextUtils.isEmpty(artistName) && artistName.length() > 2) {
				// fetch artist information
				ArtistMB artist = MusicBrainz.getArtistByName(artistName);
				if (artist != null) {
					// fetch the most recent album of the artist
					List<AlbumMB> albums = MusicBrainz.searchAlbumsByArtistId(artist.getId(), 1);
					if (!albums.isEmpty()) {
						mbid = albums.get(0).getId();
					}
				}
			}
		} else if (imageType == ImageType.ALBUM) {
			if (PreferenceUtils.getInstance(mContext).downloadMissingArtwork()
					&& !TextUtils.isEmpty(albumName) && albumName.length() > 2) {
				AlbumMB album = MusicBrainz.getReleaseByName(albumName, artistName);
				if (album != null) {
					mbid = album.getId();
				}
			}
		}
		if (mbid != null) {
			return downloadImage(mbid);
		}
		return null;
	}

	/**
	 * download image artwork using mbid
	 *
	 * @param mbid musicbrainz ID
	 * @return url of the image to download
	 */
	public String downloadImage(@NonNull String mbid) {
		Artwork artwork = MusicBrainz.getImage(mbid);
		if (artwork != null) {
			return artwork.getThumbnailUrl();
		}
		return null;
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
		String cacheKey = StringUtils.generateCacheKey(ImageType.ALBUM, album.getName(), album.getArtist());
		Bitmap artwork = mImageCache.getBitmapFromDiskCache(cacheKey);
		if (artwork == null) {
			artwork = getDefaultArtwork();
		}
		// scale down image
		return Bitmap.createScaledBitmap(artwork, NOTIFICATION_SIZE, NOTIFICATION_SIZE, false);
	}

	/**
	 * Used to fetch the artwork for an album locally from the user's device
	 *
	 * @param albumId ID of the album to get the artwork from
	 * @return The artwork for an album
	 */
	@Nullable
	public Bitmap getAlbumArtwork(long albumId) {
		Bitmap artwork = null;
		try {
			Uri uri = ContentUris.withAppendedId(URI_ARTWORK, albumId);
			ParcelFileDescriptor descriptor = mContext.getContentResolver().openFileDescriptor(uri, "r");
			if (descriptor != null) {
				FileDescriptor fileDescriptor = descriptor.getFileDescriptor();
				artwork = BitmapFactory.decodeFileDescriptor(fileDescriptor);
				descriptor.close();
			}
		} catch (FileNotFoundException e) {
			// proceed if no album artwork was found
		} catch (Exception e) {
			Log.w(TAG, "error while loading album art", e);
		}
		return artwork;
	}

	/**
	 * Called to fetch the artist or album art.
	 *
	 * @param key        The unique identifier for the image.
	 * @param artistName The artist name.
	 * @param albumName  The album name.
	 * @param albumId    The album ID.
	 * @param imageViews The {@link ImageView} used to set the cached {@link Bitmap}. A second image is optional and will be used to add blurring effect
	 * @param imageType  The type of image URL to fetch for.
	 */
	private void loadImage(String key, String artistName, String albumName, long albumId, ImageType imageType, ImageView... imageViews) {
		if (key != null && imageViews.length > 0) {
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
				ImageAsyncTag asyncTag = new ImageAsyncTag(this, key, imageType, imageViews);
				imageViews[0].setTag(asyncTag);
				asyncTag.run(artistName, albumName, albumId);
			}
		}
	}

	/**
	 * set default artwork
	 *
	 * @param imageViews imageview to set the default artwork
	 */
	private void setDefaultImage(ImageView... imageViews) {
		imageViews[0].setImageResource(R.drawable.default_artwork);
		if (imageViews.length > 1) {
			imageViews[1].setImageResource(0);
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
	private void addImageToCache(Uri uri, String key) {
		try {
			Bitmap bitmap = MediaStore.Images.Media.getBitmap(mContext.getContentResolver(), uri);
			addImageToCache(bitmap, key);
		} catch (IOException exception) {
			Log.e(TAG, "could not load local image to cache!", exception);
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