package org.nuclearfog.apollo.async.loader;

import android.content.Context;
import android.database.Cursor;
import android.util.Log;

import org.nuclearfog.apollo.async.AsyncExecutor;
import org.nuclearfog.apollo.model.Album;
import org.nuclearfog.apollo.utils.CursorFactory;

import java.util.LinkedList;
import java.util.List;

/**
 * Used to return the albums for a particular artist.
 *
 * @author Andrew Neal (andrewdneal@gmail.com)
 * @author nuclearfog
 */
public class ArtistAlbumLoader extends AsyncExecutor<Long, List<Album>> {

	private static final String TAG = "ArtistAlbumLoader";


	public ArtistAlbumLoader(Context context) {
		super(context);
	}


	@Override
	protected List<Album> doInBackground(Long param) {
		List<Album> result = new LinkedList<>();
		Context context = getContext();
		if (context != null && param != null) {
			try {
				// Create the Cursor
				Cursor mCursor = CursorFactory.makeArtistAlbumCursor(context, param);
				// Gather the dataS
				if (mCursor != null) {
					if (mCursor.moveToFirst()) {
						do {
							// Copy the album id
							long id = mCursor.getLong(0);
							// Copy the album name
							String albumName = mCursor.getString(1);
							// Copy the artist name
							String artist = mCursor.getString(2);
							// Copy the number of songs
							int songCount = mCursor.getInt(3);
							// Copy the release year
							String year = mCursor.getString(4);
							// Create a new album
							Album album = new Album(id, albumName, artist, songCount, year, true);
							// Add everything up
							result.add(album);
						} while (mCursor.moveToNext());
					}
					mCursor.close();
				}
			} catch (Exception exception) {
				Log.e(TAG, "error loading albums from artist", exception);
			}
		}
		return result;
	}
}