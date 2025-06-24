package org.nuclearfog.apollo.ui.dialogs;

import android.app.Dialog;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import org.nuclearfog.apollo.R;
import org.nuclearfog.apollo.cache.ImageCache;

/**
 * Dialog used to clear the image cache
 *
 * @author nuclearfog
 */
public class ClearCacheDialog extends DialogFragment {

	private static final String TAG = "ClearCacheDialog";

	/**
	 * show this dialog
	 */
	public static void show(FragmentManager fm) {
		ClearCacheDialog clearCacheDialog;
		Fragment dialog = fm.findFragmentByTag(TAG);

		if (dialog instanceof ClearCacheDialog) {
			clearCacheDialog = (ClearCacheDialog) dialog;
		} else {
			clearCacheDialog = new ClearCacheDialog();
		}
		clearCacheDialog.show(fm, TAG);
	}

	/**
	 * {@inheritDoc}
	 */
	@NonNull
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		return new AlertDialog.Builder(requireContext())
				.setMessage(R.string.delete_imagecache_warning)
				.setNegativeButton(R.string.cancel, null)
				.setPositiveButton(android.R.string.ok, (dialog, which) -> {
					ImageCache mImageCache = ImageCache.getInstance(requireContext());
					mImageCache.clearCaches();
				}).create();
	}
}