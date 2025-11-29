package org.nuclearfog.apollo.ui.activities;

import android.view.MenuItem;

import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.FragmentManager;

import org.nuclearfog.apollo.R;
import org.nuclearfog.apollo.ui.fragments.ThemeFragment;
import org.nuclearfog.apollo.utils.ThemeUtils;

/**
 * A class the displays the {@link ThemeFragment}.
 *
 * @author Andrew Neal (andrewdneal@gmail.com)
 */
public class ThemesActivity extends ActivityBase {

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected int getContentView() {
		return R.layout.activity_base;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void initialize() {
		Toolbar toolbar = findViewById(R.id.activity_base_toolbar);
		// Initialize the theme resources
		ThemeUtils mResources = new ThemeUtils(this);
		setSupportActionBar(toolbar);
		if (getSupportActionBar() != null) {
			mResources.themeActionBar(getSupportActionBar(), R.string.settings_theme_chooser_title);
			getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		}
		// Transact the theme fragment
		FragmentManager fm = getSupportFragmentManager();
		if (fm.findFragmentByTag(ThemeFragment.TAG) == null) {
			fm.beginTransaction().replace(R.id.activity_base_content, ThemeFragment.class, null, ThemeFragment.TAG).commit();
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == android.R.id.home) {
			finish();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void onMetaChanged() {
		// not used
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void onRefresh() {
		// not used
	}
}