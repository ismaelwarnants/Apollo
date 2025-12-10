package org.nuclearfog.apollo.service.lists;

import androidx.annotation.NonNull;

import java.util.Collections;
import java.util.LinkedList;
import java.util.Random;

/**
 * a Shuffle list used to randomize track positions of a playlist without modifying it.
 * Previously played tracks are stored into history
 *
 * @author nuclearfog
 */
public class ShuffleList {

	/**
	 * The max size allowed for the track history
	 */
	private static final int MAX_HISTORY_SIZE = 100;

	/**
	 * shuffle list containing random indexes
	 */
	private final LinkedList<Integer> mShuffle = new LinkedList<>();
	/**
	 * track position history used to play previously played tracks
	 */
	private final LinkedList<Integer> mHistory = new LinkedList<>();
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
			if (!mShuffle.isEmpty() && index >= 0) {
				if (index < mShuffle.size()) {
					return mShuffle.get(index++);
				}
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
	 * @param size    new size of the shuffle list
	 */
	public void shuffle(int size) {
		synchronized (mShuffle) {
			clear();
			if (size > 0) {
				index = 0;
				for (int index = 0; index < size; index++) {
					mShuffle.add(index);
				}
				Collections.shuffle(mShuffle, mRandom);
				synchronized (mHistory) {
					if (!mHistory.isEmpty()) {
						for (int i = 0; i < mShuffle.size(); i++) {
							if (mHistory.contains(mShuffle.get(i))) {
								int item = mShuffle.remove(i);
								mShuffle.add(item);
							}
						}
					}
				}
			}
		}
	}

	/**
	 * returns all previously played track positions
	 *
	 * @return array of track positions
	 */
	public int[] getHistory() {
		synchronized (mHistory) {
			int[] result = new int[mHistory.size()];
			for (int i = 0; i < result.length; i++) {
				result[i] = mHistory.get(i);
			}
			return result;
		}
	}

	/**
	 * replaces all previously played track positions
	 *
	 * @param history array of track positions
	 */
	public void setHistory(int[] history) {
		synchronized (mHistory) {
			mHistory.clear();
			for (int pos : history) {
				mHistory.add(pos);
			}
		}
	}

	/**
	 * add a new track position to history
	 *
	 * @param pos new track position to add
	 */
	public void addHistory(int pos) {
		synchronized (mHistory) {
			mHistory.add(pos);
			// clear old history entries when exceeding maximum capacity
			if (mHistory.size() > MAX_HISTORY_SIZE) {
				mHistory.removeFirst();
			}
		}
	}

	/**
	 * returns the earliest track position and remove ti from the history
	 *
	 * @return track position
	 */
	public int undoHistory() {
		synchronized (mHistory) {
			if (!mHistory.isEmpty()) {
				return mHistory.removeLast();
			}
			return -1;
		}
	}

	/**
	 * clears the history
	 */
	public void clearHistory() {
		synchronized (mHistory) {
			mHistory.clear();
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@NonNull
	@Override
	public String toString() {
		return "size=" + size() + " index=" + index + " history=" + mHistory.size();
	}
}