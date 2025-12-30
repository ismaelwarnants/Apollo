package org.nuclearfog.apollo.service.lists;

import androidx.annotation.NonNull;

import java.util.Collections;
import java.util.LinkedList;
import java.util.Random;

/**
 * A Shuffle list containing randomized playlist positions. Every unique position is
 * stored into the history, so already played positions will be moved at the end of the list.
 *
 * @author nuclearfog
 */
public class ShuffleList {

	/**
	 * The max size allowed for the track history
	 */
	private static final int MAX_HISTORY_SIZE = 100;

	/**
	 * size of the first section of the shuffle list used to move already
	 * played tracks in the back (e.g. 2 = first half, 3 = first third)
	 */
	private static final int HISTORY_SECTION = 3;

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

	/**
	 * selected index of the shuffle list
	 */
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
	 * get the next shuffle position and increase index
	 *
	 * @return shuffle position or -1 if invalid
	 */
	public int next() {
		synchronized (mShuffle) {
			if (!mShuffle.isEmpty() && index >= 0) {
				if (++index >= mShuffle.size()) {
					shuffle(mShuffle.size());
					index = 0;
				}
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
			mHistory.clear();
			index = -1;
		}
	}

	/**
	 * create a new shuffle list with a new size.
	 * Using a history to move listened track indexes to the end
	 *
	 * @param size new size of the shuffle list
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
						// only use the first part of the shuffle list
						int sortSize = Math.min(mShuffle.size() / HISTORY_SECTION, mHistory.size());
						// put the played track indexes to the end of the shuffle list
						for (int i = 0; i < sortSize; i++) {
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
	public long[] getHistory() {
		synchronized (mHistory) {
			long[] result = new long[mHistory.size()];
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
	public void setHistory(long[] history) {
		synchronized (mHistory) {
			mHistory.clear();
			for (long pos : history) {
				mHistory.add((int) pos);
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