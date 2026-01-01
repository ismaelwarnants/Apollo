package org.nuclearfog.apollo.ui.widgets;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.provider.MediaStore;
import android.widget.RemoteViews;

import org.nuclearfog.apollo.BuildConfig;
import org.nuclearfog.apollo.R;
import org.nuclearfog.apollo.model.Album;
import org.nuclearfog.apollo.service.MusicPlaybackService;
import org.nuclearfog.apollo.service.RecentWidgetService;
import org.nuclearfog.apollo.ui.activities.AudioPlayerActivity;
import org.nuclearfog.apollo.ui.activities.ProfileActivity;
import org.nuclearfog.apollo.ui.activities.ShortcutActivity;
import org.nuclearfog.apollo.utils.Constants;
import org.nuclearfog.apollo.utils.MusicUtils;

/**
 * App-Widget used to display a list of recently listened albums.
 *
 * @author Andrew Neal (andrewdneal@gmail.com)
 * @author nuclearfog
 */
public class AppWidgetRecent extends AppWidgetBase {

	/**
	 * Bundle key used to add the actions {@link #ACTION_OPEN_PROFILE} & {@link #ACTION_PLAY_ALBUM}
	 */
	public static final String KEY_SELECT = "set_action";
	/**
	 * open selected album with {@link ProfileActivity}
	 */
	public static final String ACTION_OPEN_PROFILE = "open_profile";
	/**
	 * play selected album
	 */
	public static final String ACTION_PLAY_ALBUM = "play_album";
	/**
	 *
	 */
	private static final String ACTION_CLICK = BuildConfig.APPLICATION_ID + ".recents.appwidget.action.CLICK";

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onReceive(Context context, Intent intent) {
		if (ACTION_CLICK.equals(intent.getAction())) {
			long albumId = intent.getLongExtra(Constants.ID, -1L);
			String option = intent.getStringExtra(KEY_SELECT);
			// Play the selected album
			if (ACTION_PLAY_ALBUM.equals(option)) {
				Intent shortcutIntent = new Intent(context, ShortcutActivity.class);
				shortcutIntent.setAction(Intent.ACTION_VIEW);
				shortcutIntent.putExtra(Constants.ID, albumId);
				shortcutIntent.putExtra(Constants.MIME_TYPE, MediaStore.Audio.Albums.CONTENT_TYPE);
				shortcutIntent.putExtra(ShortcutActivity.OPEN_AUDIO_PLAYER, false);
				shortcutIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
				context.startActivity(shortcutIntent);
			}
			// Open the album profile
			else if (ACTION_OPEN_PROFILE.equals(option)) {
				Album album = MusicUtils.getAlbumForId(context, albumId);
				Intent profileIntent = new Intent(context, ProfileActivity.class);
				profileIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
				profileIntent.putExtra(Constants.MIME_TYPE, MediaStore.Audio.Albums.CONTENT_TYPE);
				profileIntent.putExtra(Constants.NAME, intent.getStringExtra(Constants.NAME));
				profileIntent.putExtra(Constants.ARTIST_NAME, intent.getStringExtra(Constants.ARTIST_NAME));
				profileIntent.putExtra(Constants.ID, albumId);
				profileIntent.putExtra(Constants.ALBUM_YEAR, album != null ? album.getRelease() : "");
				context.startActivity(profileIntent);
			}
		}
		super.onReceive(context, intent);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
		// Create the remote views
		RemoteViews appWidgetView = new RemoteViews(BuildConfig.APPLICATION_ID, R.layout.app_widget_recents);
		// Link actions buttons to intents
		linkButtons(context, appWidgetView, false);
		// fill list with recent albums
		Intent recentIntent = new Intent(context, RecentWidgetService.class);
		recentIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds);
		appWidgetView.setRemoteAdapter(R.id.app_widget_recents_list, recentIntent);
		appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetIds, R.id.app_widget_recents_list);
		// init playback control
		if (isPlaying) {
			appWidgetView.setImageViewResource(R.id.app_widget_recents_play, R.drawable.btn_playback_pause);
			appWidgetView.setContentDescription(R.id.app_widget_recents_play, context.getString(R.string.accessibility_pause));
		} else {
			appWidgetView.setImageViewResource(R.id.app_widget_recents_play, R.drawable.btn_playback_play);
			appWidgetView.setContentDescription(R.id.app_widget_recents_play, context.getString(R.string.accessibility_play));
		}
		// register this class
		Intent intent = new Intent(context, AppWidgetRecent.class);
		intent.setAction(ACTION_CLICK);
		intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetIds);
		intent.setData(Uri.parse(intent.toUri(Intent.URI_INTENT_SCHEME)));
		PendingIntent onClickPendingIntent = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE);
		appWidgetView.setPendingIntentTemplate(R.id.app_widget_recents_list, onClickPendingIntent);
		// Update the widget
		appWidgetManager.updateAppWidget(appWidgetIds, appWidgetView);
		// Link actions buttons to intents
		linkButtons(context, appWidgetView, isPlaying);
	}

	/**
	 * Link up various button actions using {@link PendingIntent}.
	 *
	 * @param isPlaying True if player is active in background, which means widget click will launch {@link AudioPlayerActivity}
	 */
	private void linkButtons(Context context, RemoteViews views, boolean isPlaying) {
		// open player
		PendingIntent pendingIntent = createAudioPlayerIntent(context, isPlaying);
		views.setOnClickPendingIntent(R.id.app_widget_recents_action_bar, pendingIntent);
		// Previous track
		pendingIntent = createPlaybackControlIntent(context, MusicPlaybackService.ACTION_PREVIOUS);
		views.setOnClickPendingIntent(R.id.app_widget_recents_previous, pendingIntent);
		// Play and pause
		pendingIntent = createPlaybackControlIntent(context, MusicPlaybackService.ACTION_TOGGLEPAUSE);
		views.setOnClickPendingIntent(R.id.app_widget_recents_play, pendingIntent);
		// Next track
		pendingIntent = createPlaybackControlIntent(context, MusicPlaybackService.ACTION_NEXT);
		views.setOnClickPendingIntent(R.id.app_widget_recents_next, pendingIntent);
	}
}