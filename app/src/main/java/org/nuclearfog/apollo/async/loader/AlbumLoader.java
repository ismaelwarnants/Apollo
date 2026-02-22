package org.nuclearfog.apollo.async.loader;

import android.content.Context;
import android.database.Cursor;
import android.util.Log;

import org.nuclearfog.apollo.async.AsyncExecutor;
import org.nuclearfog.apollo.model.Album;
import org.nuclearfog.apollo.store.ExcludeStore;
import org.nuclearfog.apollo.store.ExcludeStore.Type;
import org.nuclearfog.apollo.utils.CursorFactory;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * Used to return the albums on a user's device.
 *
 * @author Andrew Neal (andrewdneal@gmail.com)
 * @author nuclearfog
 */
public class AlbumLoader extends AsyncExecutor<Void, List<Album>> {

	private static final String TAG = "AlbumLoader";


	public AlbumLoader(Context context) {
		super(context);
	}


	@Override
	protected List<Album> doInBackground(Void v) {
		List<Album> result = new LinkedList<>();
		Context context = getContext();
		if (context != null) {
			ExcludeStore exclude_db = ExcludeStore.getInstance(context);
			try {
				// init filter list
				Set<Long> excludedIds = exclude_db.getIds(Type.ALBUM);
				// Create the Cursor
				Cursor mCursor = CursorFactory.makeAlbumCursor(context);
				// Gather the data
				if (mCursor != null) {
					if (mCursor.moveToFirst()) {
						do {
							long id = mCursor.getLong(0);
							boolean visible = !excludedIds.contains(id);
							Album album = new Album(mCursor, visible);
							result.add(album);
						} while (mCursor.moveToNext());
					}
					mCursor.close();
				}
			} catch (Exception exception) {
				Log.e(TAG, "error loading albums", exception);
			}
		}
		return result;
	}
}