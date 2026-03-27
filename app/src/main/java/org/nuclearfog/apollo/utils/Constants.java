package org.nuclearfog.apollo.utils;

import static android.Manifest.permission.POST_NOTIFICATIONS;
import static android.Manifest.permission.READ_EXTERNAL_STORAGE;
import static android.Manifest.permission.READ_MEDIA_AUDIO;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;

import android.os.Build;

/**
 * App-wide constants.
 *
 * @author Andrew Neal (andrewdneal@gmail.com)
 * @author nuclearfog
 */
public final class Constants {

	/**
	 * link to the source code repository
	 */
	public static final String SOURCE_URL = "https://codeberg.org/nuclearfog/Apollo";

	/**
	 * The ID of an artist, album, genre, or playlist passed to the profile activity
	 */
	public static final String ID = "id";
	/**
	 * a group of IDs
	 */
	public static final String IDS = "ids";
	/**
	 * The name of an artist, album, genre, or playlist passed to the profile activity
	 */
	public static final String NAME = "name";
	/**
	 * The name of an artist passed to the profile activity
	 */
	public static final String ARTIST_NAME = "artist_name";
	/**
	 * The year an album was released passed to the profile activity
	 */
	public static final String ALBUM_YEAR = "album_year";
	/**
	 * The MIME type passed to a the profile activity
	 */
	public static final String MIME_TYPE = "mime_type";
	/**
	 * path to a music folder
	 */
	public static final String FOLDER = "folder_path";
	/**
	 * key used for context menu to add IDs to playlist entries
	 */
	public static final String PLAYLIST_ID = "context_playlist_id";

	public static final long DURATION_SHORT = 2000;

	public static final long DURATION_LONG = 5000;

	/**
	 * max entries of 'last added' playlist
	 */
	public static final int LAST_ADDED_LIMIT = 100;

	/**
	 * maximal scroll speed when dragging a list element
	 */
	public static final float DRAG_DROP_MAX_SPEED = 3.0f;
	/**
	 * opacity of list items when they are marked as hidden
	 */
	public static final float OPACITY_HIDDEN = 0.4f;
	/**
	 * width ratio percentage of a dialog window
	 */
	public static final float DIALOG_WIDTH_PERCENTAGE = 0.8f;
	/**
	 * animation duration in milliseconds
	 */
	public static final long ANIMATION_SPEED = 500;
	/**
	 * item limit to use a smooth scrolling animation
	 */
	public static final int SCROLL_ANIM_JUMP_LIMIT = 50;

	public static final int TRANSPARENCY_MASK_INACTIVE = 0x5fffffff;

	/**
	 * permissions used for Android 6+
	 */
	public static final String[] PERMISSIONS;

	static {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
			PERMISSIONS = new String[]{READ_MEDIA_AUDIO, POST_NOTIFICATIONS};
		} else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
			PERMISSIONS = new String[]{READ_EXTERNAL_STORAGE};
		} else {
			PERMISSIONS = new String[]{READ_EXTERNAL_STORAGE, WRITE_EXTERNAL_STORAGE};
		}
	}

	/* This class is never initiated. */
	private Constants() {
	}

	/**
	 * types of images/artworks to download
	 */
	public enum ImageType {

		ARTIST("artist"), ALBUM("album"), GENRE("genre"), PLAYLIST("playlist"), FOLDER("folder"), ARTWORK("artwork");

		public final String value;

		ImageType(String value) {
			this.value = value;
		}
	}
}