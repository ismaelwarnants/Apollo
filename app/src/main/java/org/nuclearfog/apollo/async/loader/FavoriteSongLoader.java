package org.nuclearfog.apollo.async.loader;

import android.content.Context;

import org.nuclearfog.apollo.async.AsyncExecutor;
import org.nuclearfog.apollo.model.Song;
import org.nuclearfog.apollo.store.FavoritesStore;

import java.util.List;

/**
 * Used to query the {@link FavoritesStore} for the tracks marked as favorites.
 *
 * @author Andrew Neal (andrewdneal@gmail.com)
 * @author nuclearfog
 */
public class FavoriteSongLoader extends AsyncExecutor<Void, List<Song>> {

	private FavoritesStore favoriteStore;


	public FavoriteSongLoader(Context context) {
		super(context);
		favoriteStore = FavoritesStore.getInstance(context);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected List<Song> doInBackground(Void v) {
		return favoriteStore.getFavorites();
	}
}