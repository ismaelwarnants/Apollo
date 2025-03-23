package org.nuclearfog.apollo.cache;

import android.content.Context;
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
	 * unique key used to identify this tag
	 */
	private String mKey;

	/**
	 * @param mKey       cache key used to identify thumbnail image
	 * @param imageViews imageview(s) to show the downloaded images
	 */
	public ImageAsyncTag(Context context, String mKey, ImageView... imageViews) {
		imageWorker = new ImageWorker(context);
		this.imageViews = imageViews;
		this.mKey = mKey;
	}


	@Override
	public void onResult(@NonNull Result result) {
		imageViews[0].setImageDrawable(result.drawable);
		if (imageViews.length > 1) {
			imageViews[1].setImageDrawable(result.blurredDrawable);
		}
	}

	/**
	 * execute the background task
	 *
	 * @param id   local MediaStore ID of an album/artist entry to load the local image
	 * @param type type of the image to download
	 */
	public void run(ImageType type, long id) {
		Param param = new Param(type, mKey, id);
		imageWorker.execute(param, this);
	}

	/**
	 * execute the background task
	 *
	 * @param mbid online MusicBrainz ID to download the image
	 * @param type type of the image to download
	 */
	public void run(ImageType type, String mbid) {
		Param param = new Param(type, mKey, mbid);
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