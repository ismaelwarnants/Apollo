package org.nuclearfog.apollo.utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.preference.PreferenceManager;

import org.nuclearfog.apollo.R;
import org.nuclearfog.apollo.service.MusicPlaybackService;
import org.nuclearfog.apollo.ui.fragments.phone.AlbumFragment;
import org.nuclearfog.apollo.ui.fragments.phone.ArtistFragment;
import org.nuclearfog.apollo.ui.fragments.phone.SongFragment;

/**
 * A collection of helpers designed to get and set various preferences across
 * Apollo.
 *
 * @author Andrew Neal (andrewdneal@gmail.com)
 * @author nuclearfog
 */
@SuppressLint("ApplySharedPref")
public final class PreferenceUtils {

	private static final String TAG = "PreferenceUtils";

	/* Default start page (Artist page) */
	private static final int DEFAULT_PAGE = 3;
	/**
	 * Saves the last page the pager was on in {@link org.nuclearfog.apollo.ui.fragments.phone.MusicBrowserPhoneFragment}
	 */
	private static final String START_PAGE = "start_page";
	// Sort order for the artist list
	private static final String ARTIST_SORT_ORDER = "artist_sort_order";
	// Sort order for the artist song list
	private static final String ARTIST_SONG_SORT_ORDER = "artist_song_sort_order";
	// Sort order for the artist album list
	private static final String ARTIST_ALBUM_SORT_ORDER = "artist_album_sort_order";
	// Sort order for the album list
	private static final String ALBUM_SORT_ORDER = "album_sort_order";
	// Sort order for the album song list
	private static final String ALBUM_SONG_SORT_ORDER = "album_song_sort_order";
	// Sort order for the album song list
	private static final String FOLDER_SONG_SORT_ORDER = "folder_song_sort_order";
	// Sort order for the song list
	private static final String SONG_SORT_ORDER = "song_sort_order";
	// Sets the type of layout to use for the artist list
	private static final String ARTIST_LAYOUT = "artist_layout";
	// Sets the type of layout to use for the album list
	private static final String ALBUM_LAYOUT = "album_layout";
	// Sets the type of layout to use for the recent list
	private static final String RECENT_LAYOUT = "recent_layout";
	// Key that gives permissions to download missing album covers
	private static final String DOWNLOAD_MISSING_ARTWORK = "download_missing_artwork";
	// Key that gives permissions to download missing artist images
	private static final String DOWNLOAD_MISSING_ARTIST_IMAGES = "download_missing_artist_images";
	// Key used to set the overall theme color
	private static final String DEFAULT_THEME_COLOR = "default_theme_color";
	public static final String LAYOUT_SIMPLE = "simple";
	public static final String LAYOUT_DETAILED = "detailed";
	public static final String LAYOUT_GRID = "grid";
	// equalizer settings
	private static final String FX_PREF_NAME = "eq_settings";
	private static final String FX_ENABLE = "fx_enable_effects";
	private static final String FX_BASSBOOST = "fx_bassboost_enable";
	private static final String FX_REVERB = "fx_reverb_enable";
	private static final String FX_PREFER_EXT = "fx_prefer_external";
	private static final String FX_EQUALIZER_BANDS = "fx_equalizer_bands";
	private static final String FX_PRESET = "fx_preset_name";
	// app settings
	private static final String PACKAGE_INDEX = "theme_index";
	private static final String BAT_OPTIMIZATION = "ignore_bat_opt";
	private static final String SHOW_HIDDEN = "view_hidden_items";
	private static final String ENABLE_XFADE = "fade_effect_enable";
	private static final String KEEP_SCREEN_ON = "keep_screen_on";
	private static final String AUTOSCROLL = "autoscroll_current";
	private static final String DISABLE_CERT_VALID = "disable_ssl_verification";
	// playback settings
	private static final String PLAYBACK_PREF_NAME = "playback_settings";
	private static final String POS_SEEK = "seekpos";
	private static final String MODE_SHUFFLE = "shufflemode";
	private static final String MODE_REPEAT = "repeatmode";
	private static final String POS_CURSOR = "curpos";
	private static final String HISTORY = "history";
	private static final String QUEUE = "queue";

	private static PreferenceUtils sInstance;

	private SharedPreferences defaultPref;
	private SharedPreferences playbackPref;
	private SharedPreferences audioEffectsPref;
	private int defaultColor;


	private PreferenceUtils(Context context) {
		defaultPref = PreferenceManager.getDefaultSharedPreferences(context);
		audioEffectsPref = context.getSharedPreferences(FX_PREF_NAME, Context.MODE_PRIVATE);
		playbackPref = context.getSharedPreferences(PLAYBACK_PREF_NAME, Context.MODE_PRIVATE);
		defaultColor = context.getResources().getColor(R.color.holo_green);
	}

	/**
	 * creates and returns a singleton instance of this class
	 *
	 * @return A singleton of this class
	 */
	public static PreferenceUtils getInstance(Context context) {
		if (sInstance == null) {
			sInstance = new PreferenceUtils(context.getApplicationContext());
		}
		return sInstance;
	}

	/**
	 * Returns the last page the user was on when the app was exited.
	 *
	 * @return The page to start on when the app is opened.
	 */
	public int getStartPage() {
		return defaultPref.getInt(START_PAGE, DEFAULT_PAGE);
	}

	/**
	 * Saves the current page the user is on when they close the app.
	 *
	 * @param value The last page the pager was on {@link org.nuclearfog.apollo.ui.fragments.phone.MusicBrowserPhoneFragment}.
	 */
	public void setStartPage(int value) {
		SharedPreferences.Editor editor = defaultPref.edit();
		editor.putInt(START_PAGE, value);
		editor.commit();
	}

	/**
	 * Returns the current theme color.
	 *
	 * @return The current theme color.
	 */
	public int getThemeColor() {
		return defaultPref.getInt(DEFAULT_THEME_COLOR, defaultColor);
	}

	/**
	 * Sets the new theme color.
	 *
	 * @param value The new theme color to use.
	 */
	public void setThemeColor(int value) {
		SharedPreferences.Editor editor = defaultPref.edit();
		editor.putInt(DEFAULT_THEME_COLOR, value);
		editor.commit();
	}

	/**
	 * @return True if the user has checked to download missing album covers,
	 * false otherwise.
	 */
	public boolean downloadMissingArtwork() {
		return defaultPref.getBoolean(DOWNLOAD_MISSING_ARTWORK, false);
	}

	/**
	 * @return True if the user has checked to download missing artist images,
	 * false otherwise.
	 */
	public boolean downloadMissingArtistImages() {
		return defaultPref.getBoolean(DOWNLOAD_MISSING_ARTIST_IMAGES, false);
	}

	/**
	 * Saves the sort order for a list.
	 *
	 * @param key   Which sort order to change
	 * @param value The new sort order
	 */
	private void setSortOrder(String key, String value) {
		SharedPreferences.Editor editor = defaultPref.edit();
		editor.putString(key, value);
		editor.commit();
	}

	/**
	 * @return The sort order used for the artist list in {@link ArtistFragment}
	 */
	public String getArtistSortOrder() {
		// This is only to prevent return an invalid field name caused by bug BUGDUMP-21136
		String defaultSortKey = SortOrder.ArtistSortOrder.ARTIST_A_Z;
		String key = defaultPref.getString(ARTIST_SORT_ORDER, defaultSortKey);
		if (key.equals(SortOrder.ArtistSongSortOrder.SONG_FILENAME)) {
			key = defaultSortKey;
		}
		return key;
	}

	/**
	 * Sets the sort order for the artist list.
	 *
	 * @param value The new sort order
	 */
	public void setArtistSortOrder(String value) {
		setSortOrder(ARTIST_SORT_ORDER, value);
	}

	/**
	 * @return The sort order used for the artist song list in
	 * {@link org.nuclearfog.apollo.ui.fragments.profile.ArtistSongFragment}
	 */
	public String getArtistSongSortOrder() {
		return defaultPref.getString(ARTIST_SONG_SORT_ORDER, SortOrder.ArtistSongSortOrder.SONG_A_Z);
	}

	/**
	 * Sets the sort order for the artist song list.
	 *
	 * @param value The new sort order
	 */
	public void setArtistSongSortOrder(String value) {
		setSortOrder(ARTIST_SONG_SORT_ORDER, value);
	}

	/**
	 * @return The sort order used for the artist album list in
	 * {@link org.nuclearfog.apollo.ui.fragments.profile.ArtistAlbumFragment}
	 */
	public String getArtistAlbumSortOrder() {
		return defaultPref.getString(ARTIST_ALBUM_SORT_ORDER, SortOrder.ArtistAlbumSortOrder.ALBUM_A_Z);
	}

	/**
	 * Sets the sort order for the artist album list.
	 *
	 * @param value The new sort order
	 */
	public void setArtistAlbumSortOrder(String value) {
		setSortOrder(ARTIST_ALBUM_SORT_ORDER, value);
	}

	/**
	 * @return The sort order used for the album list in {@link AlbumFragment}
	 */
	public String getAlbumSortOrder() {
		return defaultPref.getString(ALBUM_SORT_ORDER, SortOrder.AlbumSortOrder.ALBUM_A_Z);
	}

	/**
	 * Sets the sort order for the album list.
	 *
	 * @param value The new sort order
	 */
	public void setAlbumSortOrder(String value) {
		setSortOrder(ALBUM_SORT_ORDER, value);
	}

	/**
	 * @return The sort order used for the album song in
	 * {@link org.nuclearfog.apollo.ui.fragments.profile.AlbumSongFragment}
	 */
	public String getAlbumSongSortOrder() {
		return defaultPref.getString(ALBUM_SONG_SORT_ORDER, SortOrder.AlbumSongSortOrder.SONG_TRACK_LIST);
	}

	/**
	 * Sets the sort order for the album song list.
	 *
	 * @param value The new sort order
	 */
	public void setAlbumSongSortOrder(String value) {
		setSortOrder(ALBUM_SONG_SORT_ORDER, value);
	}

	/**
	 * @return The sort order used for the folder song in
	 * {@link org.nuclearfog.apollo.ui.fragments.profile.FolderSongFragment}
	 */
	public String getFolderSongSortOrder() {
		return defaultPref.getString(FOLDER_SONG_SORT_ORDER, SortOrder.FolderSongSortOrder.SONG_TRACK_LIST);
	}

	/**
	 * Sets the sort order for the folder song list.
	 *
	 * @param value The new sort order
	 */
	public void setFolderSongSortOrder(String value) {
		setSortOrder(FOLDER_SONG_SORT_ORDER, value);
	}

	/**
	 * get current cursor position
	 *
	 * @return cursor position
	 */
	public int getCursorPosition() {
		return defaultPref.getInt(POS_CURSOR, 0);
	}

	/**
	 * set current cursor position
	 *
	 * @param position cursor position
	 */
	public void setCursorPosition(int position) {
		SharedPreferences.Editor editor = playbackPref.edit();
		editor.putInt(POS_CURSOR, position);
		editor.commit();
	}

	/**
	 * @return The sort order used for the song list in {@link SongFragment}
	 */
	public String getSongSortOrder() {
		return defaultPref.getString(SONG_SORT_ORDER, SortOrder.SongSortOrder.SONG_A_Z);
	}

	/**
	 * Sets the sort order for the song list.
	 *
	 * @param value The new sort order
	 */
	public void setSongSortOrder(String value) {
		setSortOrder(SONG_SORT_ORDER, value);
	}

	/**
	 * get last seek position
	 *
	 * @return position of the seekbar
	 */
	public long getSeekPosition() {
		return playbackPref.getLong(POS_SEEK, 0L);
	}

	/**
	 * set last seekbar position
	 *
	 * @param seekPos seekbar position
	 */
	public void setSeekPosition(long seekPos) {
		SharedPreferences.Editor editor = playbackPref.edit();
		editor.putLong(POS_SEEK, seekPos);
		editor.commit();
	}

	/**
	 * get status of the repeat mode
	 *
	 * @return integer mode {@link MusicPlaybackService#REPEAT_NONE#REPEAT_CURRENT#REPEAT_ALL}
	 */
	public int getRepeatMode() {
		return defaultPref.getInt(MODE_REPEAT, MusicPlaybackService.REPEAT_NONE);
	}

	/**
	 * get status of the shuffle mode
	 *
	 * @return integer mode {@link MusicPlaybackService#SHUFFLE_NONE#SHUFFLE_NORMAL#SHUFFLE_AUTO}
	 */
	public int getShuffleMode() {
		return playbackPref.getInt(MODE_SHUFFLE, MusicPlaybackService.SHUFFLE_NONE);
	}

	/**
	 * set repeat and shuffle mode
	 *
	 * @param repeatMode  repeat mode flags
	 * @param shuffleMode shuffle mode flags
	 */
	public void setRepeatAndShuffleMode(int repeatMode, int shuffleMode) {
		SharedPreferences.Editor editor = playbackPref.edit();
		editor.putInt(MODE_REPEAT, repeatMode);
		editor.putInt(MODE_SHUFFLE, shuffleMode);
		editor.commit();
	}

	/**
	 * get layout for the artist list
	 *
	 * @return layout type {@link #LAYOUT_SIMPLE,#LAYOUT_DETAILED,#LAYOUT_GRID}
	 */
	public String getArtistLayout() {
		return defaultPref.getString(ARTIST_LAYOUT, LAYOUT_GRID);
	}

	/**
	 * Sets the layout type for the artist list
	 *
	 * @param value The new layout type
	 */
	public void setArtistLayout(String value) {
		SharedPreferences.Editor editor = defaultPref.edit();
		editor.putString(ARTIST_LAYOUT, value);
		editor.commit();
	}

	/**
	 * get layout type for the album list
	 *
	 * @return layout type {@link #LAYOUT_SIMPLE,#LAYOUT_DETAILED,#LAYOUT_GRID}
	 */
	public String getAlbumLayout() {
		return defaultPref.getString(ALBUM_LAYOUT, LAYOUT_GRID);
	}

	/**
	 * Sets the layout type for the album list
	 *
	 * @param value The new layout type
	 */
	public void setAlbumLayout(String value) {
		SharedPreferences.Editor editor = defaultPref.edit();
		editor.putString(ALBUM_LAYOUT, value);
		editor.commit();
	}

	/**
	 * get layout type for the recent album list
	 *
	 * @return layout type {@link #LAYOUT_SIMPLE,#LAYOUT_DETAILED,#LAYOUT_GRID}
	 */
	public String getRecentLayout() {
		return defaultPref.getString(RECENT_LAYOUT, LAYOUT_GRID);
	}

	/**
	 * Sets the layout type for the recent list
	 *
	 * @param value The new layout type
	 */
	public void setRecentLayout(String value) {
		SharedPreferences.Editor editor = defaultPref.edit();
		editor.putString(RECENT_LAYOUT, value);
		editor.commit();
	}

	/**
	 * get last playlist
	 *
	 * @deprecated replaced by database {@link org.nuclearfog.apollo.store.PlaylistStore}
	 * @return playlist
	 */
	@Deprecated
	public long[] getPlaylist() {
		String trackQueue = playbackPref.getString(QUEUE, "");
		// delete playlist from shared preferences as it isn't used anymore
		playbackPref.edit().remove(QUEUE).commit();
		if (!trackQueue.isEmpty()) {
			String[] items = trackQueue.split(";");
			long[] ids = new long[items.length];
			for (int i = 0; i < items.length; i++) {
				String item = items[i];
				try {
					ids[i] = Long.parseLong(item, 16);
				} catch (NumberFormatException exception) {
					Log.w(TAG, "bad playlist id: " + item);
					ids[i] = -1;
				}
			}
			return ids;
		}
		return new long[0];
	}

	/**
	 * get track history
	 *
	 * @deprecated replaced by database {@link org.nuclearfog.apollo.store.PlaylistStore}
	 * @return array of track IDs
	 */
	@Deprecated
	public long[] getTrackHistory() {
		String trackHistory = playbackPref.getString(HISTORY, "");
		// delete shuffle history from shared preferences as it isn't used anymore
		playbackPref.edit().remove(HISTORY).commit();
		if (!trackHistory.isEmpty()) {
			String[] items = trackHistory.split(";");
			long[] ids = new long[items.length];
			for (int i = 0; i < ids.length; i++) {
				try {
					ids[i] = Integer.parseInt(items[i], 16);
				} catch (NumberFormatException exception) {
					Log.w(TAG, "bad history index: " + items[i]);
				}
			}
			return ids;
		}
		return new long[0];
	}

	/**
	 * check if audiofx is enabled
	 *
	 * @return true if audiofx is enabled
	 */
	public boolean isAudioFxEnabled() {
		return audioEffectsPref.getBoolean(FX_ENABLE, false);
	}

	/**
	 * enable/disable audiofx
	 *
	 * @param enable true to enable audiofx
	 */
	public void setAudioFxEnabled(boolean enable) {
		SharedPreferences.Editor editor = audioEffectsPref.edit();
		editor.putBoolean(FX_ENABLE, enable);
		editor.commit();
	}

	/**
	 * get equalizer band setup
	 *
	 * @return array of band levels starting with the lowest frequency
	 */
	public int[] getEqualizerBands() {
		String serializedBands = audioEffectsPref.getString(FX_EQUALIZER_BANDS, "");
		if (serializedBands.isEmpty())
			return new int[0];

		String[] bands = serializedBands.split(";");
		int[] result = new int[bands.length];
		for (int i = 0; i < result.length; i++) {
			result[i] = Integer.parseInt(bands[i]);
		}
		return result;
	}

	/**
	 * save new equalizer band setup
	 *
	 * @param bands array of band levels starting with the lowest frequency
	 */
	public void setEqualizerBands(int[] bands) {
		StringBuilder result = new StringBuilder();
		for (int band : bands)
			result.append(band).append(';');
		if (result.length() > 0)
			result.deleteCharAt(result.length() - 1);

		SharedPreferences.Editor editor = audioEffectsPref.edit();
		editor.putString(FX_EQUALIZER_BANDS, result.toString());
		editor.commit();
	}

	/**
	 * get bass boost level
	 *
	 * @return bass level from 0 to 1000
	 */
	public int getBassLevel() {
		return audioEffectsPref.getInt(FX_BASSBOOST, 0);
	}

	/**
	 * set bass boost level
	 *
	 * @param level bass level from 0 to 1000
	 */
	public void setBassLevel(int level) {
		SharedPreferences.Editor editor = audioEffectsPref.edit();
		editor.putInt(FX_BASSBOOST, level);
		editor.commit();
	}

	/**
	 * get reverb level
	 *
	 * @return reverb level (room size)
	 */
	public int getReverbLevel() {
		return audioEffectsPref.getInt(FX_REVERB, 0);
	}

	/**
	 * set reverb level
	 *
	 * @param level reverb level (room size)
	 */
	public void setReverbLevel(int level) {
		SharedPreferences.Editor editor = audioEffectsPref.edit();
		editor.putInt(FX_REVERB, level);
		editor.commit();
	}

	/**
	 * get name of the current selected preset
	 *
	 * @return preset name
	 */
	public String getPresetName() {
		return audioEffectsPref.getString(FX_PRESET, "default");
	}

	/**
	 * set name of the current selected preset
	 *
	 * @param name preset name
	 */
	public void setPresetName(String name) {
		SharedPreferences.Editor editor = audioEffectsPref.edit();
		editor.putString(FX_PRESET, name);
		editor.commit();
	}

	/**
	 * @return true to show tracks hidden by the user
	 */
	public boolean getExcludeTracks() {
		return defaultPref.getBoolean(SHOW_HIDDEN, false);
	}

	/**
	 * @param showHidden true to show hidden tracks
	 */
	public void setExcludeTracks(boolean showHidden) {
		SharedPreferences.Editor editor = defaultPref.edit();
		editor.putBoolean(SHOW_HIDDEN, showHidden);
		editor.commit();
	}

	/**
	 * check if wakelock is enabled
	 *
	 * @return true if wakelock is enabled by user
	 */
	public boolean getWakelockStatus() {
		return defaultPref.getBoolean(KEEP_SCREEN_ON, false);
	}

	/**
	 * check if autoscroll to current track is enabled
	 */
	public boolean autoScrollEnabled() {
		return defaultPref.getBoolean(AUTOSCROLL, true);
	}

	/**
	 * check if ssl certificate is disabled
	 */
	public boolean certificationValidationDisabled() {
		return defaultPref.getBoolean(DISABLE_CERT_VALID, false);
	}

	/**
	 * Return the index of the selected theme
	 *
	 * @return selection index
	 * @noinspection unused
	 */
	public int getThemeSelectionIndex() {
		return defaultPref.getInt(PACKAGE_INDEX, 0);
	}

	/**
	 * Set the index of the theme selection
	 *
	 * @param position selection index
	 */
	public void setThemeSelectionIndex(int position) {
		SharedPreferences.Editor editor = defaultPref.edit();
		editor.putInt(PACKAGE_INDEX, position);
		editor.commit();
	}

	/**
	 * check if battery optimization warning is disabled
	 */
	public boolean isBatteryOptimizationIgnored() {
		return defaultPref.getBoolean(BAT_OPTIMIZATION, false);
	}

	/**
	 * ignore battery optimization warning
	 */
	public void setIgnoreBatteryOptimization() {
		SharedPreferences.Editor editor = defaultPref.edit();
		editor.putBoolean(BAT_OPTIMIZATION, true);
		editor.commit();
	}

	/**
	 * check if external audio effect app is preferred
	 *
	 * @return true if external audio effect app is preferred
	 */
	public boolean isExternalAudioFxPreferred() {
		return defaultPref.getBoolean(FX_PREFER_EXT, false);
	}

	/**
	 * check if crossfade is enabled
	 *
	 * @return true if crossfade is enabled
	 */
	public boolean crossfadeEnabled() {
		return defaultPref.getBoolean(ENABLE_XFADE, true);
	}
}