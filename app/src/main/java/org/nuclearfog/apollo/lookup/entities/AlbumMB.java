package org.nuclearfog.apollo.lookup.entities;

import android.annotation.SuppressLint;

import androidx.annotation.NonNull;

import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Album entity of MusicBrainz API
 *
 * @author nuclearfog
 */
public class AlbumMB {

	@SuppressLint("SimpleDateFormat")
	private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");

	private String id;
	private String name;
	private long release;


	public AlbumMB(JSONObject json) {
		id = json.optString("id", "");
		name = json.optString("title", "");
		String dateStr = json.optString("first-release-date", "");

		try {
			Date date = DATE_FORMAT.parse(dateStr);
			if (date != null)
				release = date.getTime();
		} catch (ParseException e) {
			release = 0;
		}
	}

	/**
	 * get MBID of album
	 *
	 * @return MBID code
	 */
	public String getId() {
		return id;
	}

	/**
	 * get album name
	 *
	 * @return name
	 */
	public String getName() {
		return name;
	}

	/**
	 * get release date of the album
	 *
	 * @return release time
	 */
	public long getReleaseDate() {
		return release;
	}


	@NonNull
	@Override
	public String toString() {
		return "name=" + name;
	}
}