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

package org.nuclearfog.apollo.utils;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.graphics.drawable.Icon;
import android.media.audiofx.AudioEffect;
import android.net.Uri;
import android.os.Build;
import android.os.PowerManager;
import android.provider.MediaStore;
import android.provider.Settings;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager.LayoutParams;
import android.widget.AbsListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;

import org.nuclearfog.apollo.BuildConfig;
import org.nuclearfog.apollo.R;
import org.nuclearfog.apollo.cache.ImageFetcher;
import org.nuclearfog.apollo.ui.activities.ShortcutActivity;
import org.nuclearfog.apollo.ui.appmsg.AppMsg;
import org.nuclearfog.apollo.ui.dialogs.BatteryOptDialog;

import java.io.File;
import java.util.List;

/**
 * Mostly general and UI helpers.
 *
 * @author Andrew Neal (andrewdneal@gmail.com)
 */
public final class ApolloUtils {

	private static final String TAG = "ApolloUtils";

	/* This class is never initiated */
	private ApolloUtils() {
	}

	/**
	 * Used to determine if the device is currently in landscape mode
	 *
	 * @param context The {@link Context} to use.
	 * @return True if the device is in landscape mode, false otherwise.
	 */
	public static boolean isLandscape(@Nullable Context context) {
		if (context != null) {
			int orientation = context.getResources().getConfiguration().orientation;
			return orientation == Configuration.ORIENTATION_LANDSCAPE;
		}
		return false;
	}

	/**
	 * Display a {@link Toast} letting the user know what an item does when long
	 * pressed.
	 *
	 * @param view The {@link View} to copy the content description from.
	 */
	public static void showCheatSheet(View view) {
		int[] screenPos = new int[2]; // origin is device display
		Rect displayFrame = new Rect(); // includes decorations (e.g.
		// status bar)
		view.getLocationOnScreen(screenPos);
		view.getWindowVisibleDisplayFrame(displayFrame);

		Context context = view.getContext();
		int viewWidth = view.getWidth();
		int viewHeight = view.getHeight();
		int viewCenterX = screenPos[0] + viewWidth / 2;
		int screenWidth = context.getResources().getDisplayMetrics().widthPixels;
		int estimatedToastHeight = (int) (48 * context.getResources().getDisplayMetrics().density);
		Toast cheatSheet = Toast.makeText(context, view.getContentDescription(), Toast.LENGTH_SHORT);
		boolean showBelow = screenPos[1] < estimatedToastHeight;
		if (showBelow) {
			// Show below
			// Offsets are after decorations (e.g. status bar) are factored in
			cheatSheet.setGravity(Gravity.TOP | Gravity.CENTER_HORIZONTAL, viewCenterX
					- screenWidth / 2, screenPos[1] - displayFrame.top + viewHeight);
		} else {
			// Show above
			// Offsets are after decorations (e.g. status bar) are factored in
			cheatSheet.setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL, viewCenterX
					- screenWidth / 2, displayFrame.bottom - screenPos[1]);
		}
		cheatSheet.show();
	}

	/**
	 * Used to create shortcuts for an artist, album, or playlist that is then
	 * placed on the default launcher home screen
	 *
	 * @param displayName The shortcut name
	 * @param ids         The ID of the artist, album, playlist, or genre
	 * @param mimeType    The MIME type of the shortcut
	 * @param activity    The {@link FragmentActivity} to use to
	 */
	public static void createShortcutIntent(String displayName, String artistName, String mimeType, FragmentActivity activity, long[] ids) {
		try {
			Bitmap bitmap = null;
			ImageFetcher fetcher = new ImageFetcher(activity);
			if (mimeType.equals(MediaStore.Audio.Albums.CONTENT_TYPE)) {
				bitmap = fetcher.getAlbumImage(ids[0]);
			} else if (mimeType.equals(MediaStore.Audio.Artists.CONTENT_TYPE)) {
				bitmap = fetcher.getArtistImage(ids[0]);
			} else if (mimeType.equals(MediaStore.Audio.Genres.CONTENT_TYPE)) {
				bitmap = fetcher.getGenreImage(ids);
			} else if (mimeType.equals(MediaStore.Audio.Playlists.CONTENT_TYPE)) {
				bitmap = fetcher.getPlaylistImage(ids[0]);
			}
			if (bitmap == null) {
				bitmap = BitmapFactory.decodeResource(activity.getResources(), R.drawable.default_artwork);
			}
			// Intent used when the icon is touched
			Intent shortcutIntent = new Intent(activity, ShortcutActivity.class);
			shortcutIntent.setAction(Intent.ACTION_VIEW);
			shortcutIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			shortcutIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
			shortcutIntent.putExtra(Constants.ID, ids[0]);
			shortcutIntent.putExtra(Constants.IDS, ApolloUtils.serializeIDs(ids));
			shortcutIntent.putExtra(Constants.NAME, displayName);
			shortcutIntent.putExtra(Constants.MIME_TYPE, mimeType);
			// check if display name is a path
			if (displayName.startsWith("/")) {
				File file = new File(displayName);
				if (file.exists()) {
					// use file name as label
					displayName = file.getName();
				}
			}
			if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
				// Intent that actually sets the shortcut
				Intent intent = new Intent();
				intent.putExtra(Intent.EXTRA_SHORTCUT_ICON, ImageUtils.resizeAndCropCenter(bitmap, 96));
				intent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent);
				intent.putExtra(Intent.EXTRA_SHORTCUT_NAME, displayName);
				intent.setAction("com.android.launcher.action.INSTALL_SHORTCUT");
				activity.sendBroadcast(intent);
				String resultMsg = activity.getString(R.string.pinned_to_home_screen, displayName);
				AppMsg.makeText(activity, resultMsg, AppMsg.STYLE_ALERT).show();
			} else {
				// use shortcut manager to install shortcut
				ShortcutManager sManager = activity.getSystemService(ShortcutManager.class);
				if (sManager.isRequestPinShortcutSupported()) {
					Icon icon = Icon.createWithBitmap(bitmap);
					String shortcutId = displayName + "|" + artistName + "|" + ids[0];
					ShortcutInfo sInfo = new ShortcutInfo.Builder(activity, shortcutId).setIcon(icon)
							.setIntent(shortcutIntent).setShortLabel(displayName).build();
					sManager.requestPinShortcut(sInfo, null);
				}
			}
		} catch (Exception e) {
			Log.e(TAG, "createShortcutIntent()", e);
			String resultMsg = activity.getString(R.string.could_not_be_pinned_to_home_screen, displayName);
			AppMsg.makeText(activity, resultMsg, AppMsg.STYLE_ALERT).show();
		}
	}

	/**
	 * send broadcast to external equalizer app with current audio session ID
	 *
	 * @param sessionId current audio session ID
	 */
	public static void notifyExternalEqualizer(Context context, long sessionId) {
		Intent intent = new Intent(AudioEffect.ACTION_OPEN_AUDIO_EFFECT_CONTROL_SESSION);
		intent.putExtra(AudioEffect.EXTRA_AUDIO_SESSION, sessionId);
		intent.putExtra(AudioEffect.EXTRA_PACKAGE_NAME, BuildConfig.APPLICATION_ID);
		context.sendBroadcast(intent);
	}

	/**
	 * serialize ID array into a string
	 *
	 * @param ids IDs to serialize
	 * @return serialized ID array
	 */
	public static String serializeIDs(long[] ids) {
		StringBuilder result = new StringBuilder();
		for (long id : ids) {
			result.append(id);
			result.append(';');
		}
		// remove last separator
		if (result.length() > 0)
			result.deleteCharAt(result.length() - 1);
		return result.toString();
	}

	/**
	 * read serialized ID array
	 *
	 * @param idsStr serialized string to read
	 * @return ID array
	 */
	public static long[] readSerializedIDs(String idsStr) {
		String[] items = idsStr.split(";");
		if (items.length > 0) {
			long[] ids = new long[items.length];
			try {
				for (int i = 0; i < items.length; i++) {
					String item = items[i];
					ids[i] = Long.parseLong(item);
				}
				return ids;
			} catch (NumberFormatException exception) {
				Log.w(TAG, "readSerializedIDs() number format!");
			}
		}
		return new long[1];
	}

	/**
	 * check if equalizer is supported
	 *
	 * @return true if an equalizer was found
	 */
	public static boolean isEqualizerInstalled(Context context) {
		Intent intent = new Intent(AudioEffect.ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL);
		return context.getPackageManager().resolveActivity(intent, 0) != null;
	}

	/**
	 * register an ListView click listener for a sub view
	 *
	 * @param view      sub view of the view item
	 * @param container parent view of the view item
	 * @param pos       position of the view item
	 * @param id        Item ID
	 */
	public static void registerItemViewListener(@NonNull View view, final ViewGroup container, final int pos, final long id) {
		view.setOnClickListener(v -> {
			// check if container is a list
			if (container instanceof AbsListView) {
				AbsListView list = ((AbsListView) container);
				list.performItemClick(v, pos, id);
			}
			// check if parent is a list
			else if (container.getParent() instanceof AbsListView) {
				AbsListView list = ((AbsListView) container.getParent());
				list.performItemClick(v, pos, id);
			}
		});
	}

	/**
	 * set wakelock status depending on app settings
	 */
	public static void setWakelock(Activity activity) {
		PreferenceUtils prefs = PreferenceUtils.getInstance(activity);
		if (prefs.getWakelockStatus()) {
			activity.getWindow().addFlags(LayoutParams.FLAG_KEEP_SCREEN_ON);
		} else {
			activity.getWindow().clearFlags(LayoutParams.FLAG_KEEP_SCREEN_ON);
		}
	}

	/**
	 * open battery optimization dialog
	 */
	public static void openBatteryOptimizationDialog(FragmentActivity activity) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
			PreferenceUtils pref = PreferenceUtils.getInstance(activity);
			PowerManager pm = (PowerManager) activity.getSystemService(Context.POWER_SERVICE);
			if (!pref.isBatteryOptimizationIgnored() && pm != null && !pm.isIgnoringBatteryOptimizations(activity.getPackageName())) {
				BatteryOptDialog.show(activity.getSupportFragmentManager());
			}
		}
	}

	/**
	 * open battery optimization page of the Android system
	 */
	public static void redirectToBatteryOptimization(Activity activity) {
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
	 * convert list long array
	 */
	public static long[] toLongArray(List<Long> list) {
		long[] result = new long[list.size()];
		for (int i = 0; i < result.length; i++) {
			if (list.get(i) != null) {
				result[i] = list.get(i);
			}
		}
		return result;
	}

	/**
	 * convert long array
	 */
	public static long[] toLongArray(Long[] array) {
		long[] result = new long[array.length];
		for (int i = 0; i < result.length; i++) {
			if (array[i] != null) {
				result[i] = array[i];
			}
		}
		return result;
	}

	/**
	 * convert long array
	 */
	public static Long[] toLongArray(long[] array) {
		Long[] result = new Long[array.length];
		for (int i = 0; i < result.length; i++) {
			result[i] = array[i];
		}
		return result;
	}
}