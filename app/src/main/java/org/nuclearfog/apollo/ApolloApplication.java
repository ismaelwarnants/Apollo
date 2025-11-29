package org.nuclearfog.apollo;

import android.app.Application;

import androidx.core.app.NotificationManagerCompat;

import org.nuclearfog.apollo.cache.ImageCache;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Used to turn off logging for jaudiotagger and free up memory when
 * {@code #onLowMemory()} is called on pre-ICS devices. On post-ICS memory is
 * released within {@link ImageCache}.
 *
 * @author Andrew Neal (andrewdneal@gmail.com)
 * @author nuclearfog
 */
public class ApolloApplication extends Application {

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onCreate() {
		super.onCreate();
		// Turn off logging for jaudiotagger.
		Logger.getLogger("org.jaudiotagger").setLevel(Level.OFF);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onLowMemory() {
		// clear image cache
		ImageCache.getInstance(this).evictAll();
		super.onLowMemory();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onTerminate() {
		try {
			// remove notification
			NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
			notificationManager.cancelAll();
		} catch (SecurityException exception) {
			// ignore
		}
		super.onTerminate();
	}
}