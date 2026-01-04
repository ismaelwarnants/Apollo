package org.nuclearfog.apollo.store;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

/**
 * A database for storing custom playback queue/history and playlists
 *
 * @author nuclearfog
 */
public class PlaylistStore extends AppStore {

	/**
	 * database name
	 */
	private static final String DB_NAME = "playlists.db";
	/**
	 * playlist type used for playback queue
	 */
	public static final int PLAYLIST_TYPE_PLAYBACK = 1;
	/**
	 * playlist type used for shuffle history
	 */
	public static final int PLAYLIST_TYPE_HISTORY = 2;


	private static final String[] COLUMNS = {
			PlaylistTable.ID,
	};

	private static final String ORDER = PlaylistTable.POS + " ASC";

	private static final String SELECTION_PLAYBACK = PlaylistTable.TYPE + "=? AND " + PlaylistTable.CARD + "=?";

	/**
	 * singleton instance
	 */
	private static PlaylistStore sInstance;

	/**
	 * query to create playlist table
	 */
	private static final String PLAYBACK_TABLE = "CREATE TABLE IF NOT EXISTS " + PlaylistTable.TABLE + "("
			+ PlaylistTable.ID + " LONG NOT NULL,"
			+ PlaylistTable.TYPE + " INTEGER NOT NULL,"
			+ PlaylistTable.POS + " INTEGER NOT NULL,"
			+ PlaylistTable.CARD + " INTEGER);";

	/**
	 *
	 */
	private PlaylistStore(Context context) {
		super(context, DB_NAME);
	}

	/**
	 * get singleton instance
	 *
	 * @return singleton instance of this class
	 */
	public static PlaylistStore getInstance(Context context) {
		if (sInstance == null)
			sInstance = new PlaylistStore(context);
		return sInstance;
	}


	@Override
	protected void onCreate(SQLiteDatabase db) {
		db.execSQL(PLAYBACK_TABLE);
	}

	/**
	 * get track IDs/Positions from playlist
	 *
	 * @param type   type of the playlist {@link #PLAYLIST_TYPE_PLAYBACK,#PLAYLIST_TYPE_HISTORY}
	 * @param cardId ID of the external storage if any or -1
	 * @return an array of track IDs or positions
	 */
	public long[] getPlaylist(int type, int cardId) {
		SQLiteDatabase db = getReadableDatabase();
		String[] selectionArg = {Integer.toString(type), Integer.toString(cardId)};
		Cursor cursor = db.query(PlaylistTable.TABLE, COLUMNS, SELECTION_PLAYBACK, selectionArg, null, null, ORDER);
		long[] ids = new long[cursor.getCount()];
		if (cursor.moveToFirst()) {
			for (int i = 0; i < ids.length; i++) {
				ids[i] = cursor.getLong(0);
				if (!cursor.moveToNext()) {
					break;
				}
			}
		}
		cursor.close();
		return ids;
	}

	/**
	 * save/replace playlist
	 *
	 * @param type   type of the playlist {@link #PLAYLIST_TYPE_PLAYBACK,#PLAYLIST_TYPE_HISTORY}
	 * @param cardId ID of the external storage if any or -1
	 * @param ids    array of track IDs or positions
	 */
	public void setPlaylist(int type, int cardId, long[] ids) {
		SQLiteDatabase db = getWritableDatabase();
		String[] selectionArg = {Integer.toString(type), Integer.toString(cardId)};
		db.delete(PlaylistTable.TABLE, SELECTION_PLAYBACK, selectionArg);
		for (int i = 0; i < ids.length; i++) {
			ContentValues column = new ContentValues();
			column.put(PlaylistTable.ID, ids[i]);
			column.put(PlaylistTable.POS, i);
			column.put(PlaylistTable.CARD, cardId);
			column.put(PlaylistTable.TYPE, type);
			db.insertWithOnConflict(PlaylistTable.TABLE, null, column, SQLiteDatabase.CONFLICT_REPLACE);
		}
		commit();
	}

	/**
	 *
	 */
	private interface PlaylistTable {
		/**
		 * table name
		 */
		String TABLE = "playlists";
		/**
		 * track ID
		 */
		String ID = "song_id";
		/**
		 * type of the playlist {@link #PLAYLIST_TYPE_PLAYBACK,#PLAYLIST_TYPE_HISTORY}
		 */
		String TYPE = "playlistType";
		/**
		 * track position in the playlist
		 */
		String POS = "position";
		/**
		 * ID of the external storage
		 */
		String CARD = "card_id";
	}
}