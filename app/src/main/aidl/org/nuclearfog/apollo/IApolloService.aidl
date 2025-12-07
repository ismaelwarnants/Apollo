package org.nuclearfog.apollo;

import org.nuclearfog.apollo.model.Song;
import org.nuclearfog.apollo.model.Album;

interface IApolloService {
	void stopForeground();
	int getAudioSessionId();
	void openFile(in Uri uri);
	void open(in long[] list, int position);
	void pause(boolean force);
	void play();
	void stop();
	void gotoNext();
	void gotoPrev();
	void enqueue(in long[] list, int action);
	void moveQueueItem(int from, int to);
	long[] getQueue();
	int getQueuePosition();
	void setQueuePosition(int index);
	void clearQueue();
	int getShuffleMode();
	int getRepeatMode();
	void setShuffleMode(int shufflemode);
	void setRepeatMode(int repeatmode);
	void refresh();
	boolean isPlaying();
	long getPlayerDuration();
	long getPlayerPosition();
	void setPlayerPosition(long pos);
	void removeTrack(int pos);
	void setCrossfade(boolean enable);
	void releaseService();
	Song getCurrentTrack();
	Album getCurrentAlbum();
}