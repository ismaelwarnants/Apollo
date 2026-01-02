package org.nuclearfog.apollo.ui.widgets;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.RemoteViews;

import androidx.annotation.CallSuper;
import androidx.annotation.Nullable;

import org.nuclearfog.apollo.model.Album;
import org.nuclearfog.apollo.model.Song;
import org.nuclearfog.apollo.service.MusicPlaybackService;
import org.nuclearfog.apollo.ui.activities.AudioPlayerActivity;
import org.nuclearfog.apollo.ui.activities.HomeActivity;

/**
 * super class for all app widgets
 *
 * @author nuclearfog
 */
public abstract class AppWidgetBase extends AppWidgetProvider {

	/**
	 * track information to show
	 */
	protected Song song;
	/**
	 * album information to show
	 */
	protected Album album;
	/**
	 * playback status
	 */
	protected boolean isPlaying = false;
	/**
	 * shuffle/repeat status
	 */
	protected int repeatMode, shuffleMode;

	/**
	 * {@inheritDoc}
	 */
	@Override
	@CallSuper
	public void onReceive(Context context, Intent intent) {
		if (AppWidgetManager.ACTION_APPWIDGET_UPDATE.equals(intent.getAction())) {
			Bundle extras = intent.getExtras();
			if (extras != null && extras.containsKey(MusicPlaybackService.KEY_IS_PLAYING)) {
				song = (Song) extras.getSerializable(MusicPlaybackService.KEY_SONG);
				album = (Album) extras.getSerializable(MusicPlaybackService.KEY_ALBUM);
				isPlaying = extras.getBoolean(MusicPlaybackService.KEY_IS_PLAYING, false);
				repeatMode = extras.getInt(MusicPlaybackService.KEY_REPEAT, MusicPlaybackService.REPEAT_NONE);
				shuffleMode = extras.getInt(MusicPlaybackService.KEY_SHUFFLE, MusicPlaybackService.SHUFFLE_NONE);
			} else {
				// start PlaybackService to fetch playback information
				Intent serviceIntent = new Intent(context, MusicPlaybackService.class);
				serviceIntent.setAction(MusicPlaybackService.ACTION_WIDGET_UPDATE);
				serviceIntent.putExtra(MusicPlaybackService.EXTRA_FOREGROUND, false);
				context.startService(serviceIntent);
			}
		}
		super.onReceive(context, intent);
	}

	/**
	 * create pending intent used for playback control
	 *
	 * @param action type of playback control action used by {@link MusicPlaybackService}
	 * @return PendingIntent instance
	 */
	protected PendingIntent createPlaybackControlIntent(Context context, String action) {
		Intent intent = new Intent(context, MusicPlaybackService.class).setAction(action);
		return PendingIntent.getService(context, 0, intent, PendingIntent.FLAG_IMMUTABLE);
	}

	/**
	 * create pending intent used to open {@link HomeActivity} or {@link AudioPlayerActivity}
	 *
	 * @param isPlaying true to open {@link AudioPlayerActivity}
	 * @return PendingIntent instance
	 */
	protected PendingIntent createAudioPlayerIntent(Context context, boolean isPlaying) {
		Intent intent = new Intent(context, isPlaying ? AudioPlayerActivity.class : HomeActivity.class);
		return PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE);
	}

	/**
	 * send update to all installed widgets
	 */
	protected void pushUpdate(Context context, Class<?> widgetClass, @Nullable int[] appWidgetIds, RemoteViews views) {
		AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
		if (appWidgetIds != null) {
			appWidgetManager.updateAppWidget(appWidgetIds, views);
		} else {
			appWidgetManager.updateAppWidget(new ComponentName(context, widgetClass), views);
		}
	}
}