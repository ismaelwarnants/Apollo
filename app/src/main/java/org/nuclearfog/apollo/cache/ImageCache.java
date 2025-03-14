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

import android.app.ActivityManager;
import android.content.ComponentCallbacks2;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.os.StatFs;
import android.util.Log;
import android.util.LruCache;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.nuclearfog.apollo.utils.StringUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * This class holds the memory and disk bitmap caches.
 */
public final class ImageCache implements ComponentCallbacks2 {

	/**
	 *
	 */
	private static final String TAG = "image_cache";

	/**
	 * Default memory cache size as a percent of device memory class
	 */
	private static final float MEM_CACHE_DIVIDER = 0.25f;

	/**
	 * Default disk cache size 32 MB
	 */
	private static final int DISK_CACHE_SIZE = 1024 * 1024 * 32;

	/**
	 * Compression settings when writing images to disk cache
	 */
	private static final CompressFormat COMPRESS_FORMAT = CompressFormat.JPEG;

	/**
	 * Disk cache index to read from
	 */
	private static final int DISK_CACHE_INDEX = 0;

	/**
	 * Image compression quality
	 */
	private static final int COMPRESS_QUALITY = 90;

	/**
	 * singleton instance of this class
	 */
	private static ImageCache sInstance;

	/**
	 *
	 */
	private final Object PAUSELOCK = new Object();

	/**
	 * Used to temporarily pause the disk cache while scrolling
	 */
	public boolean mPauseDiskAccess = false;

	/**
	 * LRU cache
	 */
	@Nullable
	private LruCache<String, Bitmap> mLruCache;

	/**
	 * Disk LRU cache
	 */
	@Nullable
	private DiskLruCache mDiskCache;

	/**
	 * Constructor of <code>ImageCache</code>
	 *
	 * @param context The {@link Context} to use
	 */
	private ImageCache(Context context) {
		init(context);
	}

	/**
	 * Used to create a singleton of {@link ImageCache}
	 *
	 * @param context The {@link Context} to use
	 * @return A new instance of this class.
	 */
	public static ImageCache getInstance(Context context) {
		if (sInstance == null) {
			sInstance = new ImageCache(context.getApplicationContext());
		}
		return sInstance;
	}

	/**
	 * Check if space is available at a given path.
	 *
	 * @param path The path to check
	 * @return true if there is enough space for the cache
	 */
	private static boolean isSpaceAvailable(String path) {
		StatFs fs = new StatFs(path);
		return fs.getBlockSizeLong() * fs.getAvailableBlocksLong() > DISK_CACHE_SIZE;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onTrimMemory(int level) {
		if (level >= TRIM_MEMORY_MODERATE) {
			evictAll();
		} else if (level >= TRIM_MEMORY_BACKGROUND) {
			if (mLruCache != null) {
				mLruCache.trimToSize(mLruCache.size() / 2);
			}
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onLowMemory() {
		// Nothing to do
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onConfigurationChanged(@NonNull Configuration newConfig) {
		// Nothing to do
	}

	/**
	 * Adds a new image to the memory and disk caches
	 *
	 * @param data   The key used to store the image
	 * @param bitmap The {@link Bitmap} to cache
	 */
	public void addBitmapToCache(@NonNull String data, @NonNull Bitmap bitmap) {
		// Add to memory cache
		addBitmapToMemCache(data, bitmap);

		// Add to disk cache
		if (mDiskCache != null) {
			String key = StringUtils.hashKeyForDisk(data);
			OutputStream out = null;
			try {
				DiskLruCache.Snapshot snapshot = mDiskCache.get(key);
				if (snapshot == null) {
					DiskLruCache.Editor editor = mDiskCache.edit(key);
					if (editor != null) {
						out = editor.newOutputStream(DISK_CACHE_INDEX);
						bitmap.compress(COMPRESS_FORMAT, COMPRESS_QUALITY, out);
						editor.commit();
						flush();
					}
				} else {
					snapshot.getInputStream(DISK_CACHE_INDEX).close();
				}
			} catch (IOException e) {
				Log.e(TAG, "addBitmapToCache()" + e);
			} finally {
				try {
					if (out != null) {
						out.close();
					}
				} catch (IOException | IllegalStateException e) {
					Log.e(TAG, "addBitmapToCache(): failed to close output stream");
				}
			}
		}
	}

	/**
	 * Called to add a new image to the memory cache
	 *
	 * @param data   The key identifier
	 * @param bitmap The {@link Bitmap} to cache
	 */
	public void addBitmapToMemCache(@NonNull String data, @NonNull Bitmap bitmap) {
		// Add to memory cache
		if (mLruCache != null && getBitmapFromMemCache(data) == null) {
			mLruCache.put(data, bitmap);
		}
	}

	/**
	 * Fetches a cached image from the memory cache
	 *
	 * @param data Unique identifier for which item to get
	 * @return The {@link Bitmap} if found in cache, null otherwise
	 */
	public Bitmap getBitmapFromMemCache(@NonNull String data) {
		if (mLruCache != null) {
			return mLruCache.get(data);
		}
		return null;
	}

	/**
	 * Fetches a cached image from the disk cache
	 *
	 * @param data Unique identifier for which item to get
	 * @return The {@link Bitmap} if found in cache, null otherwise
	 */
	public Bitmap getBitmapFromDiskCache(@NonNull String data) {
		// Check in the memory cache here to avoid going to the disk cache less often
		if (getBitmapFromMemCache(data) != null) {
			return getBitmapFromMemCache(data);
		}

		synchronized (PAUSELOCK) {
			String key = StringUtils.hashKeyForDisk(data);
			if (mDiskCache != null) {
				InputStream inputStream = null;
				try {
					DiskLruCache.Snapshot snapshot = mDiskCache.get(key);
					if (snapshot != null) {
						inputStream = snapshot.getInputStream(DISK_CACHE_INDEX);
						if (inputStream != null) {
							Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
							if (bitmap != null) {
								return bitmap;
							}
						}
					}
				} catch (IOException e) {
					Log.e(TAG, "getBitmapFromDiskCache():" + e);
				} finally {
					try {
						if (inputStream != null) {
							inputStream.close();
						}
					} catch (IOException e) {
						Log.e(TAG, "getBitmapFromDiskCache(): failed to close output stream");
					}
				}
			}
			return null;
		}
	}

	/**
	 * Tries to return a cached image from memory cache before fetching from the
	 * disk cache
	 *
	 * @param data Unique identifier for which item to get
	 * @return The {@link Bitmap} if found in cache, null otherwise
	 */
	@Nullable
	public Bitmap getCachedBitmap(@NonNull String data) {
		Bitmap cachedImage = getBitmapFromDiskCache(data);
		if (cachedImage != null) {
			addBitmapToMemCache(data, cachedImage);
			return cachedImage;
		}
		return null;
	}

	/**
	 * flush() is called to synchronize up other methods that are accessing the
	 * cache first
	 */
	public void flush() {
		new Thread(() -> {
			if (mDiskCache != null) {
				try {
					if (!mDiskCache.isClosed()) {
						mDiskCache.flush();
					}
				} catch (IOException e) {
					Log.e(TAG, "flush():" + e);
				}
			}
		}).start();
	}

	/**
	 * Clears the disk and memory caches
	 */
	public void clearCaches() {
		new Thread(() -> {
			// Clear the disk cache
			try {
				if (mDiskCache != null) {
					mDiskCache.delete();
					mDiskCache = null;
				}
			} catch (IOException e) {
				Log.e(TAG, "error cleaning disk cache" + e);
			}
			// Clear the memory cache
			evictAll();
		}).start();
	}

	/**
	 * Evicts all of the items from the memory cache
	 */
	public void evictAll() {
		Log.d(TAG, "cleaning image cache");
		if (mLruCache != null) {
			mLruCache.evictAll();
		}
	}

	/**
	 * @param key The key used to identify which cache entries to delete.
	 */
	public void removeFromCache(@NonNull String key) {
		// Remove the Lru entry
		if (mLruCache != null) {
			mLruCache.remove(key);
		}
		try {
			// Remove the disk entry
			if (mDiskCache != null) {
				mDiskCache.remove(StringUtils.hashKeyForDisk(key));
			}
		} catch (IOException e) {
			Log.e(TAG, "remove():" + e);
		}
		flush();
	}

	/**
	 * Used to temporarily pause the disk cache while the user is scrolling to
	 * improve scrolling.
	 *
	 * @param pause True to temporarily pause the disk cache, false otherwise.
	 */
	public void setPauseDiskCache(boolean pause) {
		synchronized (PAUSELOCK) {
			if (mPauseDiskAccess != pause) {
				mPauseDiskAccess = pause;
				if (!pause) {
					PAUSELOCK.notify();
				}
			}
		}
	}

	/**
	 * @return True if the user is scrolling, false otherwise.
	 */
	public boolean isDiskCachePaused() {
		return mPauseDiskAccess;
	}

	/**
	 * Initialize the cache, providing all parameters.
	 *
	 * @param context The {@link Context} to use
	 */
	private void init(Context context) {
		// create cache folder
		File cacheFolder = context.getExternalCacheDir();
		if (cacheFolder == null)
			cacheFolder = context.getCacheDir();
		final File folder = new File(cacheFolder, TAG);
		if (!folder.exists()) {
			folder.mkdirs();
		}

		// Initialize the disk cache in a background thread
		new Thread(() -> {
			if ((mDiskCache == null || mDiskCache.isClosed()) && isSpaceAvailable(folder.getPath())) {
				try {
					mDiskCache = DiskLruCache.open(folder, 1, 1, DISK_CACHE_SIZE);
				} catch (IOException e) {
					Log.e(TAG, "init() failed", e);
				}
			}
		}).start();

		// Set up the memory cache
		ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
		int lruCacheSize;
		if (activityManager != null) {
			lruCacheSize = Math.round(MEM_CACHE_DIVIDER * activityManager.getMemoryClass() * 1024 * 1024);
		} else {
			lruCacheSize = 16000000;
		}
		mLruCache = new LruCache<>(lruCacheSize);
		// Release some memory as needed
		context.registerComponentCallbacks(this);
	}
}