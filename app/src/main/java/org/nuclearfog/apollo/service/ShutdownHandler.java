package org.nuclearfog.apollo.service;

import android.os.Handler;
import android.util.Log;

/**
 * Handler used to shutdown (idle) playback service after timeout
 *
 * @author nuclearfog
 */
public class ShutdownHandler extends Handler implements Runnable {

	private static final String TAG = "ShutdownHandler";

	/**
	 * Idle time in milliseconds before stopping the foreground notification
	 */
	private static final long IDLE_DELAY = 30000L;

	private MusicPlaybackService service;

	/**
	 * @param service callback to playback service
	 */
	public ShutdownHandler(MusicPlaybackService service) {
		super(service.getMainLooper());
		this.service = service;
	}


	@Override
	public void run() {
		service.releaseService(false);
		Log.d(TAG, "shutdown executed.");
	}

	/**
	 * start scheduled shutdown
	 */
	public void start() {
		removeCallbacks(this);
		postDelayed(this, IDLE_DELAY);
		Log.d(TAG, "shutdown in (s): " + IDLE_DELAY);
	}

	/**
	 * abort running scheduled shutdown
	 */
	public void stop() {
		removeCallbacks(this);
		Log.d(TAG, "shutdown stopped.");
	}
}