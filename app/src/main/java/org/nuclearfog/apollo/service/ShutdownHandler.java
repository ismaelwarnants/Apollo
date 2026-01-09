package org.nuclearfog.apollo.service;

import android.os.Handler;

/**
 * Handler used to shutdown (idle) playback service after timeout
 *
 * @author nuclearfog
 */
public class ShutdownHandler extends Handler implements Runnable {

	public static final int DELAY_LONG = 30000;

	public static final int DELAY_SHORT = 1000;

	private MusicPlaybackService service;

	private boolean active = false;

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
	 *
	 * @param delay delay in milliseconds {@link #DELAY_SHORT,#DELAY_LONG}
	 */
	public void start(int delay) {
		if (!active) {
			active = postDelayed(this, delay);
		}
	}

	/**
	 * abort running scheduled shutdown
	 */
	public void stop() {
		removeCallbacks(this);
		active = false;
	}
}