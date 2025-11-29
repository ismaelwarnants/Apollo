package org.nuclearfog.apollo.model;

import androidx.annotation.NonNull;

import org.nuclearfog.apollo.utils.ApolloUtils;

import java.util.Arrays;

/**
 * A class that represents a genre.
 *
 * @author Andrew Neal (andrewdneal@gmail.com)
 * @author nuclearfog
 */
public class Genre extends Music implements Comparable<Genre> {

	private static final long serialVersionUID = 4073965515512657827L;

	/**
	 * IDs of a genre
	 */
	private final long[] ids;

	/**
	 * @param ids        IDs referencing to this genre
	 * @param genre_name The genre name
	 * @param visibility visibility of this genre
	 */
	public Genre(Long[] ids, String genre_name, boolean visibility) {
		super(-1L, genre_name, visibility);
		this.ids = ApolloUtils.toLongArray(ids);
	}

	/**
	 * return a set of genre IDs for the same Genre name
	 *
	 * @return ID array
	 */
	public long[] getGenreIds() {
		return ids;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int compareTo(@NonNull Genre g) {
		// sort genre by name
		return getName().compareToIgnoreCase(g.getName());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int hashCode() {
		int prime = 31;
		int result = 1;
		result = prime * result + Arrays.hashCode(ids);
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
		if (obj instanceof Genre) {
			Genre g = (Genre) obj;
			if (g.ids.length == ids.length) {
				for (int i = 0; i < ids.length; i++) {
					if (g.ids[i] != ids[i])
						return false;
				}
			}
			return g.getName().equals(getName());
		}
		return false;
	}
}