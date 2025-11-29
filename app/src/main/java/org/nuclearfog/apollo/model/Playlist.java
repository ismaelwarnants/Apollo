package org.nuclearfog.apollo.model;

/**
 * A class that represents a playlist.
 *
 * @author Andrew Neal (andrewdneal@gmail.com)
 * @author nuclearfog
 */
public class Playlist extends Music {

	private static final long serialVersionUID = -7247188259770481366L;

	/**
	 * unique ID to define this playlist as favorite list
	 */
	public static final long FAVORITE_ID = 0xF092D5DEB4A19EEL;

	/**
	 * unique ID to define this playlist as "last added" list
	 */
	public static final long LAST_ADDED_ID = 0xF57622096950ABBCL;

	/**
	 * unique ID to define this playlist as "most played" list
	 */
	public static final long POPULAR_ID = 0x502CDB3BD99EE393L;

	/**
	 * @param playlistId   The Id of the playlist
	 * @param playlistName The playlist name
	 */
	public Playlist(long playlistId, String playlistName) {
		super(playlistId, playlistName, true);
	}

	/**
	 * check if playlist is default playlist
	 *
	 * @return true if playlist is one of the default playlists
	 */
	public boolean isDefault() {
		return getId() == FAVORITE_ID || getId() == LAST_ADDED_ID || getId() == POPULAR_ID;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int hashCode() {
		int prime = 31;
		int result = 1;
		result = prime * result + (int) getId();
		result = prime * result + getName().hashCode();
		return result;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj instanceof Playlist) {
			Playlist playlist = (Playlist) obj;
			return getId() == playlist.getId() && getName().equals(playlist.getName());
		}
		return false;
	}
}