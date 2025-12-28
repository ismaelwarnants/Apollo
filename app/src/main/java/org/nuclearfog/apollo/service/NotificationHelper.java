package org.nuclearfog.apollo.service;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.Build.VERSION_CODES;
import android.support.v4.media.session.MediaSessionCompat;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationChannelCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.app.ServiceCompat;
import androidx.media.app.NotificationCompat.MediaStyle;

import org.nuclearfog.apollo.BuildConfig;
import org.nuclearfog.apollo.R;
import org.nuclearfog.apollo.cache.ImageFetcher;
import org.nuclearfog.apollo.model.Album;
import org.nuclearfog.apollo.model.Song;

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
	 *
	 */
	private ImageFetcher imageFetcher;

	/**
	 * callbacks to the service
	 */
	private PendingIntent callbackPlayPause, callbackNext, callbackPrevious, callbackStop;


	/**
	 * @param service  callback to the service
	 * @param mSession media session of the current playback
	 */
	NotificationHelper(MusicPlaybackService service, MediaSessionCompat mSession) {
		mService = service;
		imageFetcher = new ImageFetcher(service.getApplicationContext());

		// init notification manager & channel
		NotificationChannelCompat.Builder channelBuilder = new NotificationChannelCompat.Builder(NOTIFICATION_CHANNEL_ID, NotificationManagerCompat.IMPORTANCE_DEFAULT);
		channelBuilder.setName(NOTIFICATION_NAME).setLightsEnabled(false).setVibrationEnabled(false).setSound(null, null);
		notificationManager = NotificationManagerCompat.from(service);
		notificationManager.createNotificationChannel(channelBuilder.build());

		// initialize player activity callback
		Intent intent = new Intent(INTENT_AUDIO_PLAYER);
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

		// initialize callbacks
		callbackPlayPause = createIntent(MusicPlaybackService.ACTION_TOGGLEPAUSE);
		callbackNext = createIntent(MusicPlaybackService.ACTION_NEXT);
		callbackPrevious = createIntent(MusicPlaybackService.ACTION_PREVIOUS);
		callbackStop = createIntent(MusicPlaybackService.ACTION_STOP);
		PendingIntent contentIntent = PendingIntent.getActivity(mService, 0, intent, PendingIntent.FLAG_IMMUTABLE);

		// style for the notification
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
	@SuppressLint("InlinedApi")
	void createNotification() {
		ServiceCompat.startForeground(mService, APOLLO_MUSIC_SERVICE, buildNotification(), ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK);
	}

	/**
	 * update existing notification
	 */
	void updateNotification() {
		postNotification(buildNotification());
	}

	/**
	 * dismiss existing notification
	 */
	void dismissNotification() {
		postNotification(null);
	}

	/**
	 * Changes the playback controls in and out of a paused state
	 */
	private Notification buildNotification() {
		// Android 13+ uses PlaybackState & MediaMetadata to update notification data
		// no need to set playback information here
		if (Build.VERSION.SDK_INT < VERSION_CODES.TIRAMISU) {
			Album album = mService.getCurrentAlbum();
			Song song = mService.getCurrentSong();
			if (song != null && album != null) {
				notificationBuilder.setContentTitle(song.getName());
				notificationBuilder.setContentText(song.getArtist());
				notificationBuilder.setLargeIcon(imageFetcher.getAlbumArtwork(album));
			}
			notificationBuilder.clearActions();
			notificationBuilder.addAction(R.drawable.btn_playback_previous, "Previous", callbackPrevious);
			if (mService.isPlaying()) {
				notificationBuilder.addAction(R.drawable.btn_playback_pause, "Pause", callbackPlayPause);
			} else {
				notificationBuilder.addAction(R.drawable.btn_playback_play, "Play", callbackPlayPause);
			}
			notificationBuilder.addAction(R.drawable.btn_playback_next, "Next", callbackNext);
			notificationBuilder.addAction(R.drawable.btn_playback_stop, "Stop", callbackStop);
		}
		return notificationBuilder.build();
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

	/**
	 * create playback service intent
	 *
	 * @param action action send to playback service
	 */
	private PendingIntent createIntent(String action) {
		ComponentName serviceName = new ComponentName(mService, MusicPlaybackService.class);
		Intent intent = new Intent(action).setComponent(serviceName);
		return PendingIntent.getService(mService, 0, intent, PendingIntent.FLAG_MUTABLE);
	}
}