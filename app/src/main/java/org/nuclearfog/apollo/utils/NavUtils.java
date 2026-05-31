package org.nuclearfog.apollo.utils;

import android.app.Activity;
import android.app.SearchManager;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.media.audiofx.AudioEffect;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.provider.Settings;
import android.util.Log;

import androidx.annotation.NonNull;

import org.nuclearfog.apollo.BuildConfig;
import org.nuclearfog.apollo.R;
import org.nuclearfog.apollo.model.Album;
import org.nuclearfog.apollo.ui.activities.AudioPlayerActivity;
import org.nuclearfog.apollo.ui.activities.HomeActivity;
import org.nuclearfog.apollo.ui.activities.ProfileActivity;
import org.nuclearfog.apollo.ui.activities.SearchActivity;
import org.nuclearfog.apollo.ui.activities.SettingsActivity;

/**
 * Various navigation helpers.
 *
 * @author Andrew Neal (andrewdneal@gmail.com)
 * @author nuclearfog
 */
public final class NavUtils {

	private static final String TAG = "NavUtils";

	/**
	 * Opens the profile of an artist.
	 *
	 * @param artist_name The name of the artist
	 */
	public static void openArtistProfile(Activity activity, String artist_name) {
		long id = MusicUtils.getIdForArtist(activity, artist_name);
		if (id != -1) {
			Bundle bundle = new Bundle();
			bundle.putLong(Constants.ID, id);
			bundle.putString(Constants.MIME_TYPE, MediaStore.Audio.Artists.CONTENT_TYPE);
			bundle.putString(Constants.ARTIST_NAME, artist_name);
			// Create the intent to launch the profile activity
			Intent intent = new Intent(activity, ProfileActivity.class);
			intent.putExtras(bundle);
			activity.startActivity(intent);
		}
	}

	/**
	 * Opens the profile of an album.
	 *
	 * @param album Album to open
	 */
	public static void openAlbumProfile(Activity activity, @NonNull Album album) {
		Bundle bundle = new Bundle();
		bundle.putString(Constants.ALBUM_YEAR, album.getRelease());
		bundle.putString(Constants.ARTIST_NAME, album.getArtist());
		bundle.putString(Constants.MIME_TYPE, MediaStore.Audio.Albums.CONTENT_TYPE);
		bundle.putLong(Constants.ID, album.getId());
		bundle.putString(Constants.NAME, album.getName());
		// Create the intent to launch the profile activity
		Intent intent = new Intent(activity, ProfileActivity.class);
		intent.putExtras(bundle);
		activity.startActivity(intent);
	}

	/**
	 * open audio player activity
	 */
	public static void openAudioPlayer(Activity activity, boolean actionBackPress) {
		Intent intent = new Intent(activity, AudioPlayerActivity.class);
		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		intent.putExtra(AudioPlayerActivity.KEY_BACK_PRESS_CLOSE, actionBackPress);
		activity.startActivity(intent);
	}

	/**
	 * Opens the sound effects panel or DSP manager in CM
	 */
	public static void openEffectsPanel(Activity activity) {
		int sessionId = MusicUtils.getAudioSessionId(activity);
		if (sessionId != 0) {
			Intent effects = new Intent(AudioEffect.ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL);
			effects.putExtra(AudioEffect.EXTRA_AUDIO_SESSION, sessionId);
			try {
				activity.startActivity(effects);
			} catch (ActivityNotFoundException exception) {
				ApolloUtils.showAlertToast(activity, R.string.no_effects_for_you);
				Log.w(TAG, "couldn't open external equalizer!", exception);
			}
		} else {
			ApolloUtils.showAlertToast(activity, R.string.no_effects_for_you);
			Log.w(TAG, "invalid audio session!");
		}
	}

	/**
	 * open battery optimization page of the Android system
	 */
	public static void openBatteryPage(Activity activity) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
			Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
			intent.setData(Uri.parse("package:" + BuildConfig.APPLICATION_ID));
			try {
				activity.startActivity(intent);
			} catch (Exception exception) {
				Log.d(TAG, "could not open battery optimization settings");
			}
		}
	}

	/**
	 * Opens to {@link SettingsActivity}.
	 */
	public static void openSettings(Activity activity) {
		Intent intent = new Intent(activity, SettingsActivity.class);
		activity.startActivity(intent);
	}

	/**
	 * Opens to {@link SearchActivity}.
	 *
	 * @param query The search query.
	 */
	public static void openSearch(Activity activity, String query) {
		Intent intent = new Intent(activity, SearchActivity.class);
		intent.putExtra(SearchManager.QUERY, query);
		activity.startActivity(intent);
	}

	/**
	 * Opens to {@link HomeActivity}.
	 *
	 * @param activity The {@link Activity} to replace with home activity
	 */
	public static void goHome(Activity activity) {
		Intent intent = new Intent(activity, HomeActivity.class);
		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		activity.startActivity(intent);
		activity.finish();
	}

	/**
	 * closes app and stops playback
	 */
	public static void closeApp(Activity activity) {
		MusicUtils.releaseService(activity);
		activity.finishAffinity();
	}
}