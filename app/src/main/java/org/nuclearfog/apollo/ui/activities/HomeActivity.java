package org.nuclearfog.apollo.ui.activities;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.ViewModelProvider;
import androidx.viewpager.widget.ViewPager;

import org.nuclearfog.apollo.R;
import org.nuclearfog.apollo.ui.fragments.phone.MusicBrowserPhoneFragment;
import org.nuclearfog.apollo.utils.ApolloUtils;
import org.nuclearfog.apollo.utils.FragmentViewModel;
import org.nuclearfog.apollo.utils.MusicUtils;
import org.nuclearfog.apollo.utils.ThemeUtils;

/**
 * This class is used to display the {@link ViewPager} used to swipe between the
 * main {@link Fragment}s used to browse the user's music.
 *
 * @author Andrew Neal (andrewdneal@gmail.com)
 */
public class HomeActivity extends ActivityBase {

	/**
	 *
	 */
	private FragmentViewModel viewModel;

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_base);
		Toolbar toolbar = findViewById(R.id.activity_base_toolbar);
		// init fragment callback
		viewModel = new ViewModelProvider(this).get(FragmentViewModel.class);
		// Initialize the theme resources
		ThemeUtils mResources = new ThemeUtils(this);
		setSupportActionBar(toolbar);
		if (getSupportActionBar() != null) {
			mResources.themeActionBar(getSupportActionBar(), R.string.app_name);
		}
		if (ApolloUtils.permissionsGranted(this)) {
			onPermissionGranted();
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void onPermissionGranted() {
		FragmentManager fm = getSupportFragmentManager();
		if (fm.findFragmentByTag(MusicBrowserPhoneFragment.TAG) == null) {
			fm.beginTransaction().replace(R.id.activity_base_content, MusicBrowserPhoneFragment.class, null, MusicBrowserPhoneFragment.TAG).commit();
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void onRefresh() {
		viewModel.notify(MusicBrowserPhoneFragment.REFRESH);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void onMetaChanged() {
		viewModel.notify(MusicBrowserPhoneFragment.META_CHANGED);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (requestCode == MusicUtils.REQUEST_DELETE_FILES && resultCode == RESULT_OK) {
			MusicUtils.refresh(this);
		}
	}
}