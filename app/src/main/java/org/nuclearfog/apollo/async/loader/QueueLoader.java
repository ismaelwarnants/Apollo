package org.nuclearfog.apollo.async.loader;

import android.content.Context;
import android.database.Cursor;

import org.nuclearfog.apollo.async.AsyncExecutor;
import org.nuclearfog.apollo.model.Song;
import org.nuclearfog.apollo.utils.CursorFactory;

import java.util.LinkedList;
import java.util.List;

/**
 * Used to return the current playlist or queue.
 *
 * @author Andrew Neal (andrewdneal@gmail.com)
 * @author nuclearfog
 */
public class QueueLoader extends AsyncExecutor<List<Long>, List<Song>> {


	public QueueLoader(Context context) {
		super(context);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected List<Song> doInBackground(final List<Long> param) {
		List<Song> result = new LinkedList<>();
		Context context = getContext();
		if (context != null) {
			for (long id : param) {
				// Create the Cursor
				Cursor cursor = CursorFactory.makeTrackCursor(context, id);
				// Gather the data
				if (cursor != null) {
					if (cursor.moveToFirst()) {
						// Copy the song name
						String songName = cursor.getString(1);
						// Copy the artist name
						String artist = cursor.getString(2);
						// Copy the album name
						String album = cursor.getString(3);
						// Copy the duration
						long duration = cursor.getLong(4);
						// Create a new song
						Song song = new Song(id, songName, artist, album, duration);
						// Add everything up
						result.add(song);
					}
					cursor.close();
				}
			}
		}
		return result;
	}
}