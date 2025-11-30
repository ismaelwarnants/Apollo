package org.nuclearfog.apollo.ui.fragments;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.CheckBoxPreference;
import androidx.preference.Preference;
import androidx.preference.Preference.OnPreferenceClickListener;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;

import org.nuclearfog.apollo.BuildConfig;
import org.nuclearfog.apollo.R;
import org.nuclearfog.apollo.ui.activities.ThemesActivity;
import org.nuclearfog.apollo.ui.dialogs.ClearCacheDialog;
import org.nuclearfog.apollo.ui.dialogs.ColorSchemeDialog;
import org.nuclearfog.apollo.ui.dialogs.LicenseDialog;
import org.nuclearfog.apollo.utils.Constants;
import org.nuclearfog.apollo.utils.NavUtils;

/**
 * Preference fragment class
 *
 * @author nuclearfog
 * @see org.nuclearfog.apollo.ui.activities.SettingsActivity
 */
public class PreferenceFragment extends PreferenceFragmentCompat implements OnPreferenceClickListener {

	public static final String TAG = "PreferenceFragment";

	private static final String DEL_CACHE = "delete_cache";
	private static final String THEME_SEL = "theme_chooser";
	private static final String COLOR_SEL = "color_scheme";
	private static final String VERSION = "version";
	private static final String BAT_OPT = "disable_battery_opt";
	private static final String DOWNLOAD_IMAGES = "download_missing_artist_images";
	private static final String DOWNLOAD_ARTWORK = "download_missing_artwork";
	private static final String SOURCECODE = "source_code";
	private static final String LICENSE = "licenses";
	private static final String CERT_DISABLE = "disable_ssl_verification";


	@Override
	public void onCreatePreferences(@Nullable Bundle savedInstanceState, @Nullable String rootKey) {
		setPreferencesFromResource(R.xml.settings, rootKey);
		PreferenceManager.setDefaultValues(requireContext(), R.xml.settings, true);
		Preference mOpenSourceLicenses = findPreference(LICENSE);
		Preference deleteCache = findPreference(DEL_CACHE);
		Preference themeChooser = findPreference(THEME_SEL);
		Preference colorScheme = findPreference(COLOR_SEL);
		Preference batteryOpt = findPreference(BAT_OPT);
		Preference sourceCode = findPreference(SOURCECODE);
		Preference disableCert = findPreference(CERT_DISABLE);
		Preference version = findPreference(VERSION);
		CheckBoxPreference downloadImages = findPreference(DOWNLOAD_IMAGES);
		CheckBoxPreference downloadArtwork = findPreference(DOWNLOAD_ARTWORK);

		if (version != null)
			version.setSummary(BuildConfig.VERSION_NAME);
		if (mOpenSourceLicenses != null)
			mOpenSourceLicenses.setOnPreferenceClickListener(this);
		if (deleteCache != null)
			deleteCache.setOnPreferenceClickListener(this);
		if (themeChooser != null)
			themeChooser.setOnPreferenceClickListener(this);
		if (colorScheme != null)
			colorScheme.setOnPreferenceClickListener(this);
		if (sourceCode != null)
			sourceCode.setOnPreferenceClickListener(this);
		if (downloadImages != null && downloadArtwork != null) {
			downloadImages.setOnPreferenceClickListener(this);
			downloadArtwork.setOnPreferenceClickListener(this);
		}
		if (disableCert != null) {
			if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
				disableCert.setVisible(true);
			}
		}
		if (batteryOpt != null) {
			if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
				batteryOpt.setVisible(false);
			} else {
				batteryOpt.setOnPreferenceClickListener(this);
			}
		}
	}


	@Override
	public boolean onPreferenceClick(@NonNull Preference preference) {
		switch (preference.getKey()) {
			case LICENSE:
				LicenseDialog.show(getParentFragmentManager());
				return true;

			case DEL_CACHE:
				ClearCacheDialog.show(getParentFragmentManager());
				return true;

			case COLOR_SEL:
				ColorSchemeDialog.show(getParentFragmentManager());
				return true;

			case THEME_SEL:
				startActivity(new Intent(requireContext(), ThemesActivity.class));
				return true;

			case BAT_OPT:
				NavUtils.openBatteryPage(requireActivity());
				return true;

			case SOURCECODE:
				// open source code repository
				String url = Constants.SOURCE_URL;
				Intent intent = new Intent(Intent.ACTION_VIEW);
				intent.setData(Uri.parse(url));
				try {
					startActivity(intent);
				} catch (ActivityNotFoundException e) {
					Log.w(TAG, "could not redirect to source code repository!");
				}
				break;
		}
		return false;
	}
}