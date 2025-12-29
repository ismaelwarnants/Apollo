package org.nuclearfog.apollo.ui.widgets;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.widget.RemoteViews;

import org.nuclearfog.apollo.BuildConfig;
import org.nuclearfog.apollo.R;
import org.nuclearfog.apollo.cache.ImageFetcher;
import org.nuclearfog.apollo.service.MusicPlaybackService;
import org.nuclearfog.apollo.ui.activities.AudioPlayerActivity;
import org.nuclearfog.apollo.ui.activities.HomeActivity;

/**
 * 4x2 App-Widget
 *
 * @author Andrew Neal (andrewdneal@gmail.com)
 * @author nuclearfog
 */
public class AppWidgetLargeAlt extends AppWidgetBase {

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
		RemoteViews appWidgetView = new RemoteViews(BuildConfig.APPLICATION_ID, R.layout.app_widget_large_alt);
		// Set the titles and artwork
		if (album != null && song != null) {
			ImageFetcher imageFetcher = new ImageFetcher(context);
			appWidgetView.setTextViewText(R.id.app_widget_large_alternate_line_one, song.getName());
			appWidgetView.setTextViewText(R.id.app_widget_large_alternate_line_two, album.getArtist());
			appWidgetView.setTextViewText(R.id.app_widget_large_alternate_line_three, album.getName());
			appWidgetView.setImageViewBitmap(R.id.app_widget_large_alternate_image, imageFetcher.getAlbumArtwork(album));
		}
		// Set correct drawable for pause state
		if (isPlaying) {
			appWidgetView.setImageViewResource(R.id.app_widget_large_alternate_play, R.drawable.btn_playback_pause);
			appWidgetView.setContentDescription(R.id.app_widget_large_alternate_play, context.getString(R.string.accessibility_pause));
		} else {
			appWidgetView.setImageViewResource(R.id.app_widget_large_alternate_play, R.drawable.btn_playback_play);
			appWidgetView.setContentDescription(R.id.app_widget_large_alternate_play, context.getString(R.string.accessibility_play));
		}
		// Set the correct drawable and color for the repeat state
		if (repeatMode == MusicPlaybackService.REPEAT_CURRENT) {
			appWidgetView.setImageViewResource(R.id.app_widget_large_alternate_repeat, R.drawable.btn_playback_repeat_one);
			appWidgetView.setInt(R.id.app_widget_large_alternate_repeat, "setColorFilter", Color.WHITE);
			appWidgetView.setContentDescription(R.id.app_widget_large_alternate_repeat, context.getString(R.string.accessibility_repeat_one));
		} else if (repeatMode == MusicPlaybackService.REPEAT_ALL) {
			appWidgetView.setImageViewResource(R.id.app_widget_large_alternate_repeat, R.drawable.btn_playback_repeat);
			appWidgetView.setInt(R.id.app_widget_large_alternate_repeat, "setColorFilter", Color.WHITE);
			appWidgetView.setContentDescription(R.id.app_widget_large_alternate_repeat, context.getString(R.string.accessibility_repeat_all));
		} else {
			appWidgetView.setImageViewResource(R.id.app_widget_large_alternate_repeat, R.drawable.btn_playback_repeat);
			appWidgetView.setInt(R.id.app_widget_large_alternate_repeat, "setColorFilter", Color.GRAY);
			appWidgetView.setContentDescription(R.id.app_widget_large_alternate_repeat, context.getString(R.string.accessibility_repeat));
		}
		// Set the correct drawable color for the shuffle state
		appWidgetView.setImageViewResource(R.id.app_widget_large_alternate_shuffle, R.drawable.btn_playback_shuffle);
		appWidgetView.setContentDescription(R.id.app_widget_large_alternate_shuffle, context.getString(R.string.accessibility_shuffle));
		if (shuffleMode == MusicPlaybackService.SHUFFLE_NONE) {
			appWidgetView.setInt(R.id.app_widget_large_alternate_shuffle, "setColorFilter", Color.GRAY);
		} else {
			appWidgetView.setInt(R.id.app_widget_large_alternate_shuffle, "setColorFilter", Color.WHITE);
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
		views.setOnClickPendingIntent(R.id.app_widget_large_alternate_info_container, pendingIntent);
		views.setOnClickPendingIntent(R.id.app_widget_large_alternate_image, pendingIntent);
		// Shuffle modes
		pendingIntent = createPlaybackControlIntent(context, MusicPlaybackService.ACTION_SHUFFLE);
		views.setOnClickPendingIntent(R.id.app_widget_large_alternate_shuffle, pendingIntent);
		// Previous track
		pendingIntent = createPlaybackControlIntent(context, MusicPlaybackService.ACTION_PREVIOUS);
		views.setOnClickPendingIntent(R.id.app_widget_large_alternate_previous, pendingIntent);
		// Play and pause
		pendingIntent = createPlaybackControlIntent(context, MusicPlaybackService.ACTION_TOGGLEPAUSE);
		views.setOnClickPendingIntent(R.id.app_widget_large_alternate_play, pendingIntent);
		// Next track
		pendingIntent = createPlaybackControlIntent(context, MusicPlaybackService.ACTION_NEXT);
		views.setOnClickPendingIntent(R.id.app_widget_large_alternate_next, pendingIntent);
		// Repeat modes
		pendingIntent = createPlaybackControlIntent(context, MusicPlaybackService.ACTION_REPEAT);
		views.setOnClickPendingIntent(R.id.app_widget_large_alternate_repeat, pendingIntent);
	}
}