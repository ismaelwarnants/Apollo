package org.nuclearfog.apollo.lookup.entities;

import androidx.annotation.NonNull;

import org.json.JSONObject;

/**
 * Artist entity of MusicBrainz API
 *
 * @author nuclearfog
 */
public class ArtistMB {

	private String mbid;
	private String name;


	public ArtistMB(JSONObject json) {
		mbid = json.optString("id", "");
		name = json.optString("name", "");
	}

	/**
	 * get MBID of the artist
	 *
	 * @return mbid code
	 */
	public String getId() {
		return mbid;
	}

	/**
	 * get artist's name
	 *
	 * @return name
	 */
	public String getName() {
		return name;
	}


	@NonNull
	@Override
	public String toString() {
		return "name=" + name;
	}
}