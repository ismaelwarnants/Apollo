package org.nuclearfog.apollo.ui.dialogs;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import org.nuclearfog.apollo.R;


public class ImageSelectorDialog extends DialogFragment {

	private static final String TAG = "ImageSelectorDialog";

	private static final String KEY_SEARCH = "search";


	public static void open(FragmentManager fm, String search) {
		ImageSelectorDialog dialog;
		Bundle bundle = new Bundle();
		bundle.putString(KEY_SEARCH, search);
		Fragment fragment = fm.findFragmentByTag(TAG);
		if (fragment instanceof ImageSelectorDialog) {
			dialog = (ImageSelectorDialog) fragment;
		} else {
			dialog = new ImageSelectorDialog();
		}
		dialog.setArguments(bundle);
		dialog.show(fm, TAG);
	}


	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		View view = inflater.inflate(R.layout.dialog_image_selector, container, false);

		return view;
	}
}