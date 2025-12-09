package org.nuclearfog.apollo.service.lists;

import androidx.annotation.NonNull;

import org.nuclearfog.apollo.utils.ApolloUtils;

import java.util.LinkedList;

/**
 * Threadsafe implementation of a playback list containing song IDs and playback position
 *
 * @author nuclearfog
 */
public class PlaybackList {

	/**
	 * list containing song IDs
	 */
	private final LinkedList<Long> mPlaylist = new LinkedList<>();

	/**
	 * index of the selected song ID
	 */
	private int playPos = -1;

	/**
	 * check if list is empty
	 *
	 * @return true if list is empty
	 */
	public boolean isEmpty() {
		synchronized (mPlaylist) {
			return mPlaylist.isEmpty();
		}
	}

	/**
	 * add song ID at the beginning of the list and move playback position
	 *
	 * @param id ID of the song to add
	 */
	public void addFirst(long id) {
		synchronized (mPlaylist) {
			mPlaylist.addFirst(id);
			if (playPos >= 0) {
				playPos++;
			}
		}
	}

	/**
	 * remove all entries
	 */
	public void clear() {
		synchronized (mPlaylist) {
			mPlaylist.clear();
			playPos = -1;
		}
	}

	/**
	 * get current playback position
	 *
	 * @return current position or -1 if not selected
	 */
	public int getPosition() {
		synchronized (mPlaylist) {
			return playPos;
		}
	}

	/**
	 * set playback position
	 *
	 * @param position position of the selected track
	 */
	public void setPosition(int position) {
		synchronized (mPlaylist) {
			if (position >= 0 && position < mPlaylist.size()) {
				playPos = position;
			} else {
				playPos = -1;
			}
		}
	}

	/**
	 * get entry count of this list
	 *
	 * @return number of entries
	 */
	public int size() {
		synchronized (mPlaylist) {
			return mPlaylist.size();
		}
	}

	/**
	 * get all song IDs of the list
	 *
	 * @return array of song IDs
	 */
	public long[] getItems() {
		synchronized (mPlaylist) {
			return ApolloUtils.toLongArray(mPlaylist);
		}
	}

	/**
	 * replace all song IDs
	 *
	 * @param items array of song IDs
	 */
	public void setItems(long[] items) {
		synchronized (mPlaylist) {
			mPlaylist.clear();
			playPos = -1;
			for (long item : items) {
				mPlaylist.add(item);
			}
		}
	}

	/**
	 * add song IDs at the end of the list
	 *
	 * @param items song IDs to add
	 */
	public void addItems(long[] items) {
		synchronized (mPlaylist) {
			for (long item : items) {
				mPlaylist.add(item);
			}
		}
	}

	/**
	 * add items behind the current playback position
	 *
	 * @param items song IDs to add
	 */
	public void addItemsToNext(long[] items) {
		synchronized (mPlaylist) {
			if (playPos >= 0 && playPos < mPlaylist.size()) {
				int pos = playPos;
				for (long item : items) {
					mPlaylist.add(pos++, item);
				}
			} else {
				addItems(items);
			}
		}
	}

	/**
	 * move specific item to another index
	 *
	 * @param from index of the entry to move from
	 * @param to   destiny index to move the item
	 */
	public void move(int from, int to) {
		synchronized (mPlaylist) {
			if (from != to && from >= 0 && from < mPlaylist.size() && to >= 0 && to <= mPlaylist.size()) {
				long trackId = mPlaylist.remove(from);
				mPlaylist.add(to, trackId);
				if (playPos == from) {
					playPos = to;
				} else if (playPos >= from && playPos <= to) {
					playPos--;
				} else if (playPos <= from && playPos >= to) {
					playPos++;
				}
			}
		}
	}

	/**
	 * remove specific item at index
	 *
	 * @param pos position of the item
	 */
	public void remove(int pos) {
		synchronized (mPlaylist) {
			if (pos >= 0 && pos < mPlaylist.size()) {
				mPlaylist.remove(pos);
				if (mPlaylist.isEmpty())
					playPos = -1;
				else if (playPos > pos)
					playPos--;
				else if (playPos == pos) {
					if (playPos > 0) {
						playPos = Math.min(playPos, size() - 1);
					}
				}
			}
		}
	}

	/**
	 * get current selected song ID
	 *
	 * @return song ID
	 */
	public long getCurrent() {
		synchronized (mPlaylist) {
			if (playPos >= 0 && playPos < size())
				return get(playPos);
			return -1L;
		}
	}

	/**
	 * get song ID at specific index
	 *
	 * @param index index of the song ID
	 * @return song ID or -1 if invalid
	 */
	public long get(int index) {
		synchronized (mPlaylist) {
			if (!mPlaylist.isEmpty() && index >= 0 && index < mPlaylist.size())
				return mPlaylist.get(index);
			return -1L;
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@NonNull
	@Override
	public String toString() {
		return "size=" + size() + " position=" + playPos;
	}
}