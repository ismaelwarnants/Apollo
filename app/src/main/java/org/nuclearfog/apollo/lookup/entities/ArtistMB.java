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
	private String area = "";


	public ArtistMB(JSONObject json) {
		mbid = json.optString("id", "");
		name = json.optString("name", "");
		JSONObject areaJson = json.optJSONObject("area");
		if (areaJson != null) {
			area = areaJson.optString("name", "");
		}
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

	/**
	 * The artist area, indicates the area with which an artist is primarily identified with. (e.g. birth/formation country)
	 *
	 * @return area name
	 * @noinspection unused
	 */
	public String getArea() {
		return area;
	}


	@NonNull
	@Override
	public String toString() {
		return "name=" + name;
	}
}