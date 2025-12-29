package org.nuclearfog.apollo.ui.widgets;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.widget.RemoteViews;

import org.nuclearfog.apollo.BuildConfig;
import org.nuclearfog.apollo.R;
import org.nuclearfog.apollo.cache.ImageFetcher;
import org.nuclearfog.apollo.service.MusicPlaybackService;
import org.nuclearfog.apollo.ui.activities.AudioPlayerActivity;
import org.nuclearfog.apollo.ui.activities.HomeActivity;

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
			appWidgetView.setViewVisibility(R.id.app_widget_small_info_container, View.VISIBLE);
			appWidgetView.setTextViewText(R.id.app_widget_small_line_one, song.getName());
			appWidgetView.setTextViewText(R.id.app_widget_small_line_two, song.getArtist());
			appWidgetView.setImageViewBitmap(R.id.app_widget_small_image, imageFetcher.getAlbumArtwork(album));
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
	 * @param playerActive True if player is active in background, which means widget click will launch {@link AudioPlayerActivity}
	 */
	private void linkButtons(Context context, RemoteViews views, boolean playerActive) {
		// open player
		Intent action = new Intent(context, playerActive ? AudioPlayerActivity.class : HomeActivity.class);
		PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, action, PendingIntent.FLAG_IMMUTABLE);
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