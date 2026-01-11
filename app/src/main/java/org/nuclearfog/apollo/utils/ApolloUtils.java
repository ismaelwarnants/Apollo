package org.nuclearfog.apollo.utils;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Rect;
import android.media.audiofx.AudioEffect;
import android.os.Build;
import android.os.PowerManager;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.MarginLayoutParams;
import android.view.ViewParent;
import android.view.WindowManager.LayoutParams;
import android.widget.AbsListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.PluralsRes;
import androidx.annotation.StringRes;
import androidx.core.content.ContextCompat;
import androidx.core.content.pm.ShortcutInfoCompat;
import androidx.core.content.pm.ShortcutManagerCompat;
import androidx.core.graphics.drawable.IconCompat;
import androidx.fragment.app.FragmentActivity;

import org.nuclearfog.apollo.BuildConfig;
import org.nuclearfog.apollo.R;
import org.nuclearfog.apollo.cache.ImageFetcher;
import org.nuclearfog.apollo.store.preferences.AppPreferences;
import org.nuclearfog.apollo.ui.activities.ShortcutActivity;
import org.nuclearfog.apollo.ui.dialogs.BatteryOptDialog;
import org.nuclearfog.apollo.ui.views.ToastView;

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
 * @author nuclearfog
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
	 * @param mimeType    The MIME type of the shortcut
	 * @param ids         The ID of the artist, album, playlist, or genre
	 */
	@SuppressWarnings("deprecation")
	public static void createShortcutIntent(FragmentActivity activity, String displayName, String mimeType, long... ids) {
		try {
			Context context = activity.getApplicationContext();
			if (ShortcutManagerCompat.isRequestPinShortcutSupported(context)) {
				IconCompat icon;
				ImageFetcher fetcher = new ImageFetcher(context);
				// get icon for shortcut
				if (MediaStore.Audio.Albums.CONTENT_TYPE.equals(mimeType)) {
					icon = IconCompat.createWithBitmap(ImageUtils.createShortcutIcon(context, fetcher.getAlbumImage(ids[0])));
				} else if (MediaStore.Audio.Artists.CONTENT_TYPE.equals(mimeType)) {
					icon = IconCompat.createWithBitmap(ImageUtils.createShortcutIcon(context, fetcher.getArtistImage(ids[0])));
				} else if (MediaStore.Audio.Genres.CONTENT_TYPE.equals(mimeType)) {
					icon = IconCompat.createWithBitmap(ImageUtils.createShortcutIcon(context, fetcher.getGenreImage(ids)));
				} else if (MediaStore.Audio.Playlists.CONTENT_TYPE.equals(mimeType)) {
					icon = IconCompat.createWithBitmap(ImageUtils.createShortcutIcon(context, fetcher.getPlaylistImage(ids[0])));
				} else {
					icon = IconCompat.createWithResource(context, R.drawable.default_artwork);
				}
				// create Intent used when the icon is touched
				Intent shortcutIntent = new Intent(context, ShortcutActivity.class);
				shortcutIntent.setAction(Intent.ACTION_VIEW);
				shortcutIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				shortcutIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
				shortcutIntent.putExtra(Constants.ID, ids[0]);
				shortcutIntent.putExtra(Constants.IDS, ApolloUtils.serializeIDs(ids));
				shortcutIntent.putExtra(Constants.NAME, displayName);
				shortcutIntent.putExtra(Constants.MIME_TYPE, mimeType);
				// format display name and create ID
				displayName = StringUtils.getFolderName(displayName);
				String shortcutId = mimeType + "; " + displayName + "; (" + serializeIDs(ids) + ")";
				// create shortcut
				ShortcutInfoCompat sInfo = new ShortcutInfoCompat.Builder(context, shortcutId)
						.setIcon(icon).setIntent(shortcutIntent).setShortLabel(displayName).build();
				ShortcutManagerCompat.requestPinShortcut(context, sInfo, null);
				// show notification if shortcut was added
				if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
					showInfoToast(activity, R.string.pinned_to_home_screen, displayName);
				}
			} else {
				showInfoToast(activity, R.string.could_not_be_pinned_to_home_screen, displayName);
			}
		} catch (RuntimeException e) {
			Log.e(TAG, "createShortcutIntent()", e);
			showAlertToast(activity, R.string.could_not_be_pinned_to_home_screen, displayName);
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
	 * show info toast at the top of the activity view
	 *
	 * @param activity activity to show the toast view
	 * @param plural   plurals string resource
	 * @param qnt      plurals quantity
	 */
	public static void showInfoToast(Activity activity, @PluralsRes int plural, int qnt) {
		String message = activity.getResources().getQuantityString(plural, qnt, qnt);
		showToast(activity, message, false, Constants.DURATION_SHORT);
	}

	/**
	 * show info toast at the top of the activity view
	 *
	 * @param activity activity to show the toast view
	 * @param text     toast message
	 */
	public static void showInfoToast(Activity activity, @StringRes int text) {
		showToast(activity, activity.getString(text), false, Constants.DURATION_SHORT);
	}

	/**
	 * show info toast at the top of the activity view
	 *
	 * @param activity activity to show the toast view
	 * @param text     toast message
	 */
	public static void showInfoToast(Activity activity, String text) {
		showToast(activity, text, false, Constants.DURATION_SHORT);
	}

	/**
	 * show info toast at the top of the activity view
	 *
	 * @param activity activity to show the toast view
	 * @param text     toast message
	 */
	public static void showInfoToast(Activity activity, @StringRes int strRes, String text) {
		String resultMsg = activity.getString(strRes, text);
		showToast(activity, resultMsg, false, Constants.DURATION_SHORT);
	}

	/**
	 * show alert toast at the top of the activity view
	 *
	 * @param activity activity to show the toast view
	 * @param strRes   toast message
	 */
	public static void showAlertToast(Activity activity, @StringRes int strRes) {
		showToast(activity, activity.getString(strRes), true, Constants.DURATION_LONG);
	}

	/**
	 * show alert toast at the top of the activity view
	 *
	 * @param activity activity to show the toast view
	 * @param strRes   toast message string resource
	 * @param text     argument for string resource
	 */
	public static void showAlertToast(Activity activity, @StringRes int strRes, String text) {
		String resultMsg = activity.getString(strRes, text);
		showToast(activity, resultMsg, true, Constants.DURATION_LONG);
	}

	/**
	 * get the height of the status bar (notification bar)
	 *
	 * @return height in pixels
	 */
	public static int statusBarHeight(Activity activity) {
		Rect rectangle = new Rect();
		activity.getWindow().getDecorView().getWindowVisibleDisplayFrame(rectangle);
		return rectangle.top;
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

	/**
	 * show toast message at the top of the activity view, then disappear after a given time
	 *
	 * @param activity activity to attach the toast view
	 * @param text     text to show on the toast view
	 * @param alert    true to enable the alert layout
	 * @param duration duration in milliseconds to show the message
	 */
	public static void showToast(Activity activity, String text, boolean alert, long duration) {
		MarginLayoutParams params = new MarginLayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
		params.topMargin = statusBarHeight(activity);
		ToastView toastView = new ToastView(activity.getApplicationContext());
		toastView.setText(text);
		toastView.setAlert(alert);
		activity.addContentView(toastView, params);
		toastView.postDelayed(() -> {
			ViewParent p = toastView.getParent();
			if (p instanceof ViewGroup) {
				((ViewGroup) p).removeView(toastView);
			}
		}, duration);
	}
}