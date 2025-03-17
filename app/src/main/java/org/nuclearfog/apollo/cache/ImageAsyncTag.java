package org.nuclearfog.apollo.cache;

import android.widget.ImageView;

import androidx.annotation.NonNull;

import org.nuclearfog.apollo.async.AsyncExecutor.AsyncCallback;
import org.nuclearfog.apollo.async.worker.ImageWorker;
import org.nuclearfog.apollo.async.worker.ImageWorker.Param;
import org.nuclearfog.apollo.async.worker.ImageWorker.Result;
import org.nuclearfog.apollo.utils.Constants.ImageType;

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
	private ImageWorker imageWorker;

	private ImageView[] imageViews;

	/**
	 * key used to identify this tag
	 */
	private String mKey;


	public ImageAsyncTag(ImageFetcher imgWorker, @NonNull String mKey, ImageType imageType, ImageView... imageViews) {
		imageWorker = new ImageWorker(imgWorker, imageType);
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
		imageWorker.execute(param, this);
	}

	/**
	 *
	 */
	public void run(String mbid) {
		Param param = new Param(mKey, mbid);
		imageWorker.execute(param, this);
	}

	/**
	 * cancel worker task
	 */
	public void cancel() {
		imageWorker.cancel();
	}

	/**
	 * @return true if background process is finished
	 */
	public boolean isFinished() {
		return !imageWorker.isRunning();
	}

	/**
	 * @return unique tag key
	 */
	public String getTag() {
		return mKey;
	}
}