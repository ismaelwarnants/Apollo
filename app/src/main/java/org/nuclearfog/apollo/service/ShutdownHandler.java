package org.nuclearfog.apollo.service;

import android.os.Handler;

/**
 * Handler used to shutdown (idle) playback service after timeout
 *
 * @author nuclearfog
 */
public class ShutdownHandler extends Handler implements Runnable {

	/**
	 * Idle time in milliseconds before stopping the foreground notification
	 */
	public static final long IDLE_DELAY = 30000L;

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
	}

	/**
	 * start scheduled shutdown
	 */
	public void start() {
		removeCallbacks(this);
		postDelayed(this, IDLE_DELAY);
	}

	/**
	 * abort running scheduled shutdown
	 */
	public void stop() {
		removeCallbacks(this);
	}
}