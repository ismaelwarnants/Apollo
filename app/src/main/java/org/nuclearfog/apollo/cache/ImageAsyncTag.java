package org.nuclearfog.apollo.cache;

import android.widget.ImageView;

import androidx.annotation.NonNull;

import org.nuclearfog.apollo.async.AsyncExecutor.AsyncCallback;
import org.nuclearfog.apollo.async.worker.BitmapWorker;
import org.nuclearfog.apollo.async.worker.BitmapWorker.Param;
import org.nuclearfog.apollo.async.worker.BitmapWorker.Result;
import org.nuclearfog.apollo.cache.ImageFetcher.ImageType;

/**
 * A custom {@link android.view.View} Objet tag that will be attached to the
 * {@link ImageView} while the work is in progress. Contains a reference to
 * the actual worker task, so that it can be stopped if a new binding is
 * required, and makes sure that only the last started worker process can
 * bind its result, independently of the finish order.
 */
public class ImageAsyncTag implements AsyncCallback<Result> {

	/**
	 * background worker task
	 */
	private BitmapWorker bitmapWorker;

	private ImageView[] imageViews;

	/**
	 * key used to identify this tag
	 */
	private String mKey;


	public ImageAsyncTag(ImageFetcher imgWorker, @NonNull String mKey, ImageType imageType, ImageView... imageViews) {
		bitmapWorker = new BitmapWorker(imgWorker, imageType);
		this.imageViews = imageViews;
		this.mKey = mKey;
	}


	@Override
	public void onResult(@NonNull Result result) {
		if (imageViews != null) {
			imageViews[0].setImageDrawable(result.drawable);
			if (imageViews.length > 1) {
				imageViews[1].setImageDrawable(result.blurredDrawable);
			}
		}
	}

	/**
	 * execute background task
	 */
	public void run(String artistName, String albumName, long albumId) {
		Param param = new Param(mKey, albumName, artistName, albumId);
		bitmapWorker.execute(param, this);
	}

	/**
	 * cancel worker task
	 */
	public void cancel() {
		bitmapWorker.cancel();
	}

	/**
	 * @return true if background process is finished
	 */
	public boolean isFinished() {
		return !bitmapWorker.isRunning();
	}

	/**
	 * @return unique tag key
	 */
	public String getTag() {
		return mKey;
	}
}