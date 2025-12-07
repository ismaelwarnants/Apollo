package org.nuclearfog.apollo.service;

import android.net.Uri;

import org.nuclearfog.apollo.IApolloService;
import org.nuclearfog.apollo.model.Album;
import org.nuclearfog.apollo.model.Song;

import java.lang.ref.WeakReference;

/**
 * callback used to communicate with activities
 *
 * @author nuclearfog
 */
class ServiceStub extends IApolloService.Stub {

	private final WeakReference<MusicPlaybackService> mService;

	/**
	 * @param service callback reference
	 */
	ServiceStub(MusicPlaybackService service) {
		mService = new WeakReference<>(service);
	}


	@Override
	public void openFile(Uri uri) {
		MusicPlaybackService service = mService.get();
		if (service != null && uri != null) {
			service.openFile(uri);
		}
	}


	@Override
	public void open(long[] list, int position) {
		MusicPlaybackService service = mService.get();
		if (mService.get() != null && list != null) {
			service.open(list, position);
		}
	}


	@Override
	public void pause(boolean force) {
		MusicPlaybackService service = mService.get();
		if (service != null) {
			service.pause(force);
		}
	}


	@Override
	public void stop() {
		MusicPlaybackService service = mService.get();
		if (service != null) {
			service.stop();
		}
	}


	@Override
	public void play() {
		MusicPlaybackService service = mService.get();
		if (service != null) {
			service.play();
		}
	}


	@Override
	public void gotoNext() {
		MusicPlaybackService service = mService.get();
		if (service != null) {
			service.gotoNext();
		}
	}


	@Override
	public void gotoPrev() {
		MusicPlaybackService service = mService.get();
		if (service != null) {
			service.gotoPrev();
		}
	}


	@Override
	public void enqueue(long[] list, int action) {
		MusicPlaybackService service = mService.get();
		if (service != null && list != null) {
			service.enqueue(list, action);
		}
	}


	@Override
	public void moveQueueItem(int from, int to) {
		MusicPlaybackService service = mService.get();
		if (service != null) {
			service.moveQueueItem(from, to);
		}
	}


	@Override
	public void stopForeground() {
		MusicPlaybackService service = mService.get();
		if (service != null) {
			service.stopForeground();
		}
	}


	@Override
	public void refresh() {
		MusicPlaybackService service = mService.get();
		if (service != null) {
			service.notifyChange(MusicPlaybackService.ACTION_REFRESH);
		}
	}


	@Override
	public boolean isPlaying() {
		MusicPlaybackService service = mService.get();
		if (service != null)
			return service.isPlaying();
		return false;
	}


	@Override
	public long[] getQueue() {
		MusicPlaybackService service = mService.get();
		if (service != null)
			return service.getQueue();
		return new long[0];
	}


	@Override
	public long getPlayerPosition() {
		MusicPlaybackService service = mService.get();
		if (service != null)
			return service.getPosition();
		return 0L;
	}


	@Override
	public long getPlayerDuration() {
		MusicPlaybackService service = mService.get();
		if (service != null)
			return service.getDuration();
		return 0L;
	}


	@Override
	public void setPlayerPosition(long position) {
		MusicPlaybackService service = mService.get();
		if (service != null) {
			service.seekTo(position);
		}
	}


	@Override
	public Album getCurrentAlbum() {
		MusicPlaybackService service = mService.get();
		if (service != null)
			return service.getCurrentAlbum();
		return null;
	}


	@Override
	public Song getCurrentTrack() {
		MusicPlaybackService service = mService.get();
		if (service != null)
			return service.getCurrentSong();
		return null;
	}


	@Override
	public int getQueuePosition() {
		MusicPlaybackService service = mService.get();
		if (service != null)
			return service.getQueuePosition();
		return -1;
	}


	@Override
	public void setQueuePosition(int index) {
		MusicPlaybackService service = mService.get();
		if (service != null) {
			service.setQueuePosition(index);
		}
	}


	@Override
	public int getShuffleMode() {
		MusicPlaybackService service = mService.get();
		if (service != null)
			return service.getShuffleMode();
		return MusicPlaybackService.SHUFFLE_NONE;
	}


	@Override
	public void setShuffleMode(int shuffleMode) {
		MusicPlaybackService service = mService.get();
		if (service != null) {
			service.setShuffleMode(shuffleMode);
		}
	}


	@Override
	public int getRepeatMode() {
		MusicPlaybackService service = mService.get();
		if (service != null)
			return service.getRepeatMode();
		return MusicPlaybackService.REPEAT_NONE;
	}


	@Override
	public void setRepeatMode(int repeatMode) {
		MusicPlaybackService service = mService.get();
		if (service != null) {
			service.setRepeatMode(repeatMode);
		}
	}


	@Override
	public void clearQueue() {
		MusicPlaybackService service = mService.get();
		if (service != null) {
			service.clearQueue();
		}
	}


	@Override
	public void removeTrack(int pos) {
		MusicPlaybackService service = mService.get();
		if (service != null) {
			service.removeQueueTrack(pos);
		}
	}


	@Override
	public int getAudioSessionId() {
		MusicPlaybackService service = mService.get();
		if (service != null)
			return service.getAudioSessionId();
		return 0;
	}


	@Override
	public void setCrossfade(boolean enable) {
		MusicPlaybackService service = mService.get();
		if (service != null) {
			service.setCrossfade(enable);
		}
	}


	@Override
	public void releaseService() {
		MusicPlaybackService service = mService.get();
		if (service != null) {
			service.releaseService(true);
		}
	}
}