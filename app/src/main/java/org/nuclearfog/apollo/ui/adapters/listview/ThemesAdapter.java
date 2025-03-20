package org.nuclearfog.apollo.ui.adapters.listview;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;

import org.nuclearfog.apollo.R;
import org.nuclearfog.apollo.model.AppTheme;
import org.nuclearfog.apollo.ui.adapters.listview.holder.MusicHolder;

/**
 * Populates the {@link android.widget.GridView} with the available themes
 */
public class ThemesAdapter extends ArrayAdapter<AppTheme> {

	/**
	 * Item layout resource
	 */
	private static final int ITEM_LAYOUT = R.layout.fragment_themes_base;

	/**
	 * Constructor of <code>ThemesAdapter</code>
	 *
	 * @param context The {@link Context} to use.
	 */
	public ThemesAdapter(Context context) {
		super(context, ITEM_LAYOUT);
	}

	/**
	 * {@inheritDoc}
	 */
	@NonNull
	@Override
	public View getView(int position, View convertView, @NonNull ViewGroup parent) {
		MusicHolder holder;
		if (convertView == null) {
			convertView = LayoutInflater.from(parent.getContext()).inflate(ITEM_LAYOUT, parent, false);
			holder = new MusicHolder(convertView);
			convertView.setTag(holder);
		} else {
			holder = (MusicHolder) convertView.getTag();
		}
		// Retrieve the data holder
		AppTheme themeHolder = getItem(position);
		if (themeHolder != null) {
			// Set the theme preview
			holder.mImage.setImageDrawable(themeHolder.mPreview);
			// Set the theme name
			holder.mLineOne.setText(themeHolder.mName);
		}
		return convertView;
	}
}