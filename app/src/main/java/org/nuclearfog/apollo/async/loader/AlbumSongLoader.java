package org.nuclearfog.apollo.async.loader;

import android.content.Context;
import android.database.Cursor;
import android.util.Log;

import org.nuclearfog.apollo.async.AsyncExecutor;
import org.nuclearfog.apollo.model.Song;
import org.nuclearfog.apollo.utils.CursorFactory;

import java.util.LinkedList;
import java.util.List;

/**
 * Used to query Audio and return the Song for a particular album.
 *
 * @author Andrew Neal (andrewdneal@gmail.com)
 * @author nuclearfog
 */
public class AlbumSongLoader extends AsyncExecutor<Long, List<Song>> {

	private static final String TAG = "AlbumSongLoader";


	public AlbumSongLoader(Context context) {
		super(context);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected List<Song> doInBackground(Long param) {
		List<Song> result = new LinkedList<>();
		Context context = getContext();
		if (context != null && param != null) {
			try {
				// Create the Cursor
				Cursor mCursor = CursorFactory.makeAlbumSongCursor(context, param);
				// Gather the data
				if (mCursor != null) {
					if (mCursor.moveToFirst()) {
						do {
							result.add(new Song(mCursor, true));
						} while (mCursor.moveToNext());
					}
					mCursor.close();
				}
			} catch (Exception exception) {
				Log.e(TAG, "error loading songs from album", exception);
			}
		}
		return result;
	}
}