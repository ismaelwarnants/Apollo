package org.nuclearfog.apollo.async.loader;

import android.content.Context;

import org.nuclearfog.apollo.async.AsyncExecutor;
import org.nuclearfog.apollo.lookup.MusicBrainz;
import org.nuclearfog.apollo.lookup.entities.AlbumMB;

import java.util.ArrayList;
import java.util.List;

/**
 * Async loader for online album arts provided by MusicBrainz API
 *
 * @author nuclearfog
 */
public class AlbumArtLoader extends AsyncExecutor<String[], List<AlbumMB>> {

	/**
	 * maximum number of entries returned by the API
	 */
	private static final int MAX_RESULTS = 20;

	/**
	 *
	 */
	public AlbumArtLoader(Context context) {
		super(context);
	}


	@Override
	protected List<AlbumMB> doInBackground(String[] param) {
		if (param.length > 1 && param[1].length() > 2)
			return MusicBrainz.searchAlbumsByName(param[0], param[1], MAX_RESULTS);
		if (param[0].length() > 2)
			return MusicBrainz.searchAlbumsByName(param[0], "", MAX_RESULTS);
		return new ArrayList<>(0);
	}
}