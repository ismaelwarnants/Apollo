package org.nuclearfog.apollo.ui.activities;

import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.FragmentManager;

import org.nuclearfog.apollo.R;
import org.nuclearfog.apollo.ui.fragments.PreferenceFragment;
import org.nuclearfog.apollo.utils.ApolloUtils;
import org.nuclearfog.apollo.utils.MusicUtils;
import org.nuclearfog.apollo.utils.ThemeUtils;

/**
 * Apollo settings activity
 *
 * @author Andrew Neal (andrewdneal@gmail.com)
 * @author nuclearfog
 */
public class SettingsActivity extends AppCompatActivity {

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.settings_layout);
		Toolbar toolbar = findViewById(R.id.settings_toolbar);
		View root = findViewById(R.id.settings_root);
		// setup toolbar
		setSupportActionBar(toolbar);
		ActionBar actionbar = getSupportActionBar();
		if (actionbar != null) {
			actionbar.setDisplayHomeAsUpEnabled(true);
			ThemeUtils mResources = new ThemeUtils(this);
			mResources.themeActionBar(actionbar, R.string.menu_settings);
			mResources.setBackground(root);
		}
		//attach fragment
		FragmentManager fm = getSupportFragmentManager();
		if (fm.findFragmentByTag(PreferenceFragment.TAG) == null) {
			fm.beginTransaction().replace(R.id.settings_frame, PreferenceFragment.class, null, PreferenceFragment.TAG).commit();
		}
		ApolloUtils.setWakelock(this);
		MusicUtils.bindToService(this, null);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean onOptionsItemSelected(@NonNull MenuItem item) {
		if (item.getItemId() == android.R.id.home) {
			finish();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
}