package org.nuclearfog.apollo.model;

/**
 * A class that represents an artist.
 *
 * @author Andrew Neal (andrewdneal@gmail.com)
 * @author nuclearfog
 */
public class Artist extends Music {

	private static final long serialVersionUID = -5566436937675713728L;

	/**
	 * The number of albums for the artist
	 */
	private int mAlbumNumber;

	/**
	 * The number of songs for the artist
	 */
	private int mSongNumber;

	/**
	 * @param artistId    The Id of the artist
	 * @param artistName  The artist name
	 * @param songNumber  The number of songs for the artist
	 * @param albumNumber The number of albums for the artist
	 * @param visible     The visibility of the artist
	 */
	public Artist(long artistId, String artistName, int songNumber, int albumNumber, boolean visible) {
		super(artistId, artistName, visible);
		mSongNumber = songNumber;
		mAlbumNumber = albumNumber;
	}

	/**
	 * get album count of the artist
	 *
	 * @return number of albums by artist
	 */
	public int getAlbumCount() {
		return mAlbumNumber;
	}

	/**
	 * get track count of the artist
	 *
	 * @return number of the tracks by artist
	 */
	public int getTrackCount() {
		return mSongNumber;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int hashCode() {
		int prime = 31;
		int result = 1;
		result = prime * result + mAlbumNumber;
		result = prime * result + (int) getId();
		result = prime * result + getName().hashCode();
		result = prime * result + mSongNumber;
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
		if (obj instanceof Artist) {
			Artist artist = (Artist) obj;
			return getId() == artist.getId() && mAlbumNumber == artist.mAlbumNumber &&
					mSongNumber == artist.mSongNumber && getName().equals(artist.getName());
		}
		return false;
	}
}