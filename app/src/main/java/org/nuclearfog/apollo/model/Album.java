package org.nuclearfog.apollo.model;

import android.database.Cursor;
import android.media.MediaMetadataRetriever;
import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * A class that represents an album.
 *
 * @author Andrew Neal (andrewdneal@gmail.com)
 * @author nuclearfog
 */
public class Album extends Music implements Parcelable {

	private static final long serialVersionUID = 4612921914269834447L;

	public static final Creator<? extends Album> CREATOR = new Creator<Album>() {

		@Override
		public Album createFromParcel(Parcel source) {
			long id = source.readLong();
			String name = source.readString();
			boolean visible = source.readInt() == 1;
			String artist = source.readString();
			int count = source.readInt();
			int release = source.readInt();
			return new Album(id, name, artist, count, release, visible);
		}


		@Override
		public Album[] newArray(int size) {
			return new Album[size];
		}
	};

	/**
	 * The album artist
	 */
	private String mArtistName = "";

	/**
	 * The number of songs in the album
	 */
	private int songNumber;

	/**
	 * The year the album was released
	 */
	private int year;

	/**
	 * @param cursor  cursor with fixed Column order {@link org.nuclearfog.apollo.utils.CursorFactory#ALBUM_COLUMN}
	 * @param visible Visibility of this album
	 */
	public Album(Cursor cursor, boolean visible) {
		this(cursor.getLong(0), cursor.getString(1), cursor.getString(2), cursor.getInt(3), cursor.getInt(4), visible);
	}

	/**
	 * @param albumId    The Id of the album
	 * @param albumName  The name of the album
	 * @param artistName The album artist
	 * @param songNumber The number of songs in the album
	 * @param year       The year the album was released
	 * @param visible    Visibility of this album
	 */
	public Album(long albumId, String albumName, String artistName, int songNumber, @Nullable String year, boolean visible) {
		this(albumId, albumName, artistName, songNumber, 0, visible);
		if (year != null && year.matches("\\d+")) {
			this.year = Integer.parseInt(year);
		}
	}


	/**
	 * @param albumId    The Id of the album
	 * @param albumName  The name of the album
	 * @param artistName The album artist
	 * @param songNumber The number of songs in the album
	 * @param year       The year the album was released
	 * @param visible    Visibility of this album
	 */
	public Album(long albumId, String albumName, String artistName, int songNumber, int year, boolean visible) {
		super(albumId, albumName, visible);
		if (artistName != null && !artistName.equals("<unknown>"))
			mArtistName = artistName;
		this.year = year;
		this.songNumber = songNumber;
	}

	/**
	 * @param mmr Metadata of the album
	 */
	public Album(MediaMetadataRetriever mmr) {
		this(-1, mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM),
				mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUMARTIST), 0, 0, true);
	}

	/**
	 * get artist name of the album
	 *
	 * @return name of the artist
	 */
	public String getArtist() {
		return mArtistName;
	}

	/**
	 * get number of tracks in this album
	 *
	 * @return number of tracks
	 */
	public int getTrackCount() {
		return songNumber;
	}

	/**
	 * get release date
	 *
	 * @return release date string or empty string if not defined
	 */
	public String getRelease() {
		if (year == 0)
			return "";
		return Integer.toString(year);
	}


	@Override
	public int describeContents() {
		return 0;
	}


	@Override
	public void writeToParcel(@NonNull Parcel dest, int flags) {
		dest.writeLong(getId());
		dest.writeString(getName());
		dest.writeInt(isVisible() ? 1 : 0);
		dest.writeString(getArtist());
		dest.writeInt(getTrackCount());
		dest.writeInt(year);
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
		result = prime * result + mArtistName.hashCode();
		result = prime * result + songNumber;
		result = prime * result + year;
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
		if (obj instanceof Album) {
			Album album = (Album) obj;
			return getId() == album.getId() && songNumber == album.songNumber &&
					getName().equals(album.getName()) && mArtistName.equals(album.mArtistName) && year == album.year;
		}
		return false;
	}
}