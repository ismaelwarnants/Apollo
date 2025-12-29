package org.nuclearfog.apollo.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;

import org.nuclearfog.apollo.service.MusicPlaybackService;

/**
 * Broadcast receiver class used to detect status changes affecting playback
 *
 * @author nuclearfog
 */
public class ServiceBroadcastReceiver extends BroadcastReceiver {

	public static final String ACTION_WIDGET_UPDATE = "widget-update";

	private MusicPlaybackService service;

	/**
	 * @param service callback to playback service to stop playback
	 */
	public ServiceBroadcastReceiver(MusicPlaybackService service) {
		this.service = service;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onReceive(Context context, Intent intent) {
		String action = intent.getAction();
		if (AudioManager.ACTION_AUDIO_BECOMING_NOISY.equals(intent.getAction())) {
			service.pause(true);
		} else if (Intent.ACTION_MEDIA_EJECT.equals(action)) {
			service.onExternalStorageChanged(false);
		} else if (Intent.ACTION_MEDIA_MOUNTED.equals(action)) {
			service.onExternalStorageChanged(true);
		} else if (ACTION_WIDGET_UPDATE.equals(action)) {
			service.onWidgetUpdate();
		}
	}
}