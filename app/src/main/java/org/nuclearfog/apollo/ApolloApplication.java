package org.nuclearfog.apollo;

import android.app.Application;
import android.os.Build;

import org.nuclearfog.apollo.cache.ImageCache;
import org.nuclearfog.apollo.store.preferences.AppPreferences;
import org.nuclearfog.apollo.utils.ApolloUtils;

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
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M && AppPreferences.getInstance(getApplicationContext()).certificationValidationDisabled()) {
			ApolloUtils.disableSSLCertificateValidation();
		}
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
}