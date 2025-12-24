package org.nuclearfog.apollo.async.worker;

import android.content.ContentUris;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.nuclearfog.apollo.async.AsyncExecutor;
import org.nuclearfog.apollo.async.worker.ImageWorker.Param;
import org.nuclearfog.apollo.async.worker.ImageWorker.Result;
import org.nuclearfog.apollo.cache.ImageCache;
import org.nuclearfog.apollo.lookup.MusicBrainz;
import org.nuclearfog.apollo.lookup.entities.AlbumMB;
import org.nuclearfog.apollo.lookup.entities.ArtistMB;
import org.nuclearfog.apollo.lookup.entities.Artwork;
import org.nuclearfog.apollo.model.Album;
import org.nuclearfog.apollo.model.Artist;
import org.nuclearfog.apollo.store.preferences.AppPreferences;
import org.nuclearfog.apollo.utils.Constants.ImageType;
import org.nuclearfog.apollo.utils.ImageUtils;
import org.nuclearfog.apollo.utils.MusicUtils;

import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.util.List;

/**
 * Async worker to download image artworks
 *
 * @author nuclearfog
 */
public class ImageWorker extends AsyncExecutor<Param, Result> {

	private static final String TAG = "ImageWorker";

	private static final Uri URI_ARTWORK = Uri.parse("content://media/external/audio/albumart");

	private AppPreferences mPrefs;
	private ImageCache mImageCache;


	public ImageWorker(Context context) {
		super(context);
		mImageCache = ImageCache.getInstance(context);
		mPrefs = AppPreferences.getInstance(context);
	}


	@Override
	protected Result doInBackground(Param param) {
		Context context = getContext();
		if (context == null) {
			return null;
		}

		// First, check the disk cache for the image
		Bitmap bitmap = mImageCache.getCachedBitmap(param.cacheKey);

		// second, check the MediaStore database for any album art
		if (bitmap == null && param.type == ImageType.ALBUM) {
			try {
				Uri uri = ContentUris.withAppendedId(URI_ARTWORK, param.id);
				ParcelFileDescriptor descriptor = context.getContentResolver().openFileDescriptor(uri, "r");
				if (descriptor != null) {
					FileDescriptor fileDescriptor = descriptor.getFileDescriptor();
					bitmap = BitmapFactory.decodeFileDescriptor(fileDescriptor);
					mImageCache.addBitmapToCache(param.cacheKey, bitmap);
					descriptor.close();
				}
			} catch (FileNotFoundException e) {
				// proceed without file
			} catch (Exception e) {
				Log.w(TAG, "could not load local thumbnail! ID=" + param.id);
			}
		}

		// Third, by now we need to download the image
		if (bitmap == null) {
			// if MBID is not defined, search for a mbid using local MediaStore ID
			if (param.mbid == null) {
				if (param.type == ImageType.ARTIST && mPrefs.downloadMissingArtistImages()) {
					Artist artist = MusicUtils.getArtistForId(context, param.id);
					if (artist != null) {
						// fetch artist information
						ArtistMB artistMb = MusicBrainz.getArtistByName(artist.getName());
						if (artistMb != null) {
							// fetch the most recent album of the artist
							List<AlbumMB> albums = MusicBrainz.searchAlbumsByArtistId(artistMb.getId(), 1);
							if (!albums.isEmpty()) {
								param.mbid = albums.get(0).getId();
							}
						}
					}
				} else if (param.type == ImageType.ALBUM && mPrefs.downloadMissingArtwork()) {
					Album album = MusicUtils.getAlbumForId(context, param.id);
					if (album != null) {
						AlbumMB albumMb = MusicBrainz.getReleaseByName(album.getName(), album.getArtist());
						if (albumMb != null) {
							param.mbid = albumMb.getId();
						}
					}
				}
			}
			// download artwork if MBID is defined
			if (param.mbid != null) {
				Artwork artwork = MusicBrainz.getImage(param.mbid);
				if (artwork != null) {
					try {
						String mUrl = artwork.getThumbnailUrl();
						bitmap = BitmapFactory.decodeStream(new URL(mUrl).openConnection().getInputStream());
						if (bitmap != null) {
							if (param.type == ImageType.ARTWORK) {
								// don't add online artwork images to persistent cache
								mImageCache.addBitmapToMemCache(param.cacheKey, bitmap);
							} else {
								mImageCache.addBitmapToCache(param.cacheKey, bitmap);
							}
						}
					} catch (IOException e) {
						Log.w(TAG, "could not download image!");
					}
				}
			}
		}

		// Fourth, add the new image to the cache and create drawables
		if (bitmap != null) {
			Drawable result = ImageUtils.createTransitionDrawable(context.getResources(), bitmap);
			Drawable layerBlur = ImageUtils.createBlurredDrawable(context.getResources(), bitmap);
			return new Result(result, layerBlur);
		}
		return null;
	}

	/**
	 *
	 */
	public static class Param {

		final ImageType type;
		final long id;
		final String cacheKey;
		@Nullable
		String mbid = null;

		public Param(ImageType type, @NonNull String cacheKey, @NonNull String mbid) {
			this.mbid = mbid;
			this.type = type;
			this.cacheKey = cacheKey;
			id = 0;
		}

		public Param(ImageType type, @NonNull String cacheKey, long id) {
			this.id = id;
			this.type = type;
			this.cacheKey = cacheKey;
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