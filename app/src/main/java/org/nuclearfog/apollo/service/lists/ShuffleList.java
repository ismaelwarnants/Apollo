package org.nuclearfog.apollo.service.lists;

import androidx.annotation.NonNull;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

/**
 * Threadsafe implementation of a shuffle list, containing indexes of tracks in the {@link PlaybackList}
 *
 * @author nuclearfog
 */
public class ShuffleList {

	/**
	 * shuffle list containing random indexes
	 */
	private final LinkedList<Integer> mShuffle = new LinkedList<>();
	/**
	 * random generator used for shuffle
	 */
	private Random mRandom = new Random();
	private int index = -1;

	/**
	 * get size of this list
	 *
	 * @return item count
	 */
	public int size() {
		synchronized (mShuffle) {
			return mShuffle.size();
		}
	}

	/**
	 * set index of the selected item
	 *
	 * @param index new index
	 */
	public void setIndex(int index) {
		if (index >= 0 && index < mShuffle.size()) {
			this.index = index;
		} else {
			this.index = -1;
		}
	}

	/**
	 * get the next shuffle position and increase index
	 *
	 * @return shuffle position
	 */
	public int next() {
		synchronized (mShuffle) {
			if (!mShuffle.isEmpty() && index >= 0 && index < mShuffle.size()) {
				index = ++index % mShuffle.size();
				return mShuffle.get(index);
			}
		}
		return -1;
	}

	/**
	 * clears this list and resets index
	 */
	public void clear() {
		synchronized (mShuffle) {
			mShuffle.clear();
			index = -1;
		}
	}

	/**
	 * create a new shuffle list with a new size.
	 * Using a history to move listened track indexes to the end
	 *
	 * @param history a list of played track indexes
	 * @param size    new size of the shuffle list
	 */
	public void shuffle(List<Integer> history, int size) {
		synchronized (mShuffle) {
			clear();
			if (size > 0) {
				index = 0;
				for (int index = 0; index < size; index++) {
					mShuffle.add(index);
				}
				Collections.shuffle(mShuffle, mRandom);
				if (!history.isEmpty()) {
					for (int i = 0; i < mShuffle.size(); i++) {
						if (history.contains(mShuffle.get(i))) {
							int item = mShuffle.remove(i);
							mShuffle.add(item);
						}
					}
				}
			}
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@NonNull
	@Override
	public String toString() {
		return "size=" + size() + " index=" + index;
	}
}