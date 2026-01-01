package org.nuclearfog.apollo.ui.widgets;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.widget.RemoteViews;

import org.nuclearfog.apollo.BuildConfig;
import org.nuclearfog.apollo.R;
import org.nuclearfog.apollo.cache.ImageFetcher;
import org.nuclearfog.apollo.service.MusicPlaybackService;
import org.nuclearfog.apollo.ui.activities.AudioPlayerActivity;

/**
 * 4x1 App-Widget
 *
 * @author Andrew Neal (andrewdneal@gmail.com)
 * @author nuclearfog
 */
public class AppWidgetSmall extends AppWidgetBase {

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
		RemoteViews appWidgetView = new RemoteViews(BuildConfig.APPLICATION_ID, R.layout.app_widget_small);
		// update views
		if (song != null && album != null) {
			ImageFetcher imageFetcher = new ImageFetcher(context);
			appWidgetView.setTextViewText(R.id.app_widget_small_line_one, song.getName());
			appWidgetView.setTextViewText(R.id.app_widget_small_line_two, song.getArtist());
			appWidgetView.setImageViewBitmap(R.id.app_widget_small_image, imageFetcher.getAlbumArtwork(album));
		} else {
			appWidgetView.setTextViewText(R.id.app_widget_small_line_one, "");
			appWidgetView.setTextViewText(R.id.app_widget_small_line_two, "");
			appWidgetView.setImageViewResource(R.id.app_widget_small_image, R.drawable.default_artwork);
		}
		if (isPlaying) {
			appWidgetView.setImageViewResource(R.id.app_widget_small_play, R.drawable.btn_playback_pause);
			appWidgetView.setContentDescription(R.id.app_widget_small_play, context.getString(R.string.accessibility_pause));
		} else {
			appWidgetView.setImageViewResource(R.id.app_widget_small_play, R.drawable.btn_playback_play);
			appWidgetView.setContentDescription(R.id.app_widget_small_play, context.getString(R.string.accessibility_play));
		}
		// Link actions buttons to intents
		linkButtons(context, appWidgetView, isPlaying);
		// Update the app-widget
		pushUpdate(context, getClass(), appWidgetIds, appWidgetView);
	}

	/**
	 * Link up various button actions using {@link PendingIntent}.
	 *
	 * @param isPlaying True if player is active in background, which means widget click will launch {@link AudioPlayerActivity}
	 */
	private void linkButtons(Context context, RemoteViews views, boolean isPlaying) {
		// open player
		PendingIntent pendingIntent = createAudioPlayerIntent(context, isPlaying);
		views.setOnClickPendingIntent(R.id.app_widget_small_info_container, pendingIntent);
		views.setOnClickPendingIntent(R.id.app_widget_small_image, pendingIntent);
		// Previous track
		pendingIntent = createPlaybackControlIntent(context, MusicPlaybackService.ACTION_PREVIOUS);
		views.setOnClickPendingIntent(R.id.app_widget_small_previous, pendingIntent);
		// Play and pause
		pendingIntent = createPlaybackControlIntent(context, MusicPlaybackService.ACTION_TOGGLEPAUSE);
		views.setOnClickPendingIntent(R.id.app_widget_small_play, pendingIntent);
		// Next track
		pendingIntent = createPlaybackControlIntent(context, MusicPlaybackService.ACTION_NEXT);
		views.setOnClickPendingIntent(R.id.app_widget_small_next, pendingIntent);
	}
}