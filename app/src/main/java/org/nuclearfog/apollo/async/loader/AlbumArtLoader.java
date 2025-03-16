package org.nuclearfog.apollo.async.loader;

import android.content.Context;

import org.nuclearfog.apollo.async.AsyncExecutor;
import org.nuclearfog.apollo.lookup.MusicBrainz;
import org.nuclearfog.apollo.lookup.entities.AlbumMB;

import java.util.List;

/**
 * @author nuclearfog
 */
public class AlbumArtLoader extends AsyncExecutor<String, List<AlbumMB>> {


	public AlbumArtLoader(Context context) {
		super(context);
	}


	@Override
	protected List<AlbumMB> doInBackground(String param) {
		return MusicBrainz.searchAlbumsByName(param, null, 20);
	}
}