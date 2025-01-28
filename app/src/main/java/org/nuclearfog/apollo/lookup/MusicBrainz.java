package org.nuclearfog.apollo.lookup;

import android.util.Log;

import androidx.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.nuclearfog.apollo.BuildConfig;
import org.nuclearfog.apollo.lookup.entities.AlbumMB;
import org.nuclearfog.apollo.lookup.entities.ArtistMB;
import org.nuclearfog.apollo.lookup.entities.Artwork;
import org.nuclearfog.apollo.utils.StringUtils;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import javax.net.ssl.HttpsURLConnection;

/**
 * MusicBrainz API
 *
 * @author nuclearfog
 */
public class MusicBrainz {

	private static final String TAG = "MusicBrainz";

	/**
	 * user agent used for the MusicBrainz API
	 */
	private static final String USER_AGENT = "Apollo/" + BuildConfig.VERSION_NAME + " (https://codeberg.org/nuclearfog/Apollo)";

	/**
	 * API host used to access music information
	 */
	private static final String API_MUSICBRAINZ = "https://musicbrainz.org/ws/2/";

	/**
	 * API host used to access cover art images
	 */
	private static final String API_COVERART_ARCHIVE = "https://coverartarchive.org/release/";

	private static final String ENDPOINT_SEARCH_ARTIST = "artist";
	private static final String ENDPOINT_SEARCH_ALBUM = "release";

	/**
	 * get artist information
	 *
	 * @param name artist name
	 * @return artist information of 'null' if not found
	 */
	@Nullable
	public static ArtistMB getArtist(String name) {
		List<ArtistMB> artists = searchArtists(name, 1);
		if (artists.isEmpty())
			return null;
		return artists.get(0);
	}

	/**
	 * get artist release information (album)
	 *
	 * @param name release/album name
	 * @return release information of 'null' if not found
	 */
	@Nullable
	public static AlbumMB getRelease(String name) {
		List<AlbumMB> album = searchAlbums(name, 1);
		if (album.isEmpty())
			return null;
		return album.get(0);
	}

	/**
	 * get release/album artwork
	 *
	 * @param mbid MusicBrainz ID of the release
	 * @return artwork information or 'null' if not found
	 */
	@Nullable
	public static Artwork getImage(String mbid) {
		try {
			JSONObject json = get(API_COVERART_ARCHIVE, mbid, new String[0]);
			return new Artwork(json);
		} catch (JSONException e) {
			Log.e(TAG, "error JSON-Format", e);
		}
		return null;
	}

	/**
	 * search for artists matching search string
	 *
	 * @param name  name of the artist to search
	 * @param count max result count
	 * @return list of artist matches
	 */
	public static List<ArtistMB> searchArtists(String name, int count) {
		try {
			String[] param = {"query=" + StringUtils.encodeUTF8(name), "limit=" + count, "inc=artist-rels"};
			JSONObject json = get(API_MUSICBRAINZ, ENDPOINT_SEARCH_ARTIST, param);
			JSONArray array = json.optJSONArray("artists");
			if (array != null) {
				List<ArtistMB> result = new LinkedList<>();
				for (int i = 0; i < array.length(); i++) {
					ArtistMB artist = new ArtistMB(array.getJSONObject(i));
					result.add(artist);
				}
				return result;
			}
		} catch (JSONException e) {
			Log.e(TAG, "error JSON-Format", e);
		}
		return new ArrayList<>();
	}

	/**
	 * search for albums matching search string
	 *
	 * @param name  name of the album to search
	 * @param count max result count
	 * @return list of album matches
	 */
	public static List<AlbumMB> searchAlbums(String name, int count) {
		try {
			String[] param = {"query=" + StringUtils.encodeUTF8(name), "limit=" + count};
			JSONObject json = get(API_MUSICBRAINZ, ENDPOINT_SEARCH_ALBUM, param);
			JSONArray array = json.optJSONArray("releases");
			if (array != null) {
				List<AlbumMB> result = new LinkedList<>();
				for (int i = 0; i < array.length(); i++) {
					AlbumMB album = new AlbumMB(array.getJSONObject(i));
					result.add(album);
				}
				return result;
			}
		} catch (Exception e) {
			Log.e(TAG, "error JSON-Format", e);
		}
		return new ArrayList<>();
	}

	/**
	 * access to GET endpoint
	 *
	 * @param host     hostname
	 * @param endpoint endpoint
	 * @param param    additional parameter
	 * @return json response
	 */
	@SuppressWarnings("CharsetObjectCanBeUsed")
	private static JSONObject get(String host, String endpoint, String[] param) throws JSONException {
		try {
			StringBuilder buf = new StringBuilder(host + endpoint);
			if (param.length > 0) {
				buf.append('?');
				for (String query : param) {
					buf.append(query).append('&');
				}
				buf.deleteCharAt(buf.length() - 1);
			}
			URL urlEndpoint = new URL(buf.toString());
			HttpsURLConnection connection = (HttpsURLConnection) urlEndpoint.openConnection();
			connection.setRequestProperty("User-Agent", USER_AGENT);
			connection.setRequestProperty("Accept", "application/json");
			if (connection.getResponseCode() == 200) {
				InputStream responseBody = connection.getInputStream();
				InputStreamReader responseBodyReader = new InputStreamReader(responseBody, "UTF-8");
				return new JSONObject(new BufferedReader(responseBodyReader).readLine());
			} else if (connection.getResponseCode() == 404) {
				Log.e(TAG, "url not found!");
			} else if (connection.getResponseCode() == 503) {
				Log.e(TAG, "rate limit exceeded!");
			}
		} catch (Exception e) {
			Log.e(TAG, "connection error", e);
		}
		throw new JSONException("no json object");
	}
}