package org.nuclearfog.apollo.store;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import org.nuclearfog.apollo.model.Album;

import java.util.LinkedList;
import java.util.List;

/**
 * The {@link RecentStore} is used to display a a grid or list of
 * recently listened to albums. In order to populate the this grid or list with
 * the correct data, we keep a cache of the album ID, name, and time it was
 * played to be retrieved later.
 * <p>
 * In {@link org.nuclearfog.apollo.ui.activities.ProfileActivity}, when viewing the profile for an artist, the first
 * image the carousel header is the last album the user listened to for that
 * particular artist.
 *
 * @author Andrew Neal (andrewdneal@gmail.com)
 * @author nuclearfog
 */
public class RecentStore extends AppStore {

	/**
	 * SQL query to create table with recently listened albums
	 */
	private static final String CREATE_TABLE = "CREATE TABLE IF NOT EXISTS " + RecentStoreColumns.NAME + " ("
			+ RecentStoreColumns.ID + " LONG PRIMARY KEY,"
			+ RecentStoreColumns.ALBUMNAME + " TEXT NOT NULL,"
			+ RecentStoreColumns.ARTISTNAME + " TEXT NOT NULL,"
			+ RecentStoreColumns.ALBUMSONGCOUNT + " TEXT NOT NULL,"
			+ RecentStoreColumns.TIMEPLAYED + " LONG NOT NULL,"
			+ RecentStoreColumns.ALBUMYEAR + " TEXT);";

	/**
	 * column projection of recent tracks
	 */
	private static final String[] COLUMNS = {
			RecentStoreColumns.ID,
			RecentStoreColumns.ALBUMNAME,
			RecentStoreColumns.ARTISTNAME,
			RecentStoreColumns.ALBUMSONGCOUNT,
			RecentStoreColumns.ALBUMYEAR,
			RecentStoreColumns.TIMEPLAYED
	};

	/**
	 * select recent album by ID
	 */
	private static final String RECENT_SELECT_ID = RecentStoreColumns.ID + "=?";

	/**
	 * default sort order
	 */
	private static final String RECENT_ORDER = RecentStoreColumns.TIMEPLAYED + " DESC";

	/**
	 * Name of database file
	 */
	private static final String DB_NAME = "albumhistory.db";

	/**
	 * singleton instance of this class
	 */
	private static RecentStore sInstance;

	/**
	 * {@inheritDoc}
	 */
	private RecentStore(Context context) {
		super(context, DB_NAME);
	}

	/**
	 * initialize and returns singleton instance of this class
	 *
	 * @return A new instance of this class.
	 */
	public static RecentStore getInstance(Context context) {
		if (sInstance == null) {
			sInstance = new RecentStore(context.getApplicationContext());
		}
		return sInstance;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void onCreate(SQLiteDatabase db) {
		db.execSQL(CREATE_TABLE);
	}

	/**
	 * Used to store artist IDs in the database.
	 *
	 * @param album album to add
	 */
	public synchronized void addAlbum(Album album) {
		SQLiteDatabase database = getWritableDatabase();
		ContentValues values = new ContentValues(6);
		values.put(RecentStoreColumns.ID, album.getId());
		values.put(RecentStoreColumns.ALBUMNAME, album.getName());
		values.put(RecentStoreColumns.ARTISTNAME, album.getArtist());
		values.put(RecentStoreColumns.ALBUMSONGCOUNT, album.getTrackCount());
		values.put(RecentStoreColumns.ALBUMYEAR, album.getRelease());
		values.put(RecentStoreColumns.TIMEPLAYED, System.currentTimeMillis());
		database.insertWithOnConflict(RecentStoreColumns.NAME, null, values, SQLiteDatabase.CONFLICT_REPLACE);
		commit();
	}

	/**
	 * remove recent album from history
	 *
	 * @param albumId ID of the album to remove
	 */
	public synchronized void removeAlbum(long albumId) {
		String[] args = {Long.toString(albumId)};
		SQLiteDatabase database = getWritableDatabase();
		database.delete(RecentStoreColumns.NAME, RECENT_SELECT_ID, args);
		commit();
	}

	/**
	 * get all recent played albums
	 */
	public synchronized List<Album> getRecentAlbums() {
		SQLiteDatabase database = getReadableDatabase();
		Cursor cursor = database.query(RecentStoreColumns.NAME, COLUMNS, null, null, null, null, RECENT_ORDER);
		List<Album> result = new LinkedList<>();
		if (cursor.moveToFirst()) {
			do {
				long id = cursor.getLong(0);
				String name = cursor.getString(1);
				String artist = cursor.getString(2);
				int count = cursor.getInt(3);
				String year = cursor.getString(4);
				Album album = new Album(id, name, artist, count, year, true);
				result.add(album);
			} while (cursor.moveToNext());
		}
		cursor.close();
		return result;
	}

	/**
	 * table columns of recent played tracks
	 */
	private interface RecentStoreColumns {

		/* Table name */
		String NAME = "albumhistory";

		/* Album IDs column */
		String ID = "albumid";

		/* Album name column */
		String ALBUMNAME = "itemname";

		/* Artist name column */
		String ARTISTNAME = "artistname";

		/* Album song count column */
		String ALBUMSONGCOUNT = "albumsongcount";

		/* Album year column. It's okay for this to be null */
		String ALBUMYEAR = "albumyear";

		/* Time played column */
		String TIMEPLAYED = "timeplayed";
	}
}