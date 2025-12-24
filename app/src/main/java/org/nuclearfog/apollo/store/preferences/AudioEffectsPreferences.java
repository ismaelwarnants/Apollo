package org.nuclearfog.apollo.store.preferences;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;

/**
 * Preferences used for internal equalizer/audio effects
 *
 * @author nuclearfog
 */
@SuppressLint("ApplySharedPref")
public class AudioEffectsPreferences {

	/**
	 * Name of the preference file
	 */
	private static final String FX_PREF_NAME = "eq_settings";
	/**
	 * preference used to enable/disable integrated audio effectx
	 */
	private static final String FX_ENABLE = "fx_enable_effects";
	/**
	 * preference used to enable/disable bass boost
	 */
	private static final String FX_BASSBOOST = "fx_bassboost_enable";
	/**
	 * preference used to enable/disable reverb effect
	 */
	private static final String FX_REVERB = "fx_reverb_enable";
	/**
	 * preference used to save/load equalizer band levels
	 */
	private static final String FX_EQUALIZER_BANDS = "fx_equalizer_bands";
	/**
	 * preference used to set name of the selected preset
	 */
	private static final String FX_PRESET = "fx_preset_name";

	private static AudioEffectsPreferences mInstance;

	private SharedPreferences audioEffectsPref;

	/**
	 *
	 */
	private AudioEffectsPreferences(Context context) {
		audioEffectsPref = context.getSharedPreferences(FX_PREF_NAME, Context.MODE_PRIVATE);
	}

	/**
	 * get singleton instance of this class
	 *
	 * @param context Context to load app settings
	 * @return singleton instance
	 */
	public static AudioEffectsPreferences getInstance(Context context) {
		if (mInstance == null) {
			mInstance = new AudioEffectsPreferences(context.getApplicationContext());
		}
		return mInstance;
	}


	/**
	 * check if audiofx is enabled
	 *
	 * @return true if audiofx is enabled
	 */
	public boolean isAudioFxEnabled() {
		return audioEffectsPref.getBoolean(FX_ENABLE, false);
	}

	/**
	 * enable/disable audiofx
	 *
	 * @param enable true to enable audiofx
	 */
	public void setAudioFxEnabled(boolean enable) {
		SharedPreferences.Editor editor = audioEffectsPref.edit();
		editor.putBoolean(FX_ENABLE, enable);
		editor.commit();
	}

	/**
	 * get equalizer band setup
	 *
	 * @return array of band levels starting with the lowest frequency
	 */
	public int[] getEqualizerBands() {
		String serializedBands = audioEffectsPref.getString(FX_EQUALIZER_BANDS, "");
		if (serializedBands.isEmpty())
			return new int[0];

		String[] bands = serializedBands.split(";");
		int[] result = new int[bands.length];
		for (int i = 0; i < result.length; i++) {
			result[i] = Integer.parseInt(bands[i]);
		}
		return result;
	}


	/**
	 * save new equalizer band setup
	 *
	 * @param bands array of band levels starting with the lowest frequency
	 */
	public void setEqualizerBands(int[] bands) {
		StringBuilder result = new StringBuilder();
		for (int band : bands)
			result.append(band).append(';');
		if (result.length() > 0)
			result.deleteCharAt(result.length() - 1);

		SharedPreferences.Editor editor = audioEffectsPref.edit();
		editor.putString(FX_EQUALIZER_BANDS, result.toString());
		editor.commit();
	}

	/**
	 * get bass boost level
	 *
	 * @return bass level from 0 to 1000
	 */
	public int getBassLevel() {
		return audioEffectsPref.getInt(FX_BASSBOOST, 0);
	}

	/**
	 * set bass boost level
	 *
	 * @param level bass level from 0 to 1000
	 */
	public void setBassLevel(int level) {
		SharedPreferences.Editor editor = audioEffectsPref.edit();
		editor.putInt(FX_BASSBOOST, level);
		editor.commit();
	}


	/**
	 * get reverb level
	 *
	 * @return reverb level (room size)
	 */
	public int getReverbLevel() {
		return audioEffectsPref.getInt(FX_REVERB, 0);
	}

	/**
	 * set reverb level
	 *
	 * @param level reverb level (room size)
	 */
	public void setReverbLevel(int level) {
		SharedPreferences.Editor editor = audioEffectsPref.edit();
		editor.putInt(FX_REVERB, level);
		editor.commit();
	}

	/**
	 * get name of the current selected preset
	 *
	 * @return preset name
	 */
	public String getPresetName() {
		return audioEffectsPref.getString(FX_PRESET, "default");
	}

	/**
	 * set name of the current selected preset
	 *
	 * @param name preset name
	 */
	public void setPresetName(String name) {
		SharedPreferences.Editor editor = audioEffectsPref.edit();
		editor.putString(FX_PRESET, name);
		editor.commit();
	}
}
