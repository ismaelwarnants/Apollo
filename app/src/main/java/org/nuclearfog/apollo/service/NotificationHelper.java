package org.nuclearfog.apollo.service;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.support.v4.media.session.MediaSessionCompat;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationChannelCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.media.app.NotificationCompat.MediaStyle;

import org.nuclearfog.apollo.BuildConfig;
import org.nuclearfog.apollo.R;

/**
 * Builds the notification for Apollo's service. Jelly Bean and higher uses the
 * expanded notification by default.
 *
 * @author Andrew Neal (andrewdneal@gmail.com)
 * @author nuclearfog
 */
class NotificationHelper {

	private static final String TAG = "NotificationHelper";

	/**
	 * Notification ID
	 * use different notification IDs for each build to avoid conflicts
	 */
	private static final int APOLLO_MUSIC_SERVICE = BuildConfig.DEBUG ? 0x5D74E856 : 0x28E61796;

	/**
	 * Notification channel ID
	 */
	private static final String NOTIFICATION_CHANNEL_ID = BuildConfig.APPLICATION_ID + ".controlpanel";

	/**
	 * intent used to open audio player after clicking on notification
	 */
	private static final String INTENT_AUDIO_PLAYER = BuildConfig.APPLICATION_ID + ".AUDIO_PLAYER";

	/**
	 * Notification name
	 */
	private static final String NOTIFICATION_NAME = "Apollo Controlpanel";

	/**
	 * Service context
	 */
	private MusicPlaybackService mService;

	/**
	 * manage and update notification
	 */
	private NotificationManagerCompat notificationManager;

	/**
	 * Builder used to construct a notification
	 */
	private NotificationCompat.Builder notificationBuilder;

	/**
	 * @param service  callback to the service
	 * @param mSession media session of the current playback
	 */
	NotificationHelper(MusicPlaybackService service, MediaSessionCompat mSession) {
		mService = service;
		// initialize player activity callback
		Intent intent = new Intent(INTENT_AUDIO_PLAYER);
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		PendingIntent contentIntent = PendingIntent.getActivity(mService, 0, intent, PendingIntent.FLAG_IMMUTABLE);
		// init notification manager & channel
		NotificationChannelCompat.Builder channelBuilder = new NotificationChannelCompat.Builder(NOTIFICATION_CHANNEL_ID, NotificationManagerCompat.IMPORTANCE_DEFAULT);
		channelBuilder.setName(NOTIFICATION_NAME).setLightsEnabled(false).setVibrationEnabled(false).setSound(null, null);
		notificationManager = NotificationManagerCompat.from(service);
		notificationManager.createNotificationChannel(channelBuilder.build());
		// create style for the notification
		MediaStyle mediaStyle = new MediaStyle();
		mediaStyle.setMediaSession(mSession.getSessionToken());
		// create notification builder
		notificationBuilder = new NotificationCompat.Builder(mService, NOTIFICATION_CHANNEL_ID)
				.setSmallIcon(R.drawable.stat_notify_music)
				.setContentIntent(contentIntent)
				.setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
				.setPriority(NotificationCompat.PRIORITY_DEFAULT)
				.setCategory(NotificationCompat.CATEGORY_PROGRESS)
				.setWhen(System.currentTimeMillis())
				.setProgress(0, 0, true).setAutoCancel(false)
				.setShowWhen(false).setOngoing(true)
				.setSilent(true).setStyle(mediaStyle);
	}

	/**
	 * create a new notification and attach it to the foreground service
	 */
	void createNotification() {
		if (VERSION.SDK_INT >= VERSION_CODES.Q) {
			mService.startForeground(APOLLO_MUSIC_SERVICE, notificationBuilder.build(), ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK);
		} else {
			mService.startForeground(APOLLO_MUSIC_SERVICE, notificationBuilder.build());
		}
	}

	/**
	 * update existing notification
	 */
	void updateNotification() {
		postNotification(notificationBuilder.build());
	}

	/**
	 * dismiss existing notification
	 */
	void dismissNotification() {
		postNotification(null);
	}

	/**
	 * update/dismiss notification
	 *
	 * @param notification notification to post or null to remove existing notification
	 */
	private void postNotification(@Nullable Notification notification) {
		try {
			if (notification != null) {
				notificationManager.notify(APOLLO_MUSIC_SERVICE, notification);
			} else {
				notificationManager.cancel(APOLLO_MUSIC_SERVICE);
			}
		} catch (SecurityException exception) {
			Log.e(TAG, "error while updating notification");
		}
	}
}