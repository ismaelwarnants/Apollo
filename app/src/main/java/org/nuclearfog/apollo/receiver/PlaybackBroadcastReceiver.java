package org.nuclearfog.apollo.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import org.nuclearfog.apollo.service.MusicPlaybackService;

/**
 * A Broadcast receiver used by Activities to receive updates from {@link MusicPlaybackService}
 * through {@link PlayStatusListener}
 *
 * @author nuclearfog
 */
public class PlaybackBroadcastReceiver extends BroadcastReceiver {

	/**
	 * callback reference
	 */
	private PlayStatusListener callback;

	/**
	 * @param callback callback listener to update information
	 */
	public PlaybackBroadcastReceiver(PlayStatusListener callback) {
		this.callback = callback;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onReceive(Context context, Intent intent) {
		String action = intent.getAction();
		if (action != null) {
			switch (action) {
				case MusicPlaybackService.CHANGED_META:
					callback.onMetaChange();
					break;

				case MusicPlaybackService.CHANGED_PLAYSTATE:
					callback.onStateChange();
					break;

				case MusicPlaybackService.CHANGED_REPEATMODE:
				case MusicPlaybackService.CHANGED_SHUFFLEMODE:
					callback.onModeChange();
					break;

				case MusicPlaybackService.ACTION_REFRESH:
					callback.refresh();
					break;
			}
		}
	}

	/**
	 * create IntentFilter for this BroadcastReceiver class
	 */
	public IntentFilter getFilter() {
		IntentFilter intentFilter = new IntentFilter();
		// Play and pause changes
		intentFilter.addAction(MusicPlaybackService.CHANGED_PLAYSTATE);
		// Shuffle and repeat changes
		intentFilter.addAction(MusicPlaybackService.CHANGED_SHUFFLEMODE);
		intentFilter.addAction(MusicPlaybackService.CHANGED_REPEATMODE);
		intentFilter.addAction(MusicPlaybackService.CHANGED_QUEUE);
		// Track changes
		intentFilter.addAction(MusicPlaybackService.CHANGED_META);
		// Update a list, probably the playlist fragment's
		intentFilter.addAction(MusicPlaybackService.ACTION_REFRESH);
		return intentFilter;
	}

	/**
	 * callback listener for status change
	 */
	public interface PlayStatusListener {

		/**
		 * called when meta information changes
		 */
		void onMetaChange();

		/**
		 * called when play state changes
		 */
		void onStateChange();

		/**
		 * called when mode changes between repeat and shuffle
		 */
		void onModeChange();

		/**
		 *
		 */
		void refresh();
	}
}