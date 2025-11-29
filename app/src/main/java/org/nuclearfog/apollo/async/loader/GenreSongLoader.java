package org.nuclearfog.apollo.async.loader;

import android.content.Context;
import android.database.Cursor;
import android.util.Log;

import org.nuclearfog.apollo.async.AsyncExecutor;
import org.nuclearfog.apollo.model.Song;
import org.nuclearfog.apollo.utils.ApolloUtils;
import org.nuclearfog.apollo.utils.CursorFactory;

import java.util.LinkedList;
import java.util.List;

/**
 * Used to return the songs for a particular genre.
 *
 * @author Andrew Neal (andrewdneal@gmail.com)
 * @author nuclearfog
 */
public class GenreSongLoader extends AsyncExecutor<String, List<Song>> {

	private static final String TAG = "GenreSongLoader";


	public GenreSongLoader(Context context) {
		super(context);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected List<Song> doInBackground(String param) {
		List<Song> result = new LinkedList<>();
		Context context = getContext();
		if (context != null && param != null) {
			long[] genreIds = ApolloUtils.readSerializedIDs(param);
			try {
				for (long genreId : genreIds) {
					// Create the Cursor
					if (genreId == 0)
						continue;
					Cursor mCursor = CursorFactory.makeGenreSongCursor(context, genreId);
					// Gather the data
					if (mCursor != null) {
						if (mCursor.moveToFirst()) {
							do {
								// Copy the song Id
								long id = mCursor.getLong(0);
								// Copy the song name
								String songName = mCursor.getString(1);
								// Copy the artist name
								String artist = mCursor.getString(2);
								// Copy the album name
								String album = mCursor.getString(3);
								// Copy the duration
								long duration = mCursor.getLong(4);
								// Create a new song
								Song song = new Song(id, songName, artist, album, duration);
								// Add everything up
								result.add(song);
							} while (mCursor.moveToNext());
						}
						mCursor.close();
					}
				}
			} catch (Exception exception) {
				Log.e(TAG, "error loading songs from genre:", exception);
			}
		}
		return result;
	}
}