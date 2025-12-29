package org.nuclearfog.apollo.service;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import org.nuclearfog.apollo.BuildConfig;
import org.nuclearfog.apollo.R;
import org.nuclearfog.apollo.cache.ImageFetcher;
import org.nuclearfog.apollo.model.Album;
import org.nuclearfog.apollo.store.RecentStore;
import org.nuclearfog.apollo.ui.widgets.AppWidgetRecent;
import org.nuclearfog.apollo.utils.Constants;

import java.util.ArrayList;
import java.util.List;

/**
 * This class is used to build the recently listened list for the
 * {@link AppWidgetRecent}.
 *
 * @author Andrew Neal (andrewdneal@gmail.com)
 */
public class RecentWidgetService extends RemoteViewsService {

	/**
	 * {@inheritDoc}
	 */
	@Override
	public RemoteViewsFactory onGetViewFactory(Intent intent) {
		return new WidgetRemoteViewsFactory(getApplicationContext());
	}

	/**
	 * This is the factory that will provide data to the collection widget.
	 */
	private static class WidgetRemoteViewsFactory implements RemoteViewsFactory {

		/**
		 * max recent item number
		 */
		private static final int RECENT_LIMIT = 20;

		/**
		 * Image cache
		 */
		private ImageFetcher mFetcher;
		/**
		 * database to the recent played albums
		 */
		private RecentStore recentStore;

		private List<Album> albums = new ArrayList<>();

		/**
		 * {@inheritDoc}
		 */
		WidgetRemoteViewsFactory(Context context) {
			mFetcher = new ImageFetcher(context);
			recentStore = RecentStore.getInstance(context);
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public void onCreate() {
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public void onDestroy() {
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public RemoteViews getLoadingView() {
			return null;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public int getCount() {
			return Math.min(albums.size(), RECENT_LIMIT);
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public long getItemId(int position) {
			return position;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public RemoteViews getViewAt(int position) {
			Album album = albums.get(position);
			// Create the remote views
			RemoteViews mViews = new RemoteViews(BuildConfig.APPLICATION_ID, R.layout.list_item_widget_recent);
			// Set the album names
			mViews.setTextViewText(R.id.app_widget_recents_line_one, album.getName());
			// Set the artist names
			mViews.setTextViewText(R.id.app_widget_recents_line_two, album.getArtist());
			// Set the album art
			Bitmap bitmap = mFetcher.getAlbumImage(album.getId());
			if (bitmap != null) {
				mViews.setImageViewBitmap(R.id.app_widget_recents_base_image, bitmap);
			} else {
				mViews.setImageViewResource(R.id.app_widget_recents_base_image, R.drawable.default_artwork);
			}
			// Open the profile of the touched album
			Intent profileIntent = new Intent();
			profileIntent.putExtra(Constants.ID, album.getId());
			profileIntent.putExtra(Constants.NAME, album.getName());
			profileIntent.putExtra(Constants.ARTIST_NAME, album.getArtist());
			profileIntent.putExtra(AppWidgetRecent.KEY_SELECT, AppWidgetRecent.ACTION_OPEN_PROFILE);
			mViews.setOnClickFillInIntent(R.id.app_widget_recents_items, profileIntent);
			// Play the album when the artwork is touched
			Intent playAlbumIntent = new Intent();
			playAlbumIntent.putExtra(Constants.ID, album.getId());
			playAlbumIntent.putExtra(AppWidgetRecent.KEY_SELECT, AppWidgetRecent.ACTION_PLAY_ALBUM);
			mViews.setOnClickFillInIntent(R.id.app_widget_recents_base_image, playAlbumIntent);
			return mViews;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public int getViewTypeCount() {
			return 1;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public boolean hasStableIds() {
			return true;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public void onDataSetChanged() {
			albums = recentStore.getRecentAlbums();
		}
	}
}