package org.nuclearfog.apollo.async.loader;

import android.content.Context;
import android.database.Cursor;
import android.util.Log;

import org.nuclearfog.apollo.async.AsyncExecutor;
import org.nuclearfog.apollo.model.Song;
import org.nuclearfog.apollo.utils.Constants;
import org.nuclearfog.apollo.utils.CursorFactory;

import java.util.LinkedList;
import java.util.List;

/**
 * Used to return the Song the user added over the past four of weeks.
 *
 * @author Andrew Neal (andrewdneal@gmail.com)
 * @author nuclearfog
 */
public class LastAddedLoader extends AsyncExecutor<Void, List<Song>> {

	private static final String TAG = "LastAddedLoader";


	public LastAddedLoader(Context context) {
		super(context);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected List<Song> doInBackground(Void v) {
		List<Song> result = new LinkedList<>();
		Context context = getContext();
		if (context != null) {
			try {
				// Create the Cursor
				Cursor mCursor = CursorFactory.makeLastAddedCursor(context);
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
						} while (mCursor.moveToNext() && result.size() < Constants.LAST_ADDED_LIMIT);
					}
					mCursor.close();
				}
			} catch (Exception exception) {
				Log.e(TAG, "error loading songs:", exception);
			}
		}
		return result;
	}
}