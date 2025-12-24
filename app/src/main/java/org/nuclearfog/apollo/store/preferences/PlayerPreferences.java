package org.nuclearfog.apollo.store.preferences;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;

import org.nuclearfog.apollo.service.MusicPlaybackService;

/**
 * Preferences used to save the playstate of the player
 *
 * @author nuclearfog
 */
@SuppressLint("ApplySharedPref")
public class PlayerPreferences {

	/**
	 * Name of the preference file
	 */
	private static final String PLAYBACK_PREF_NAME = "playback_settings";
	/**
	 * position of the player in milliseconds
	 */
	private static final String POS_SEEK = "seekpos";
	/**
	 * shuffle mode {@link MusicPlaybackService#SHUFFLE_NONE#SHUFFLE_NORMAL#SHUFFLE_AUTO}
	 */
	private static final String MODE_SHUFFLE = "shufflemode";
	/**
	 * repeat mode {@link MusicPlaybackService#REPEAT_NONE#REPEAT_CURRENT#REPEAT_ALL}
	 */
	private static final String MODE_REPEAT = "repeatmode";
	/**
	 * position of the selected track of the playback list
	 */
	private static final String POS_CURSOR = "curpos";

	private static PlayerPreferences mInstance;

	private SharedPreferences playbackPref;

	/**
	 *
	 */
	private PlayerPreferences(Context context) {
		playbackPref = context.getSharedPreferences(PLAYBACK_PREF_NAME, Context.MODE_PRIVATE);
	}

	/**
	 * get singleton instance of this class
	 *
	 * @param context Context to load app settings
	 * @return singleton instance
	 */
	public static PlayerPreferences getInstance(Context context) {
		if (mInstance == null) {
			mInstance = new PlayerPreferences(context);
		}
		return mInstance;
	}

	/**
	 * get current cursor position
	 *
	 * @return cursor position
	 */
	public int getCursorPosition() {
		return playbackPref.getInt(POS_CURSOR, 0);
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
		return playbackPref.getInt(MODE_REPEAT, MusicPlaybackService.REPEAT_NONE);
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
	 * @param repeatMode  repeat mode flags {@link MusicPlaybackService#REPEAT_NONE#REPEAT_CURRENT#REPEAT_ALL}
	 * @param shuffleMode shuffle mode flags {@link MusicPlaybackService#SHUFFLE_NONE#SHUFFLE_NORMAL#SHUFFLE_AUTO}
	 */
	public void setRepeatAndShuffleMode(int repeatMode, int shuffleMode) {
		SharedPreferences.Editor editor = playbackPref.edit();
		editor.putInt(MODE_REPEAT, repeatMode);
		editor.putInt(MODE_SHUFFLE, shuffleMode);
		editor.commit();
	}
}