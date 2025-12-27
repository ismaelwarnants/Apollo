package org.nuclearfog.apollo.ui.widgets;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.provider.MediaStore;
import android.widget.RemoteViews;

import org.nuclearfog.apollo.BuildConfig;
import org.nuclearfog.apollo.R;
import org.nuclearfog.apollo.model.Album;
import org.nuclearfog.apollo.receiver.WidgetBroadcastReceiver;
import org.nuclearfog.apollo.service.MusicPlaybackService;
import org.nuclearfog.apollo.service.RecentWidgetService;
import org.nuclearfog.apollo.ui.activities.AudioPlayerActivity;
import org.nuclearfog.apollo.ui.activities.HomeActivity;
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
	 * tag used to identify this widget
	 * @see WidgetBroadcastReceiver#WIDGET_TYPE
	 */
	public static final String TYPE = "app_widget_recents_update";

	/**
	 * Bundle key used to add the actions {@link #ACTION_OPEN_PROFILE} & {@link #ACTION_PLAY_ALBUM}
	 */
	public static final String KEY_ACTION = "set_action";
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

	private static final int REQUEST_RECENT = 0x5103;

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
		for (int appWidgetId : appWidgetIds) {
			// Create the remote views
			RemoteViews mViews = new RemoteViews(BuildConfig.APPLICATION_ID, R.layout.app_widget_recents);
			// Link actions buttons to intents
			linkButtons(context, mViews, false);
			// fill list with recent albums
			Intent recentIntent = new Intent(context, RecentWidgetService.class);
			recentIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
			recentIntent.setData(Uri.parse(recentIntent.toUri(Intent.URI_INTENT_SCHEME)));
			mViews.setRemoteAdapter(R.id.app_widget_recents_list, recentIntent);
			// init playback control
			Intent updateIntent = new Intent(MusicPlaybackService.SERVICECMD);
			updateIntent.putExtra(WidgetBroadcastReceiver.WIDGET_TYPE, TYPE);
			updateIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds);
			updateIntent.setFlags(Intent.FLAG_RECEIVER_REGISTERED_ONLY);
			context.sendBroadcast(updateIntent);
			// register this class
			Intent intent = new Intent(context, AppWidgetRecent.class);
			intent.setAction(ACTION_CLICK);
			intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
			intent.setData(Uri.parse(intent.toUri(Intent.URI_INTENT_SCHEME)));
			PendingIntent onClickPendingIntent = PendingIntent.getBroadcast(context, REQUEST_RECENT, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE);
			mViews.setPendingIntentTemplate(R.id.app_widget_recents_list, onClickPendingIntent);
			// Update the widget
			appWidgetManager.updateAppWidget(appWidgetId, mViews);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onReceive(Context context, Intent intent) {
		super.onReceive(context, intent);
		context = context.getApplicationContext();
		if (ACTION_CLICK.equals(intent.getAction())) {
			long albumId = intent.getLongExtra(Constants.ID, -1L);
			String action = intent.getStringExtra(KEY_ACTION);
			// Play the selected album
			if (ACTION_PLAY_ALBUM.equals(action)) {
				Intent shortcutIntent = new Intent(context, ShortcutActivity.class);
				shortcutIntent.setAction(Intent.ACTION_VIEW);
				shortcutIntent.putExtra(Constants.ID, albumId);
				shortcutIntent.putExtra(Constants.MIME_TYPE, MediaStore.Audio.Albums.CONTENT_TYPE);
				shortcutIntent.putExtra(ShortcutActivity.OPEN_AUDIO_PLAYER, false);
				shortcutIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
				context.startActivity(shortcutIntent);
			}
			// Open the album profile
			else if (ACTION_OPEN_PROFILE.equals(action)) {
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
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void notifyChange(MusicPlaybackService service, String what) {
		if (hasInstances(service)) {
			if (MusicPlaybackService.CHANGED_PLAYSTATE.equals(what)) {
				performUpdate(service, null);
			} else if (MusicPlaybackService.CHANGED_META.equals(what)) {
				Handler handler = new Handler(service.getMainLooper());
				handler.post(() -> {
					Context context = service.getApplicationContext();
					AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
					ComponentName componentName = new ComponentName(context, AppWidgetRecent.class);
					appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetManager.getAppWidgetIds(componentName), R.id.app_widget_recents_list);
				});
			}
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void performUpdate(MusicPlaybackService service, int[] appWidgetIds) {
		RemoteViews mViews = new RemoteViews(BuildConfig.APPLICATION_ID, R.layout.app_widget_recents);
		boolean isPlaying = service.isPlaying();
		// set button drawable
		mViews.setImageViewResource(R.id.app_widget_recents_play, isPlaying ? R.drawable.btn_playback_pause : R.drawable.btn_playback_play);
		// Link actions buttons to intents
		linkButtons(service, mViews, isPlaying);
		// Update the app-widget
		pushUpdate(service, getClass(), appWidgetIds, mViews);
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