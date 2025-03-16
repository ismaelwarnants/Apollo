package org.nuclearfog.apollo.utils;

import android.content.Context;
import android.text.TextUtils;

import org.nuclearfog.apollo.BuildConfig;
import org.nuclearfog.apollo.R;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * This class contains utils for strings
 *
 * @author nuclearfog
 */
public final class StringUtils {

	/* This class is never initiated */
	private StringUtils() {
	}

	/**
	 * Capitalizes the first character in a string
	 *
	 * @param str The string to capitalize
	 * @return A capitalized string
	 */
	public static String capitalize(String str) {
		return capitalize(str, null);
	}

	/**
	 * Capitalizes the first character in a string
	 *
	 * @param str        The string to capitalize
	 * @param delimiters The delimiters
	 * @return A capitalized string
	 */
	public static String capitalize(String str, char[] delimiters) {
		if (TextUtils.isEmpty(str) || delimiters == null || delimiters.length == 0) {
			return str;
		}
		char[] buffer = str.toCharArray();
		boolean capitalizeNext = true;
		for (int i = 0; i < buffer.length; i++) {
			char ch = buffer[i];
			if (isDelimiter(ch, delimiters)) {
				capitalizeNext = true;
			} else if (capitalizeNext) {
				buffer[i] = Character.toTitleCase(ch);
				capitalizeNext = false;
			}
		}
		return new String(buffer);
	}

	/**
	 * Used to create a formatted time string for the duration of tracks.
	 *
	 * @param context  The {@link Context} to use.
	 * @param duration The track in milliseconds.
	 * @return Duration of a track that's properly formatted.
	 */
	public static String makeTimeString(Context context, long duration) {
		if (duration < 0) {
			// invalid time
			return "--:--";
		}
		long sec = duration / 1000;
		long min = sec / 60;
		long hour = min / 60;
		if (hour > 0)
			return String.format(context.getString(R.string.duration_format_long), hour, min % 60, sec % 60);
		return String.format(context.getString(R.string.duration_format_short), min % 60, sec % 60);
	}

	/**
	 * Is the character a delimiter.
	 *
	 * @param ch         the character to check
	 * @param delimiters the delimiters
	 * @return true if it is a delimiter
	 */
	private static boolean isDelimiter(char ch, char[] delimiters) {
		if (delimiters == null) {
			return Character.isWhitespace(ch);
		}
		for (char delimiter : delimiters) {
			if (ch == delimiter) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Used to make number of labels for the number of artists, albums, songs,
	 * genres, and playlists.
	 *
	 * @param context   The {@link Context} to use.
	 * @param pluralInt The ID of the plural string to use.
	 * @param number    The number of artists, albums, songs, genres, or playlists.
	 * @return A {@link String} used as a label for the number of artists,
	 * albums, songs, genres, and playlists.
	 */
	public static String makeLabel(Context context, int pluralInt, int number) {
		return context.getResources().getQuantityString(pluralInt, number, number);
	}

	/**
	 * encode text o UTF-8
	 *
	 * @param text text to encode
	 * @return UTF8 translated string
	 */
	public static String encodeUTF8(String text) {
		try {
			return URLEncoder.encode(text, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			return "";
		}
	}

	/**
	 * A hashing method that changes a string (like a URL) into a hash suitable
	 * for using as a disk filename.
	 *
	 * @param key The key used to store the file
	 */
	public static String hashKeyForDisk(String key) {
		String cacheKey;
		try {
			MessageDigest digest = MessageDigest.getInstance("MD5");
			digest.update(key.getBytes());
			StringBuilder builder = new StringBuilder();
			for (byte b : digest.digest()) {
				String hex = Integer.toHexString(0xFF & b);
				if (hex.length() == 1) {
					builder.append('0');
				}
				builder.append(hex);
			}
			cacheKey = builder.toString();
		} catch (NoSuchAlgorithmException e) {
			if (BuildConfig.DEBUG) {
				e.printStackTrace();
			}
			cacheKey = String.valueOf(key.hashCode());
		}
		return cacheKey;
	}

	/**
	 * create unique cache key for specific entry
	 *
	 * @param type image type to cache
	 * @param data string values (e.g. artist, album name) used to calculate cache key
	 * @return key string
	 */
	public static String generateCacheKey(Constants.ImageType type, String... data) {
		StringBuilder str = new StringBuilder();
		for (String key : data) {
			str.append(key).append('_');
		}
		str.append(type.value);
		return str.toString();
	}
}