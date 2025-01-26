package org.nuclearfog.apollo.lookup.entities;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Artwork entity of MusicBrainz
 *
 * @author nuclearfog
 */
public class Artwork {

	private String url_full;
	private String url_thumb;


	public Artwork(JSONObject json) throws JSONException {
		JSONArray images = json.getJSONArray("images");
		if (images.length() > 0) {
			JSONObject image = images.getJSONObject(0);
			JSONObject thumbs = image.getJSONObject("thumbnails");
			String url_full = image.getString("image");
			String url_thumb = thumbs.getString("small");
			this.url_full = url_full.replace("http://", "https://");
			this.url_thumb = url_thumb.replace("http://", "https://");
		}
	}

	/**
	 * get preview image url
	 *
	 * @return url
	 */
	public String getThumbnailUrl() {
		return url_thumb;
	}

	/**
	 * get url of the full sized image
	 *
	 * @return url
	 */
	public String getImageUrl() {
		return url_full;
	}
}