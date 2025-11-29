package org.nuclearfog.apollo.async.loader;

import android.content.Context;

import org.nuclearfog.apollo.async.AsyncExecutor;
import org.nuclearfog.apollo.model.Album;
import org.nuclearfog.apollo.store.RecentStore;

import java.util.List;

/**
 * Used to return the last listened to albums.
 *
 * @author Andrew Neal (andrewdneal@gmail.com)
 * @author nuclearfog
 */
public class RecentLoader extends AsyncExecutor<Void, List<Album>> {

	private RecentStore recentStore;


	public RecentLoader(Context context) {
		super(context);
		recentStore = RecentStore.getInstance(context);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected List<Album> doInBackground(Void v) {
		return recentStore.getRecentAlbums();
	}
}