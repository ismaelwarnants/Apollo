package org.nuclearfog.apollo.ui.dialogs;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.EditText;
import android.widget.ListView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import org.nuclearfog.apollo.R;
import org.nuclearfog.apollo.async.AsyncExecutor.AsyncCallback;
import org.nuclearfog.apollo.async.loader.AlbumArtLoader;
import org.nuclearfog.apollo.lookup.entities.AlbumMB;
import org.nuclearfog.apollo.ui.adapters.listview.AlbumArtAdapter;

import java.util.List;

/**
 * @author nuclearfog
 */
public class ImageSelectorDialog extends DialogFragment implements AsyncCallback<List<AlbumMB>>, OnItemClickListener, OnScrollListener {

	private static final String TAG = "ImageSelectorDialog";

	private static final String KEY_SEARCH = "search";

	private EditText search;

	private AlbumArtLoader loader;
	private AlbumArtAdapter adapter;


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
		ListView listView = view.findViewById(R.id.dialog_image_selector_list);
		search = view.findViewById(R.id.dialog_image_selector_search);
		adapter = new AlbumArtAdapter(requireContext());
		loader = new AlbumArtLoader(requireContext());

		listView.setAdapter(adapter);
		if (savedInstanceState == null) {
			savedInstanceState = getArguments();
		}
		if (savedInstanceState != null) {
			String searchStr = savedInstanceState.getString(KEY_SEARCH);
			search.setText(searchStr);
			loader.execute(searchStr, this);
		}

		listView.setOnScrollListener(this);
		listView.setOnItemClickListener(this);
		return view;
	}


	@Override
	public void onDestroyView() {
		loader.cancel();
		super.onDestroyView();
	}


	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		AlbumMB album = adapter.getItem(position);
		if (getActivity() instanceof OnItemSelectedListener && album != null) {
			((OnItemSelectedListener) getActivity()).onItemSelected(album.getId());
		}
		dismiss();
	}


	@Override
	public void onResult(@NonNull List<AlbumMB> albums) {
		adapter.clear();
		for (AlbumMB album: albums) {
			adapter.add(album);
		}
		adapter.notifyDataSetChanged();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onScrollStateChanged(AbsListView view, int scrollState) {
		// Pause disk cache access to ensure smoother scrolling
		if (scrollState == AbsListView.OnScrollListener.SCROLL_STATE_FLING
				|| scrollState == AbsListView.OnScrollListener.SCROLL_STATE_TOUCH_SCROLL) {
			adapter.setPauseDiskCache(true);
		} else {
			adapter.setPauseDiskCache(false);
			adapter.notifyDataSetChanged();
		}
	}


	@Override
	public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
	}


	public interface OnItemSelectedListener {

		void onItemSelected(String mbid);
	}
}