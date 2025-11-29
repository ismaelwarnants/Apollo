package org.nuclearfog.apollo.ui.widgets;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.widget.RemoteViews;

import androidx.annotation.NonNull;

import org.nuclearfog.apollo.BuildConfig;
import org.nuclearfog.apollo.R;
import org.nuclearfog.apollo.cache.ImageFetcher;
import org.nuclearfog.apollo.model.Album;
import org.nuclearfog.apollo.model.Song;
import org.nuclearfog.apollo.service.MusicPlaybackService;
import org.nuclearfog.apollo.ui.activities.AudioPlayerActivity;
import org.nuclearfog.apollo.ui.activities.HomeActivity;

/**
 * 4x2 App-Widget
 *
 * @author Andrew Neal (andrewdneal@gmail.com)
 */
public class AppWidgetLargeAlternate extends AppWidgetBase {

	public static final String CMDAPPWIDGETUPDATE = "app_widget_large_alternate_update";


	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
		defaultAppWidget(context, appWidgetIds);
		Intent updateIntent = new Intent(MusicPlaybackService.SERVICECMD);
		updateIntent.putExtra(MusicPlaybackService.CMDNAME, AppWidgetLargeAlternate.CMDAPPWIDGETUPDATE);
		updateIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds);
		updateIntent.setFlags(Intent.FLAG_RECEIVER_REGISTERED_ONLY);
		context.sendBroadcast(updateIntent);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void notifyChange(MusicPlaybackService service, String what) {
		if (hasInstances(service)) {
			if (MusicPlaybackService.CHANGED_META.equals(what)
					|| MusicPlaybackService.CHANGED_PLAYSTATE.equals(what)
					|| MusicPlaybackService.CHANGED_REPEATMODE.equals(what)
					|| MusicPlaybackService.CHANGED_SHUFFLEMODE.equals(what)) {
				performUpdate(service, null);
			}
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void performUpdate(MusicPlaybackService service, int[] appWidgetIds) {
		RemoteViews appWidgetView = getRemoteViews(service);

		// Set correct drawable for pause state
		boolean isPlaying = service.isPlaying();
		if (isPlaying) {
			appWidgetView.setImageViewResource(R.id.app_widget_large_alternate_play, R.drawable.btn_playback_pause);
			appWidgetView.setContentDescription(R.id.app_widget_large_alternate_play, service.getString(R.string.accessibility_pause));
		} else {
			appWidgetView.setImageViewResource(R.id.app_widget_large_alternate_play, R.drawable.btn_playback_play);
			appWidgetView.setContentDescription(R.id.app_widget_large_alternate_play, service.getString(R.string.accessibility_play));
		}
		// Set the correct drawable for the repeat state
		switch (service.getRepeatMode()) {
			case MusicPlaybackService.REPEAT_ALL:
				appWidgetView.setImageViewResource(R.id.app_widget_large_alternate_repeat, R.drawable.btn_playback_repeat_all);
				break;

			case MusicPlaybackService.REPEAT_CURRENT:
				appWidgetView.setImageViewResource(R.id.app_widget_large_alternate_repeat, R.drawable.btn_playback_repeat_one);
				break;

			default:
				appWidgetView.setImageViewResource(R.id.app_widget_large_alternate_repeat, R.drawable.btn_playback_repeat);
				break;
		}
		// Set the correct drawable for the shuffle state
		switch (service.getShuffleMode()) {
			case MusicPlaybackService.SHUFFLE_NONE:
				appWidgetView.setImageViewResource(R.id.app_widget_large_alternate_shuffle, R.drawable.btn_playback_shuffle);
				break;
			case MusicPlaybackService.SHUFFLE_AUTO:
			default:
				appWidgetView.setImageViewResource(R.id.app_widget_large_alternate_shuffle, R.drawable.btn_playback_shuffle_all);
				break;
		}
		// Link actions buttons to intents
		linkButtons(service, appWidgetView, isPlaying);
		// Update the app-widget
		pushUpdate(service, getClass(), appWidgetIds, appWidgetView);
	}


	@NonNull
	private static RemoteViews getRemoteViews(MusicPlaybackService service) {
		RemoteViews appWidgetView = new RemoteViews(BuildConfig.APPLICATION_ID, R.layout.app_widget_large_alternate);
		Album album = service.getCurrentAlbum();
		Song song = service.getCurrentSong();
		if (album != null && song != null) {
			ImageFetcher imageFetcher = new ImageFetcher(service);
			Bitmap bitmap = imageFetcher.getAlbumArtwork(album);
			// Set the titles and artwork
			appWidgetView.setTextViewText(R.id.app_widget_large_alternate_line_one, song.getName());
			appWidgetView.setTextViewText(R.id.app_widget_large_alternate_line_two, album.getArtist());
			appWidgetView.setTextViewText(R.id.app_widget_large_alternate_line_three, album.getName());
			appWidgetView.setImageViewBitmap(R.id.app_widget_large_alternate_image, bitmap);
		}
		return appWidgetView;
	}

	/**
	 * Initialize given widgets to default state, where we launch Music on
	 * default click and hide actions if service not running.
	 */
	private void defaultAppWidget(Context context, int[] appWidgetIds) {
		RemoteViews appWidgetViews = new RemoteViews(BuildConfig.APPLICATION_ID, R.layout.app_widget_large_alternate);
		linkButtons(context, appWidgetViews, false);
		pushUpdate(context, getClass(), appWidgetIds, appWidgetViews);
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