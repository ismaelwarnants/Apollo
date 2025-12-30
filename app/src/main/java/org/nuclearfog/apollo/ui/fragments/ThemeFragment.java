package org.nuclearfog.apollo.ui.fragments;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.GridView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.Fragment;

import org.nuclearfog.apollo.R;
import org.nuclearfog.apollo.model.AppTheme;
import org.nuclearfog.apollo.store.preferences.AppPreferences;
import org.nuclearfog.apollo.ui.adapters.listview.ThemesAdapter;
import org.nuclearfog.apollo.ui.adapters.listview.holder.RecycleHolder;
import org.nuclearfog.apollo.utils.ApolloUtils;

/**
 * Used to show all of the available themes on a user's device.
 *
 * @author Andrew Neal (andrewdneal@gmail.com)
 * @author nuclearfog
 */
public class ThemeFragment extends Fragment implements OnItemClickListener {

	public static final String TAG = "ThemeFragment";

	/**
	 * grid list adapter to show themes
	 */
	private ThemesAdapter mAdapter;
	private AppPreferences mPreferences;

	/**
	 * {@inheritDoc}
	 */
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		// init views
		View rootView = inflater.inflate(R.layout.grid_base, container, false);
		TextView emptyInfo = rootView.findViewById(R.id.grid_base_empty_info);
		GridView mGridView = rootView.findViewById(R.id.grid_base);
		// disable empty view holder
		emptyInfo.setVisibility(View.GONE);
		// init adapter
		mAdapter = new ThemesAdapter(requireContext());
		mPreferences = AppPreferences.getInstance(requireContext());
		// Release any reference to the recycled Views
		mGridView.setRecyclerListener(new RecycleHolder());
		mGridView.setOnItemClickListener(this);
		mGridView.setOnCreateContextMenuListener(this);
		mGridView.setAdapter(mAdapter);
		if (ApolloUtils.isLandscape(getContext())) {
			// And two for landscape
			mGridView.setNumColumns(2);
		} else {
			// Limit the columns to one in portrait mode
			mGridView.setNumColumns(1);
		}
		return rootView;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		// Default theme
		String defName = getString(R.string.app_name);
		Drawable defPrev = ResourcesCompat.getDrawable(getResources(), R.drawable.theme_preview, null);
		AppTheme defTheme = new AppTheme(defName, defPrev);
		mAdapter.add(defTheme);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		AppTheme selection = mAdapter.getItem(position);
		if (selection != null) {
			String name = getString(R.string.theme_set, selection.mName);
			mPreferences.setThemeSelectionIndex(position);
			ApolloUtils.showInfoToast(requireActivity(), name);
		}
	}
}