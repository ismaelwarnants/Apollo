package org.nuclearfog.apollo.utils;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.database.Cursor;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.RemoteException;
import android.provider.MediaStore;
import android.provider.MediaStore.Audio.AudioColumns;
import android.provider.MediaStore.Audio.Media;
import android.provider.MediaStore.Audio.Playlists;
import android.provider.Settings;
import android.util.Log;
import android.view.Menu;
import android.view.SubMenu;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import org.nuclearfog.apollo.BuildConfig;
import org.nuclearfog.apollo.IApolloService;
import org.nuclearfog.apollo.R;
import org.nuclearfog.apollo.model.Album;
import org.nuclearfog.apollo.model.Artist;
import org.nuclearfog.apollo.model.Song;
import org.nuclearfog.apollo.receiver.PlaybackBroadcastReceiver;
import org.nuclearfog.apollo.service.MusicPlaybackService;
import org.nuclearfog.apollo.ui.appmsg.AppMsg;
import org.nuclearfog.apollo.ui.dialogs.DeleteTracksDialog;
import org.nuclearfog.apollo.utils.ServiceBinder.ServiceBinderCallback;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.WeakHashMap;

/**
 * A collection of helpers directly related to music or Apollo's service.
 *
 * @author Andrew Neal (andrewdneal@gmail.com)
 * @author nuclearfog
 */
public final class MusicUtils {

	private static final String TAG = "MusicUtils";

	/**
	 * repeat mode disabled
	 */
	public static final int REPEAT_NONE = 0;

	/**
	 * repeat playlist
	 */
	public static final int REPEAT_ALL = 1;

	/**
	 * repeat current track
	 */
	public static final int REPEAT_CURRENT = 2;

	/**
	 * shuffle mode disabled
	 */
	public static final int SHUFFLE_NONE = 10;

	/**
	 * shuffle playlist
	 */
	public static final int SHUFFLE_NORMAL = 11;

	/**
	 * shuffle all songs
	 */
	public static final int SHUFFLE_AUTO = 12;

	/**
	 * selection to remove track from playlist
	 */
	@SuppressWarnings("deprecation")
	private static final String PLAYLIST_REMOVE_TRACK = Playlists.Members.AUDIO_ID + "=?";

	/**
	 * code to request file deleting
	 * only for scoped storage
	 */
	public static final int REQUEST_DELETE_FILES = 0x8DA3;

	/**
	 * information about activities accessing this service interface
	 */
	private static WeakHashMap<Activity, ServiceBinder> mConnectionMap = new WeakHashMap<>(32);

	/**
	 *
	 */
	private MusicUtils() {
	}

	/**
	 * Bind {@link MusicPlaybackService} to the Activity
	 *
	 * @param activity the activity to bind to the playback service
	 * @param callback Callback called when the service is connected
	 */
	public static void bindToService(Activity activity, @Nullable ServiceBinderCallback callback) {
		ContextWrapper contextWrapper = new ContextWrapper(activity.getBaseContext());
		Intent intent = new Intent(activity, MusicPlaybackService.class);
		activity.startService(intent);
		ServiceBinder binder = new ServiceBinder(callback);
		if (contextWrapper.bindService(intent, binder, 0)) {
			mConnectionMap.put(activity, binder);
		}
		binder.stopForeground();
	}

	/**
	 * Unbind {@link MusicPlaybackService} from Activity
	 *
	 * @param activity activity to unbind the playback service
	 */
	public static void unbindFromService(Activity activity) {
		boolean isPlaying = isPlaying(activity);
		ServiceBinder mBinder = mConnectionMap.remove(activity);
		if (mBinder != null) {
			activity.unbindService(mBinder);
		}
		// start foreground service if application is in background
		if (mConnectionMap.isEmpty()) {
			if (isPlaying) {
				Intent intent = new Intent(activity, MusicPlaybackService.class);
				intent.putExtra(MusicPlaybackService.EXTRA_FOREGROUND, true);
				ContextCompat.startForegroundService(activity, intent);
			}
		}
	}

	/**
	 * switch to next track
	 */
	public static void next(Activity activity) {
		IApolloService service = getService(activity);
		if (service != null) {
			try {
				service.gotoNext();
			} catch (RemoteException exception) {
				Log.e(TAG, "next()", exception);
			}
		}
	}

	/**
	 * switch to previous track or repeat current track
	 */
	public static void previous(Activity activity) {
		IApolloService service = getService(activity);
		if (service != null) {
			try {
				service.gotoPrev();
			} catch (RemoteException exception) {
				Log.e(TAG, "previous()", exception);
			}
		}
	}

	/**
	 * toggle play/pause of the playback service
	 */
	public static void togglePlayPause(Activity activity) {
		IApolloService service = getService(activity);
		if (service != null) {
			try {
				if (service.isPlaying()) {
					service.pause(false);
				} else {
					service.play();
				}
			} catch (RemoteException exception) {
				Log.e(TAG, "togglePlayPause()", exception);
			}
		}
	}

	/**
	 * stops playback and releases background service
	 */
	public static void releaseService(Activity activity) {
		IApolloService service = getService(activity);
		if (service != null) {
			try {
				service.stop();
				service.releaseService();
			} catch (RemoteException exception) {
				Log.e(TAG, "releaseService()", exception);
			}
		}
	}

	/**
	 * Cycles through the repeat options.
	 *
	 * @return repeat mode {@link #REPEAT_ALL,#REPEAT_CURRENT,#REPEAT_NONE}
	 */
	public static int cycleRepeat(Activity activity) {
		IApolloService service = getService(activity);
		if (service != null) {
			try {
				switch (service.getRepeatMode()) {
					case MusicPlaybackService.REPEAT_NONE:
						service.setRepeatMode(MusicPlaybackService.REPEAT_ALL);
						return REPEAT_ALL;

					case MusicPlaybackService.REPEAT_ALL:
						service.setRepeatMode(MusicPlaybackService.REPEAT_CURRENT);
						if (service.getShuffleMode() != MusicPlaybackService.SHUFFLE_NONE)
							service.setShuffleMode(MusicPlaybackService.SHUFFLE_NONE);
						return REPEAT_CURRENT;

					case MusicPlaybackService.REPEAT_CURRENT:
						service.setRepeatMode(MusicPlaybackService.REPEAT_NONE);
						return REPEAT_NONE;
				}
			} catch (RemoteException exception) {
				Log.e(TAG, "cycleRepeat()", exception);
			}
		}
		return REPEAT_NONE;
	}

	/**
	 * Cycles through the shuffle options.
	 *
	 * @return shuffle mode {@link #SHUFFLE_NORMAL,#SHUFFLE_NONE}
	 */
	public static int cycleShuffle(Activity activity) {
		IApolloService service = getService(activity);
		if (service != null) {
			try {
				switch (service.getShuffleMode()) {
					case MusicPlaybackService.SHUFFLE_NONE:
						service.setShuffleMode(MusicPlaybackService.SHUFFLE_NORMAL);
						if (service.getRepeatMode() == MusicPlaybackService.REPEAT_CURRENT) {
							service.setRepeatMode(MusicPlaybackService.REPEAT_ALL);
						}
						return SHUFFLE_NORMAL;

					case MusicPlaybackService.SHUFFLE_NORMAL:
					case MusicPlaybackService.SHUFFLE_AUTO:
						service.setShuffleMode(MusicPlaybackService.SHUFFLE_NONE);
						return SHUFFLE_NONE;
				}
			} catch (RemoteException exception) {
				Log.e(TAG, "cycleShuffle()", exception);
			}
		}
		return SHUFFLE_NONE;
	}

	/**
	 * shuffle all available songs
	 */
	public static void shuffleAll(Activity activity) {
		IApolloService service = getService(activity);
		if (service != null) {
			try {
				service.setShuffleMode(MusicPlaybackService.SHUFFLE_AUTO);
			} catch (RemoteException exception) {
				Log.e(TAG, "shuffleAll()", exception);
			}
		}
	}

	/**
	 * @return True if we're playing music, false otherwise.
	 */
	public static boolean isPlaying(Activity activity) {
		IApolloService service = getService(activity);
		if (service != null) {
			try {
				return service.isPlaying();
			} catch (RemoteException exception) {
				Log.e(TAG, "isPlaying()", exception);
			}
		}
		return false;
	}

	/**
	 * get current shuffle mode
	 *
	 * @return The current shuffle mode {@link #SHUFFLE_NONE,#SHUFFLE_NORMAL,#SHUFFLE_AUTO}
	 */
	public static int getShuffleMode(Activity activity) {
		IApolloService service = getService(activity);
		if (service != null) {
			try {
				switch (service.getShuffleMode()) {
					case MusicPlaybackService.SHUFFLE_AUTO:
						return SHUFFLE_AUTO;

					case MusicPlaybackService.SHUFFLE_NORMAL:
						return SHUFFLE_NORMAL;

					case MusicPlaybackService.SHUFFLE_NONE:
						return SHUFFLE_NONE;
				}
			} catch (RemoteException exception) {
				Log.e(TAG, "getShuffleMode()", exception);
			}
		}
		return SHUFFLE_NONE;
	}

	/**
	 * @return The current repeat mode.
	 */
	public static int getRepeatMode(Activity activity) {
		IApolloService service = getService(activity);
		if (service != null) {
			try {
				switch (service.getRepeatMode()) {
					case MusicPlaybackService.REPEAT_ALL:
						return REPEAT_ALL;

					case MusicPlaybackService.REPEAT_NONE:
						return REPEAT_NONE;

					case MusicPlaybackService.REPEAT_CURRENT:
						return REPEAT_CURRENT;
				}
				return service.getRepeatMode();
			} catch (RemoteException exception) {
				Log.e(TAG, "getRepeatMode()", exception);
			}
		}
		return REPEAT_NONE;
	}

	/**
	 * @return The current track name.
	 */
	@Nullable
	public static Song getCurrentTrack(Activity activity) {
		IApolloService service = getService(activity);
		if (service != null) {
			try {
				return service.getCurrentTrack();
			} catch (RemoteException exception) {
				Log.e(TAG, "getCurrentTrack()", exception);
			}
		}
		return null;
	}

	/**
	 * @return The current album name.
	 */
	@Nullable
	public static Album getCurrentAlbum(Activity activity) {
		IApolloService service = getService(activity);
		if (service != null) {
			try {
				return service.getCurrentAlbum();
			} catch (RemoteException exception) {
				Log.e(TAG, "getCurrentAlbum()", exception);
			}
		}
		return null;
	}

	/**
	 * get the current audio session ID
	 *
	 * @return The audio session ID or 0 if not initialized or if an error occurred
	 */
	public static int getAudioSessionId(Activity activity) {
		IApolloService service = getService(activity);
		if (service != null) {
			try {
				return service.getAudioSessionId();
			} catch (RemoteException exception) {
				Log.e(TAG, "getAudioSessionId()", exception);
			}
		}
		return 0;
	}

	/**
	 * get all song IDs from the current playback list
	 *
	 * @return The queue.
	 */
	@NonNull
	public static long[] getQueue(Activity activity) {
		IApolloService service = getService(activity);
		if (service != null) {
			try {
				return service.getQueue();
			} catch (RemoteException exception) {
				Log.e(TAG, "getQueue()", exception);
			}
		}
		return new long[0];
	}

	/**
	 * remove track from the current playlist
	 *
	 * @param pos index of the track
	 */
	public static void removeQueueItem(Activity activity, int pos) {
		IApolloService service = getService(activity);
		if (service != null) {
			try {
				service.removeTrack(pos);
			} catch (RemoteException exception) {
				Log.e(TAG, "removeQueueItem()", exception);
			}
		}
	}

	/**
	 * get current selected song from playback list
	 *
	 * @return The position of the current track in the queue.
	 */
	public static int getQueuePosition(Activity activity) {
		IApolloService service = getService(activity);
		if (service != null) {
			try {
				return service.getQueuePosition();
			} catch (RemoteException exception) {
				Log.e(TAG, "getQueuePosition()", exception);
			}
		}
		return 0;
	}

	/**
	 * select new song from the current playback list
	 *
	 * @param position The position to move the queue to
	 */
	public static void setQueuePosition(Activity activity, int position) {
		IApolloService service = getService(activity);
		if (service != null) {
			try {
				service.setQueuePosition(position);
			} catch (RemoteException exception) {
				Log.e(TAG, "setQueuePosition()", exception);
			}
		}
	}

	/**
	 * play a local song file
	 *
	 * @param uri The source of the file
	 */
	public static void playFile(Activity activity, Uri uri) {
		IApolloService service = getService(activity);
		if (uri != null && service != null) {
			try {
				service.openFile(uri);
			} catch (RemoteException exception) {
				Log.e(TAG, "playFile()", exception);
			}
		}
	}

	/**
	 * play a song
	 *
	 * @param id ID of the track to play
	 */
	public static void play(Activity activity, long id) {
		playAll(activity, new long[]{id}, 0, false);
	}

	/**
	 * play all listed songs
	 *
	 * @param songs   songs to play
	 * @param shuffle True to force a shuffle, false otherwise.
	 */
	public static void playAll(Activity activity, List<Song> songs, boolean shuffle) {
		playAll(activity, MusicUtils.getIDsFromSongList(songs), 0, shuffle);
	}

	/**
	 * play all listed songs
	 *
	 * @param list         The list of songs to play.
	 * @param position     Specify where to start.
	 * @param forceShuffle True to force a shuffle, false otherwise.
	 */
	public static void playAll(Activity activity, long[] list, int position, boolean forceShuffle) {
		IApolloService service = getService(activity);
		if (service != null) {
			try {
				if (forceShuffle) {
					service.setShuffleMode(MusicPlaybackService.SHUFFLE_NORMAL);
					if (list.length == 1) {
						service.open(list, 0);
					} else if (list.length > 1) {
						service.open(list, new Random().nextInt(list.length - 1));
					} else {
						service.clearQueue();
					}
				} else {
					service.setShuffleMode(MusicPlaybackService.SHUFFLE_NONE);
					if (list.length > 0) {
						service.open(list, position);
					} else {
						service.clearQueue();
					}
				}
			} catch (RemoteException exception) {
				Log.e(TAG, "playAll()", exception);
			}
		}
	}

	/**
	 * load all songs from list and play the selected position
	 *
	 * @param adapter  list adapter containing songs
	 * @param position selected item position
	 */
	public static void playAllFromUserItemClick(Activity activity, ArrayAdapter<Song> adapter, int position) {
		if (position >= adapter.getViewTypeCount() - 1) {
			// if view type count is greater than 1, a header exists at first position
			// calculate position offset
			int off = (adapter.getViewTypeCount() - 1);
			// length of the array adapter
			int len = adapter.getCount();
			// calculate real position
			position -= off;
			// copy all IDs to an array
			long[] list = new long[len - off];
			for (int i = 0; i < list.length; i++) {
				list[i] = adapter.getItemId(i + off);
			}
			// play whole ID list
			playAll(activity, list, position, false);
		}
	}

	/**
	 * Returns the ID for an artist.
	 *
	 * @param name The name of the artist.
	 * @return The ID for an artist.
	 */
	public static long getIdForArtist(Context context, String name) {
		Cursor cursor = CursorFactory.makeArtistCursor(context, name);
		long id = -1L;
		if (cursor != null) {
			if (cursor.moveToFirst()) {
				id = cursor.getLong(0);
			}
			cursor.close();
		}
		return id;
	}

	/**
	 * get single entry from MediaStore using ID
	 *
	 * @param id artist ID
	 * @return artist entry or null if not found
	 */
	@Nullable
	public static Artist getArtistForId(Context context, long id) {
		Artist artist = null;
		Cursor cursor = CursorFactory.makeArtistCursor(context, id);
		if (cursor != null) {
			if (cursor.moveToFirst()) {
				String artistName = cursor.getString(1);
				int albumCount = cursor.getInt(2);
				int songCount = cursor.getInt(3);
				artist = new Artist(id, artistName, songCount, albumCount, true);
			}
			cursor.close();
		}
		return artist;
	}

	/**
	 * get single entry from MediaStore using ID
	 *
	 * @param id album ID
	 * @return album entry or null if not found
	 */
	@Nullable
	public static Album getAlbumForId(Context context, long id) {
		Album album = null;
		Cursor cursor = CursorFactory.makeArtistCursor(context, id);
		if (cursor != null) {
			if (cursor.moveToFirst()) {
				String albumName = cursor.getString(1);
				String artist = cursor.getString(2);
				int songCount = cursor.getInt(3);
				String year = cursor.getString(4);
				album = new Album(id, albumName, artist, songCount, year, true);
			}
			cursor.close();
		}
		return album;
	}

	/**
	 * @param name The name of the new playlist.
	 * @return A new playlist ID.
	 */
	@SuppressWarnings("deprecation")
	public static long createPlaylist(Activity activity, String name) {
		long id = -1;
		try {
			if (name != null && !name.isEmpty()) {
				boolean playlistExists = false;
				Cursor cursor = CursorFactory.makePlaylistCursor(activity, name);
				// check if playlist exists
				if (cursor != null) {
					playlistExists = cursor.moveToFirst();
					cursor.close();
				}
				if (!playlistExists) {
					ContentResolver resolver = activity.getContentResolver();
					ContentValues values = new ContentValues(1);
					values.put(Playlists.NAME, name);
					Uri uri = resolver.insert(Playlists.EXTERNAL_CONTENT_URI, values);
					if (uri != null && uri.getLastPathSegment() != null) {
						id = Long.parseLong(uri.getLastPathSegment());
					}
				}
			}
		} catch (RuntimeException exception) {
			AppMsg.makeText(activity, R.string.error_create_playlist, AppMsg.STYLE_CONFIRM).show();
		}
		return id;
	}

	/**
	 * add songs to an existing playlist
	 *
	 * @param songs      The song(s) to add.
	 * @param playlistId The id of the playlist being added to.
	 */
	public static void addToPlaylist(Activity activity, long playlistId, List<Song> songs) {
		addToPlaylist(activity, playlistId, MusicUtils.getIDsFromSongList(songs));
	}

	/**
	 * add song IDs to an existing playlist
	 *
	 * @param ids        The id of the song(s) to add.
	 * @param playlistId The id of the playlist being added to.
	 */
	@SuppressLint("InlinedApi")
	@SuppressWarnings("deprecation")
	public static void addToPlaylist(Activity activity, long playlistId, long... ids) {
		try {
			Uri uri = Playlists.Members.getContentUri(MediaStore.VOLUME_EXTERNAL, playlistId);
			Cursor cursor = CursorFactory.makePlaylistCursor(activity.getContentResolver(), uri);
			if (cursor != null) {
				if (cursor.moveToFirst()) {
					int base = cursor.getInt(0);
					int numInserted = 0;
					for (int offset = 0; offset < ids.length; offset += 1000) {
						int len = ids.length;
						if (offset + len > ids.length) {
							len = ids.length - offset;
						}
						ContentValues[] mContentValuesCache = new ContentValues[len];
						for (int i = 0; i < len; i++) {
							mContentValuesCache[i] = new ContentValues();
							mContentValuesCache[i].put(Playlists.Members.PLAY_ORDER, base + offset + i);
							mContentValuesCache[i].put(Playlists.Members.AUDIO_ID, ids[offset + i]);
						}
						numInserted += activity.getContentResolver().bulkInsert(uri, mContentValuesCache);
					}
					String message = activity.getResources().getQuantityString(R.plurals.NNNtrackstoplaylist, numInserted, numInserted);
					AppMsg.makeText(activity, message, AppMsg.STYLE_CONFIRM).show();
				}
				cursor.close();
			}
		} catch (RuntimeException exception) {
			// thrown when the app doesn't own the playlist
			AppMsg.makeText(activity, R.string.error_add_playlist, AppMsg.STYLE_CONFIRM).show();
		}
	}

	/**
	 * rename existing playlist
	 *
	 * @param id   ID of the playlist to rename
	 * @param name new playlist name
	 */
	@SuppressWarnings("deprecation")
	public static void renamePlaylist(Activity activity, long id, String name) {
		try {
			// setting new name
			ContentValues values = new ContentValues(1);
			values.put(Playlists.NAME, name);
			// update old playlist
			Uri uri = ContentUris.withAppendedId(Playlists.EXTERNAL_CONTENT_URI, id);
			ContentResolver resolver = activity.getContentResolver();
			resolver.update(uri, values, null, null);
		} catch (Exception exception) {
			// thrown when the app doesn't own the playlist
			AppMsg.makeText(activity, R.string.error_rename_playlist, AppMsg.STYLE_CONFIRM).show();
		}
	}

	/**
	 * move a track of a playlist to a new position
	 *
	 * @param playlistId ID of the playlist
	 * @param from       location of the track
	 * @param to         new location of the track
	 * @param off        the offset of the positions, or '0'
	 * @return true if playlist item was moved successfully
	 */
	public static boolean movePlaylistTrack(Context context, long playlistId, int from, int to, int off) {
		ContentResolver resolver = context.getContentResolver();
		try {
			return Playlists.Members.moveItem(resolver, playlistId, from - off, to - off);
		} catch (RuntimeException exception) {
			// thrown when the app doesn't own the playlist
			Log.w(TAG, "could not move playlist item!");
			return false;
		}
	}

	/**
	 * Removes a single track from a given playlist
	 *
	 * @param trackId    The id of the song to remove.
	 * @param playlistId The id of the playlist being removed from.
	 * @return true if playlist item was removed successfully
	 */
	@SuppressLint("InlinedApi")
	@SuppressWarnings("deprecation")
	public static boolean removeFromPlaylist(Activity activity, long trackId, long playlistId) {
		try {
			String[] args = {Long.toString(trackId)};
			Uri uri = Playlists.Members.getContentUri(MediaStore.VOLUME_EXTERNAL, playlistId);
			ContentResolver resolver = activity.getContentResolver();
			int count = resolver.delete(uri, PLAYLIST_REMOVE_TRACK, args);
			if (count > 0) {
				String message = activity.getResources().getQuantityString(R.plurals.NNNtracksfromplaylist, count, count);
				AppMsg.makeText(activity, message, AppMsg.STYLE_CONFIRM).show();
				return true;
			}
		} catch (RuntimeException exception) {
			// thrown when the app doesn't own the playlist
			Log.w(TAG, "could not remove playlist item!");
		}
		return false;
	}

	/**
	 * add a song to the current playback queue
	 *
	 * @param id ID of the song to add
	 */
	public static void addToQueue(Activity activity, long id) {
		addToQueue(activity, new long[]{id});
	}

	/**
	 * add songs to the current playback queue
	 *
	 * @param items songs to add
	 */
	public static void addToQueue(Activity activity, List<Song> items) {
		addToQueue(activity, getIDsFromSongList(items));
	}

	/**
	 * @param list The list to enqueue.
	 */
	public static void addToQueue(Activity activity, long[] list) {
		IApolloService service = getService(activity);
		if (service != null) {
			try {
				service.enqueue(list, MusicPlaybackService.MOVE_LAST);
				AppMsg.makeText(activity, R.plurals.NNNtrackstoqueue, list.length, AppMsg.STYLE_CONFIRM).show();
			} catch (RemoteException exception) {
				Log.e(TAG, "addToQueue()", exception);
			}
		}
	}

	/**
	 * @param id The song ID.
	 */
	public static void setRingtone(Activity activity, long id) {
		ContentResolver resolver = activity.getContentResolver();
		Uri uri = ContentUris.withAppendedId(Media.EXTERNAL_CONTENT_URI, id);
		// Set ringtone
		try {
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
				// check if app can set ringtone
				if (Settings.System.canWrite(activity)) {
					// set ringtone
					RingtoneManager.setActualDefaultRingtoneUri(activity, RingtoneManager.TYPE_RINGTONE, uri);
				} else {
					// explain why we need permission to write settings
					Toast.makeText(activity, R.string.explain_permission_write_settings, Toast.LENGTH_LONG).show();
					// open settings so user can set write permissions
					Intent intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS);
					intent.setData(Uri.parse("package:" + BuildConfig.APPLICATION_ID));
					activity.startActivity(intent);
					return;
				}
			} else {
				// set ringtone directly
				ContentValues values = new ContentValues(1);
				values.put(AudioColumns.IS_RINGTONE, true);
				resolver.update(uri, values, null, null);
				Settings.System.putString(resolver, Settings.System.RINGTONE, uri.toString());
			}
		} catch (RuntimeException exception) {
			Log.e(TAG, "setRingtone()", exception);
			return;
		}
		// print message if succeeded
		Cursor cursor = CursorFactory.makeTrackCursor(activity, id);
		if (cursor != null) {
			if (cursor.moveToFirst()) {
				// get title of the current track
				String title = cursor.getString(1);
				// truncate title
				if (title.length() > 20)
					title = title.substring(0, 17) + "...";
				String message = activity.getString(R.string.set_as_ringtone, title);
				AppMsg.makeText(activity, message, AppMsg.STYLE_CONFIRM).show();
			}
			cursor.close();
		}
	}

	/**
	 * @param from The index the item is currently at.
	 * @param to   The index the item is moving to.
	 */
	public static void moveQueueItem(Activity activity, int from, int to) {
		IApolloService service = getService(activity);
		if (service != null) {
			try {
				service.moveQueueItem(from, to);
			} catch (RemoteException exception) {
				Log.e(TAG, "moveQueueItem()", exception);
			}
		}
	}

	/**
	 * returns a list of song IDs from a playlist
	 *
	 * @param playlistId The playlist Id
	 * @return The track list for a playlist
	 */
	@NonNull
	public static long[] getSongListForPlaylist(Context context, long playlistId) {
		Cursor cursor = CursorFactory.makePlaylistSongCursor(context, playlistId);
		if (cursor != null) {
			cursor.moveToFirst();
			long[] ids = new long[cursor.getCount()];
			for (int i = 0; i < ids.length; i++) {
				ids[i] = cursor.getLong(0);
				cursor.moveToNext();
			}
			cursor.close();
			return ids;
		}
		return new long[0];
	}

	/**
	 * add track to the current playback queue after current playback position
	 *
	 * @param id The list to enqueue.
	 */
	public static void playNext(Activity activity, long id) {
		IApolloService service = getService(activity);
		if (service != null) {
			try {
				service.enqueue(new long[]{id}, MusicPlaybackService.MOVE_NEXT);
			} catch (RemoteException exception) {
				Log.e(TAG, "playNext()", exception);
			}
		}
	}

	/**
	 * Creates a sub menu used to add items to a new playlist or an existing one.
	 *
	 * @param groupId       The group Id of the menu.
	 * @param subMenu       The {@link SubMenu} to add to.
	 * @param showFavorites True if we should show the option to add to the Favorites cache.
	 */
	public static void makePlaylistMenu(Context context, int groupId, SubMenu subMenu, boolean showFavorites) {
		subMenu.clear();
		if (showFavorites)
			subMenu.add(groupId, ContextMenuItems.ADD_TO_FAVORITES, Menu.NONE, R.string.add_to_favorites);
		subMenu.add(groupId, ContextMenuItems.NEW_PLAYLIST, Menu.NONE, R.string.new_playlist);
		try {
			Cursor cursor = CursorFactory.makePlaylistCursor(context);
			if (cursor != null) {
				if (cursor.moveToFirst()) {
					do {
						long id = cursor.getLong(0);
						String name = cursor.getString(1);
						if (name != null) {
							Intent intent = new Intent();
							intent.putExtra(Constants.PLAYLIST_ID, id);
							subMenu.add(groupId, ContextMenuItems.PLAYLIST_SELECTED, Menu.NONE, name).setIntent(intent);
						}
					} while (cursor.moveToNext());
				}
				cursor.close();
			}
		} catch (RuntimeException exception) {
			Log.e(TAG, "makePlaylistMenu()", exception);
		}
	}

	/**
	 * Called when one of the lists should refresh or re-query.
	 */
	public static void refresh(Context context) {
		Intent broadcastIntent = new Intent(PlaybackBroadcastReceiver.ACTION_REFRESH);
		context.sendBroadcast(broadcastIntent);
	}

	/**
	 * Seeks the current track to a desired position
	 *
	 * @param position The position to seek to
	 */
	public static void seek(Activity activity, long position) {
		IApolloService service = getService(activity);
		if (service != null) {
			try {
				service.setPlayerPosition(position);
			} catch (RemoteException exception) {
				Log.e(TAG, "seek()", exception);
			}
		}
	}

	/**
	 * @return The current position time of the track
	 */
	public static long getPositionMillis(Activity activity) {
		IApolloService service = getService(activity);
		if (service != null) {
			try {
				return service.getPlayerPosition();
			} catch (RemoteException exception) {
				Log.e(TAG, "getPositionMillis()", exception);
			}
		}
		return 0;
	}

	/**
	 * @return The total duration of the current track
	 */
	public static long getDurationMillis(Activity activity) {
		IApolloService service = getService(activity);
		if (service != null) {
			try {
				return service.getPlayerDuration();
			} catch (RemoteException exception) {
				Log.e(TAG, "getDurationMillis()", exception);
			}
		}
		return 0;
	}

	/**
	 * Clears the queue
	 */
	public static void clearQueue(Activity activity) {
		IApolloService service = getService(activity);
		if (service != null) {
			try {
				service.clearQueue();
			} catch (RemoteException exception) {
				Log.e(TAG, "clearQueue()", exception);
			}
		}
	}

	/**
	 * enable/disable player crossfade
	 *
	 * @param enable true to enable crossfade
	 */
	public static void setCrossfade(Activity activity, boolean enable) {
		IApolloService service = getService(activity);
		if (service != null) {
			try {
				service.setCrossfade(enable);
			} catch (RemoteException exception) {
				Log.e(TAG, "setCrossfade()", exception);
			}
		}
	}

	/**
	 * open delete dialog for tracks
	 *
	 * @param activity activity
	 * @param title    title of the dialog
	 * @param songs    list of songs to remove
	 */
	public static void openDeleteDialog(FragmentActivity activity, String title, List<Song> songs) {
		openDeleteDialog(activity, title, getIDsFromSongList(songs));
	}

	/**
	 * open delete dialog for a single track
	 *
	 * @param activity activity
	 * @param title    title of the dialog
	 * @param id       list of a song ID to remove
	 */
	public static void openDeleteDialog(FragmentActivity activity, String title, long id) {
		openDeleteDialog(activity, title, new long[]{id});
	}

	/**
	 * open delete dialog for tracks
	 *
	 * @param activity activity
	 * @param title    title of the dialog
	 * @param ids      list of IDs to remove
	 */
	@SuppressWarnings("deprecation")
	public static void openDeleteDialog(FragmentActivity activity, String title, long[] ids) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
			// Use system Dialog to delete media files
			try {
				List<Uri> uris = new ArrayList<>(ids.length);
				for (long id : ids)
					uris.add(Media.getContentUri(MediaStore.VOLUME_EXTERNAL, id));
				PendingIntent requestRemove = MediaStore.createDeleteRequest(activity.getContentResolver(), uris);
				activity.startIntentSenderForResult(requestRemove.getIntentSender(), REQUEST_DELETE_FILES, null, 0, 0, 0);
			} catch (Exception exception) {
				// thrown when no audio file were found
				Log.e(TAG, "openDeleteDialog() failed to open dialog!");
			}
		} else {
			DeleteTracksDialog.show(activity.getSupportFragmentManager(), title, ids);
		}
	}

	/**
	 * create an array of track ids from a song list
	 */
	public static long[] getIDsFromSongList(List<Song> songs) {
		long[] ids = new long[songs.size()];
		for (int i = 0; i < ids.length; i++) {
			ids[i] = songs.get(i).getId();
		}
		return ids;
	}

	/**
	 * get service connected with a specific activity
	 */
	@Nullable
	private static IApolloService getService(@Nullable Activity activity) {
		if (activity != null) {
			ServiceBinder binder = mConnectionMap.get(activity);
			if (binder != null) {
				return binder.getService();
			}
		}
		return null;
	}
}