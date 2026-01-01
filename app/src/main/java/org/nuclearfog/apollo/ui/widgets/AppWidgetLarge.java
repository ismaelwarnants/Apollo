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
 * 4x2 App-Widget
 *
 * @author Andrew Neal (andrewdneal@gmail.com)
 * @author nuclearfog
 */
public class AppWidgetLarge extends AppWidgetBase {

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
		RemoteViews appWidgetView = new RemoteViews(BuildConfig.APPLICATION_ID, R.layout.app_widget_large);
		// update views
		if (album != null && song != null) {
			ImageFetcher imageFetcher = new ImageFetcher(context);
			appWidgetView.setTextViewText(R.id.app_widget_large_line_one, song.getName());
			appWidgetView.setTextViewText(R.id.app_widget_large_line_two, song.getArtist());
			appWidgetView.setTextViewText(R.id.app_widget_large_line_three, song.getAlbum());
			appWidgetView.setImageViewBitmap(R.id.app_widget_large_image, imageFetcher.getAlbumArtwork(album));
		} else {
			appWidgetView.setTextViewText(R.id.app_widget_large_line_one, "");
			appWidgetView.setTextViewText(R.id.app_widget_large_line_two, "");
			appWidgetView.setTextViewText(R.id.app_widget_large_line_three, "");
			appWidgetView.setImageViewResource(R.id.app_widget_large_image, R.drawable.default_artwork);
		}
		if (isPlaying) {
			appWidgetView.setImageViewResource(R.id.app_widget_large_play, R.drawable.btn_playback_pause);
			appWidgetView.setContentDescription(R.id.app_widget_large_play, context.getString(R.string.accessibility_pause));
		} else {
			appWidgetView.setImageViewResource(R.id.app_widget_large_play, R.drawable.btn_playback_play);
			appWidgetView.setContentDescription(R.id.app_widget_large_play, context.getString(R.string.accessibility_play));
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
		views.setOnClickPendingIntent(R.id.app_widget_large_info_container, pendingIntent);
		views.setOnClickPendingIntent(R.id.app_widget_large_image, pendingIntent);
		// Previous track
		pendingIntent = createPlaybackControlIntent(context, MusicPlaybackService.ACTION_PREVIOUS);
		views.setOnClickPendingIntent(R.id.app_widget_large_previous, pendingIntent);
		// Play and pause
		pendingIntent = createPlaybackControlIntent(context, MusicPlaybackService.ACTION_TOGGLEPAUSE);
		views.setOnClickPendingIntent(R.id.app_widget_large_play, pendingIntent);
		// Next track
		pendingIntent = createPlaybackControlIntent(context, MusicPlaybackService.ACTION_NEXT);
		views.setOnClickPendingIntent(R.id.app_widget_large_next, pendingIntent);
	}
}