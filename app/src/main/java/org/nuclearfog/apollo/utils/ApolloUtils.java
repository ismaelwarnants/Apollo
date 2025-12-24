package org.nuclearfog.apollo.utils;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.graphics.drawable.Icon;
import android.media.audiofx.AudioEffect;
import android.os.Build;
import android.os.PowerManager;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager.LayoutParams;
import android.widget.AbsListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import org.nuclearfog.apollo.BuildConfig;
import org.nuclearfog.apollo.R;
import org.nuclearfog.apollo.cache.ImageFetcher;
import org.nuclearfog.apollo.store.preferences.AppPreferences;
import org.nuclearfog.apollo.ui.activities.ShortcutActivity;
import org.nuclearfog.apollo.ui.appmsg.AppMsg;
import org.nuclearfog.apollo.ui.dialogs.BatteryOptDialog;

import java.io.File;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.List;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

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
		// calculate position and dimensions
		int[] screenPos = new int[2];
		Rect displayFrame = new Rect(); // includes decorations (e.g. status bar)
		view.getLocationOnScreen(screenPos);
		view.getWindowVisibleDisplayFrame(displayFrame);
		int viewWidth = view.getWidth();
		int viewHeight = view.getHeight();
		int viewCenterX = screenPos[0] + viewWidth / 2;
		int screenWidth = view.getResources().getDisplayMetrics().widthPixels;
		int estimatedToastHeight = (int) (48 * view.getResources().getDisplayMetrics().density);
		boolean showBelow = screenPos[1] < estimatedToastHeight;
		// init toast view
		Toast cheatSheet = new Toast(view.getContext());
		View toastView = View.inflate(view.getContext(), R.layout.toast_cheatsheet, null);
		TextView tv = toastView.findViewById(R.id.toast_text);
		// setup toast view
		cheatSheet.setView(toastView);
		cheatSheet.setDuration(Toast.LENGTH_SHORT);
		tv.setText(view.getContentDescription());
		// set position of the toast
		if (showBelow) {
			// Show below
			// Offsets are after decorations (e.g. status bar) are factored in
			cheatSheet.setGravity(Gravity.TOP | Gravity.CENTER_HORIZONTAL,
					viewCenterX - screenWidth / 2, screenPos[1] - displayFrame.top + viewHeight);
		} else {
			// Show above
			// Offsets are after decorations (e.g. status bar) are factored in
			cheatSheet.setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL,
					viewCenterX - screenWidth / 2, displayFrame.bottom - screenPos[1]);
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
	 */
	@SuppressWarnings("deprecation")
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
		AppPreferences prefs = AppPreferences.getInstance(activity);
		if (prefs.getWakelockStatus()) {
			activity.getWindow().addFlags(LayoutParams.FLAG_KEEP_SCREEN_ON);
		} else {
			activity.getWindow().clearFlags(LayoutParams.FLAG_KEEP_SCREEN_ON);
		}
	}

	/**
	 * check if permissions are granted required for playback
	 *
	 * @return true if all needed permissions are granted
	 */
	public static boolean permissionsGranted(Context context) {
		for (String permission : Constants.PERMISSIONS) {
			if (ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
				return false;
			}
		}
		return true;
	}

	/**
	 * open battery optimization dialog
	 */
	public static void openBatteryOptimizationDialog(FragmentActivity activity) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
			AppPreferences pref = AppPreferences.getInstance(activity);
			PowerManager pm = (PowerManager) activity.getSystemService(Context.POWER_SERVICE);
			if (!pref.isBatteryOptimizationIgnored() && pm != null && !pm.isIgnoringBatteryOptimizations(activity.getPackageName())) {
				BatteryOptDialog.show(activity.getSupportFragmentManager());
			}
		}
	}

	/**
	 * disable SSL certificate validation on Android < 6.0
	 */
	@SuppressLint("CustomX509TrustManager,TrustAllX509TrustManager")
	public static void disableSSLCertificateValidation() {
		try {
			TrustManager[] trustAllCerts = new TrustManager[]{new X509TrustManager() {
				public X509Certificate[] getAcceptedIssuers() {
					return new X509Certificate[0];
				}

				@Override
				public void checkClientTrusted(X509Certificate[] certs, String authType) {
				}

				@Override
				public void checkServerTrusted(X509Certificate[] certs, String authType) {
				}
			}};
			SSLContext sc = SSLContext.getInstance("SSL");
			sc.init(null, trustAllCerts, new SecureRandom());
			HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
			HttpsURLConnection.setDefaultHostnameVerifier((arg0, arg1) -> true);
			Log.w(TAG, "certificate validation disabled!");
		} catch (Exception e) {
			// ignore
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