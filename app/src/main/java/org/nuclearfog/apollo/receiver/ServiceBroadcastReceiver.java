package org.nuclearfog.apollo.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;

import org.nuclearfog.apollo.service.MusicPlaybackService;

/**
 * Broadcast receiver used by {@link MusicPlaybackService} to receive status changes outside the app.
 *
 * @author nuclearfog
 */
public class ServiceBroadcastReceiver extends BroadcastReceiver {

	/**
	 * IntentFilter action used to trigger {@link MusicPlaybackService} to update the widgets
	 */
	public static final String ACTION_WIDGET_UPDATE = "widget-update";

	private OnStatusChangedListener listener;

	/**
	 * @param listener a listener to update status changes
	 */
	public ServiceBroadcastReceiver(OnStatusChangedListener listener) {
		this.listener = listener;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onReceive(Context context, Intent intent) {
		String action = intent.getAction();
		if (AudioManager.ACTION_AUDIO_BECOMING_NOISY.equals(intent.getAction())) {
			listener.onHeadphoneDisconnected();
		} else if (Intent.ACTION_MEDIA_EJECT.equals(action)) {
			listener.onExternalStorageChanged(false);
		} else if (Intent.ACTION_MEDIA_MOUNTED.equals(action)) {
			listener.onExternalStorageChanged(true);
		} else if (ACTION_WIDGET_UPDATE.equals(action)) {
			listener.onWidgetInstalled();
		}
	}

	/**
	 * create IntentFilter for this Broadcast receiver
	 */
	public IntentFilter createIntentFiler() {
		IntentFilter intentFilter = new IntentFilter();
		// init external storage intent filter
		intentFilter.addAction(Intent.ACTION_MEDIA_EJECT);
		intentFilter.addAction(Intent.ACTION_MEDIA_MOUNTED);
		// init widget listener
		intentFilter.addAction(ServiceBroadcastReceiver.ACTION_WIDGET_UPDATE);
		// init headset intent filter
		intentFilter.addAction(AudioManager.ACTION_AUDIO_BECOMING_NOISY);
		return intentFilter;
	}

	/**
	 * Listener used to update service on status changes
	 */
	public interface OnStatusChangedListener {

		/**
		 * called when headphones are disconnected
		 */
		void onHeadphoneDisconnected();

		/**
		 * called if an external storage was mounted or disconnected
		 *
		 * @param mount true if an external storage was connected
		 */
		void onExternalStorageChanged(boolean mount);

		/**
		 * called when an app widget was connected
		 */
		void onWidgetInstalled();
	}
}