package org.nuclearfog.apollo.ui.dialogs;

import android.content.res.Resources;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

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
import org.nuclearfog.apollo.utils.ThemeUtils;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
 * This dialog shows an online image browser to select an artist/album artwork
 *
 * @author nuclearfog
 */
public class ImageSelectorDialog extends DialogFragment implements AsyncCallback<List<AlbumMB>>, OnItemClickListener, TextWatcher {

	private static final String TAG = "ImageSelectorDialog";

	private static final String KEY_SEARCH_ALBUM = "search-album";
	private static final String KEY_SEARCH_ARTIST = "search-artist";
	private static final String KEY_SEARCH = "search";

	private EditText search;
	private ProgressBar loading;

	private AlbumArtLoader loader;
	private AlbumArtAdapter adapter;
	private Timer timer = new Timer();

	/**
	 * show this dialog
	 *
	 * @param album  additional album name to search for
	 * @param artist artist name to search for
	 */
	public static void open(FragmentManager fm, String album, String artist) {
		ImageSelectorDialog dialog;
		Bundle bundle = new Bundle();
		bundle.putString(KEY_SEARCH_ALBUM, album);
		bundle.putString(KEY_SEARCH_ARTIST, artist);
		Fragment fragment = fm.findFragmentByTag(TAG);
		if (fragment instanceof ImageSelectorDialog) {
			dialog = (ImageSelectorDialog) fragment;
		} else {
			dialog = new ImageSelectorDialog();
		}
		dialog.setArguments(bundle);
		dialog.show(fm, TAG);
	}

	/**
	 * @inheritDoc
	 */
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.dialog_image_selector, container, false);
		ListView listView = view.findViewById(R.id.dialog_image_selector_list);
		TextView emptyView = view.findViewById(R.id.dialog_image_Selector_empty);
		loading = view.findViewById(R.id.dialog_image_selector_loading);
		search = view.findViewById(R.id.dialog_image_selector_search);
		adapter = new AlbumArtAdapter(requireContext());
		loader = new AlbumArtLoader(requireContext());
		ThemeUtils theme = new ThemeUtils(requireContext());
		String searchStr = null;
		String album = null;
		String artist = null;

		if (savedInstanceState != null) {
			searchStr = savedInstanceState.getString(KEY_SEARCH);
		} else if (getArguments() != null) {
			album = getArguments().getString(KEY_SEARCH_ALBUM);
			artist = getArguments().getString(KEY_SEARCH_ARTIST);
		}
		if (searchStr != null) {
			search.setText(searchStr);
			loader.execute(new String[]{searchStr}, this);
		} else if (album != null && !album.isEmpty()) {
			if (artist != null && !artist.isEmpty())
				search.setText(album + ", " + artist);
			else
				search.setText(album);
			loader.execute(new String[]{album, artist}, this);
		} else if (artist != null) {
			search.setText(artist);
			loader.execute(new String[]{"", artist}, this);
		}
		theme.themeProgressbar(loading);
		listView.setEmptyView(emptyView);
		listView.setAdapter(adapter);
		search.addTextChangedListener(this);
		listView.setOnItemClickListener(this);
		return view;
	}

	/**
	 * @inheritDoc
	 */
	@Override
	public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
		// lock layout width
		view.getLayoutParams().width = Math.round(Resources.getSystem().getDisplayMetrics().widthPixels * 0.8f);
	}

	/**
	 * @inheritDoc
	 */
	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		outState.putString(KEY_SEARCH, search.getText().toString());
		super.onSaveInstanceState(outState);
	}

	/**
	 * @inheritDoc
	 */
	@Override
	public void onDestroyView() {
		loader.cancel();
		adapter.flush();
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
		loading.setVisibility(View.INVISIBLE);
		adapter.clear();
		for (AlbumMB album : albums) {
			adapter.add(album);
		}
	}


	@Override
	public void beforeTextChanged(CharSequence s, int start, int count, int after) {
	}


	@Override
	public void onTextChanged(CharSequence s, int start, int before, int count) {
	}


	@Override
	public void afterTextChanged(Editable s) {
		// delay any list updates while typing
		loading.setVisibility(View.VISIBLE);
		timer.cancel();
		timer = new Timer();
		timer.schedule(new TimerTask() {
			@Override
			public void run() {
				loader.execute(new String[]{s.toString()}, ImageSelectorDialog.this);
			}
		}, 1000);
	}

	/**
	 * callback used to return selected album's MBID to activity
	 */
	public interface OnItemSelectedListener {

		/**
		 * called if an album is selected
		 *
		 * @param mbid album ID
		 */
		void onItemSelected(String mbid);
	}
}