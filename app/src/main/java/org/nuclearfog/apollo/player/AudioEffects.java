package org.nuclearfog.apollo.player;

import android.content.Context;
import android.media.audiofx.BassBoost;
import android.media.audiofx.EnvironmentalReverb;
import android.media.audiofx.Equalizer;
import android.util.Log;

import androidx.annotation.Nullable;

import org.nuclearfog.apollo.model.AudioPreset;
import org.nuclearfog.apollo.store.preferences.AudioEffectsPreferences;

/**
 * Audio effect class providing methods to manage effects at realtime
 *
 * @author nuclearfog
 */
public final class AudioEffects {

	private static final String TAG = "AudioEffects";

	/**
	 * max limit of the bass boost effect defined in {@link BassBoost}
	 */
	public static final int MAX_BASS_BOOST = 1000;

	/**
	 * max reverb steps defined in {@link EnvironmentalReverb}
	 */
	public static final int MAX_REVERB = 11;

	/**
	 * priority used by audiofx (default 0, high > 0, low < 0)
	 */
	private static final int FX_PRIORITY = 0;

	/**
	 * singleton instance
	 * regenerated if session ID changes
	 */
	private static AudioEffects instance;

	private Equalizer equalizer;
	private BassBoost bassBooster;
	private EnvironmentalReverb reverb;
	private AudioEffectsPreferences prefs;

	private int sessionId;

	/**
	 * get singleton instance
	 *
	 * @param context   context to get equalizer settings
	 * @param sessionId current audio session ID
	 * @return {@link AudioEffects} instance or null if audio effects isn't supported
	 */
	@Nullable
	public static AudioEffects getInstance(Context context, int sessionId) {
		try {
			if (sessionId != 0) {
				if (instance == null || instance.sessionId != sessionId) {
					instance = new AudioEffects(context, sessionId);
					Log.d(TAG, "audio_session_id=" + sessionId);
				}
			} else {
				Log.e(TAG, "init audio effects failed, audio session id is '0'!");
			}
		} catch (Exception e) {
			// thrown if there is no support for audio effects
			Log.e(TAG, "audio effects not supported!", e);
		}
		return instance;
	}

	/**
	 * release all audio effects from usage
	 */
	public static void release() {
		if (instance != null) {
			try {
				instance.equalizer.release();
				instance.bassBooster.release();
				instance.reverb.release();
			} catch (RuntimeException exception) {
				Log.e(TAG, "release()", exception);
			}
		}
	}

	/**
	 * @param sessionId current audio session ID
	 */
	private AudioEffects(Context context, int sessionId) {
		equalizer = new Equalizer(FX_PRIORITY, sessionId);
		bassBooster = new BassBoost(FX_PRIORITY, sessionId);
		reverb = new EnvironmentalReverb(FX_PRIORITY, sessionId);
		prefs = AudioEffectsPreferences.getInstance(context);
		this.sessionId = sessionId;
		boolean active = prefs.isAudioFxEnabled();

		equalizer.setEnabled(active);
		bassBooster.setEnabled(active);
		reverb.setEnabled(active);
		if (active) {
			setEffectValues();
		}
	}

	/**
	 * @return true if audio FX is enabled
	 */
	public boolean isAudioFxEnabled() {
		return prefs.isAudioFxEnabled();
	}

	/**
	 * enable/disable audio effects
	 *
	 * @param enable true to enable all audio effects
	 */
	public void enableAudioFx(boolean enable) {
		try {
			equalizer.setEnabled(enable);
			bassBooster.setEnabled(enable);
			reverb.setEnabled(enable);
			prefs.setAudioFxEnabled(enable);
			if (enable) {
				setEffectValues();
			}
		} catch (RuntimeException exception) {
			Log.e(TAG, "enableAudioFx()", exception);
		}
	}

	/**
	 * get min, max limits of the eq band
	 *
	 * @return array with min and max limits
	 */
	public int[] getBandLevelRange() {
		try {
			short[] ranges = equalizer.getBandLevelRange();
			return new int[]{ranges[0], ranges[1]};
		} catch (RuntimeException exception) {
			Log.e(TAG, "getBandLevelRange()", exception);
		}
		return new int[2];
	}

	/**
	 * get band frequencies
	 *
	 * @return array of band frequencies, starting with the lowest frequency
	 */
	public int[] getBandFrequencies() {
		try {
			short bandCount = equalizer.getNumberOfBands();
			int[] freq = new int[bandCount];
			for (short i = 0; i < bandCount; i++) {
				freq[i] = equalizer.getCenterFreq(i) / 1000;
			}
			return freq;
		} catch (RuntimeException exception) {
			Log.e(TAG, "getBandFrequencies()", exception);
		}
		return new int[0];
	}

	/**
	 * get equalizer bands
	 *
	 * @return array of band levels and frequencies starting from the lowest equalizer frequency
	 */
	public int[] getBandLevel() {
		try {
			short bandCount = equalizer.getNumberOfBands();
			int[] level = new int[bandCount];
			for (short i = 0; i < bandCount; i++) {
				level[i] = equalizer.getBandLevel(i);
			}
			return level;
		} catch (RuntimeException exception) {
			Log.e(TAG, "getBandLevel()", exception);
		}
		return new int[0];
	}

	/**
	 * set a new equalizer band value
	 *
	 * @param band  index of the equalizer band
	 * @param level level of the band
	 */
	public void setBandLevel(int band, int level) {
		try {
			// set single band level
			equalizer.setBandLevel((short) band, (short) level);
			// save all equalizer band levels
			short bandCount = equalizer.getNumberOfBands();
			int[] bands = new int[bandCount];
			for (short i = 0; i < bandCount; i++) {
				bands[i] = equalizer.getBandLevel(i);
			}
			prefs.setEqualizerBands(bands);
		} catch (RuntimeException exception) {
			Log.e(TAG, "setBandLevel()", exception);
		}
	}

	/**
	 * set all band levels
	 *
	 * @param bands equalizer bands
	 */
	public void setBandLevel(int[] bands) {
		try {
			for (short i = 0; i < bands.length; i++) {
				equalizer.setBandLevel(i, (short) bands[i]);
			}
			prefs.setEqualizerBands(bands);
		} catch (RuntimeException exception) {
			Log.e(TAG, "setBandLevel()", exception);
		}
	}

	/**
	 * return bass boost strength
	 *
	 * @return bass boost strength value from 0 to 1000
	 */
	public int getBassLevel() {
		try {
			return bassBooster.getRoundedStrength();
		} catch (RuntimeException exception) {
			Log.e(TAG, "getBassLevel()", exception);
		}
		return 0;
	}

	/**
	 * set bass boost level
	 *
	 * @param level bass boost strength value from 0 to 1000
	 */
	public void setBassLevel(int level) {
		try {
			bassBooster.setStrength((short) level);
			prefs.setBassLevel(level);
		} catch (RuntimeException exception) {
			Log.e(TAG, "setBassLevel()", exception);
		}
	}

	/**
	 * get reverb level
	 * todo implement more reverb settings
	 *
	 * @return reverb level
	 */
	public int getReverbLevel() {
		try {
			int t = (reverb.getReverbLevel() + 9000) / 1000;
			return (reverb.getReverbLevel() + 9000) / 1000;
		} catch (RuntimeException exception) {
			Log.e(TAG, "getReverbLevel()", exception);
		}
		return 0;
	}

	/**
	 * set reverb level
	 *
	 * @param level reverb level
	 */
	public void setReverbLevel(int level) {
		try {
			reverb.setReverbLevel((short) (level * 1000 - 9000));
			prefs.setReverbLevel(level);
		} catch (RuntimeException exception) {
			Log.e(TAG, "setReverbLevel()", exception);
		}
	}

	/**
	 * get current preset
	 *
	 * @return current preset
	 */
	public AudioPreset getPreset() {
		return new AudioPreset(prefs.getPresetName(), getBandLevel(), getBassLevel(), getReverbLevel());
	}

	/**
	 * set new preset
	 *
	 * @param preset preset to set or null to set custom preset
	 */
	public void setPreset(@Nullable AudioPreset preset) {
		if (preset != null) {
			setBassLevel(preset.getBassLevel());
			setReverbLevel(preset.getReverbLevel());
			setBandLevel(preset.getBands());
			prefs.setPresetName(preset.getName());
		} else {
			prefs.setPresetName("");
		}
	}

	/**
	 * set saved values for audio effects
	 */
	private void setEffectValues() {
		try {
			// setup audio effects
			bassBooster.setStrength((short) prefs.getBassLevel());
			reverb.setReverbLevel((short) prefs.getReverbLevel());
			int[] bandLevel = prefs.getEqualizerBands();
			for (short i = 0; i < bandLevel.length; i++) {
				equalizer.setBandLevel(i, (short) bandLevel[i]);
			}
		} catch (RuntimeException exception) {
			Log.e(TAG, "setEffectValues()", exception);
		}
	}
}