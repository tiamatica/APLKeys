package com.athoraya.aplpad;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;

import com.athoraya.aplkeys.R;

/**
 * Created by gil on 27/08/2014.
 */
public class APLPadSettings extends PreferenceActivity {

    public static final String KEY_PREF_FONT_NAME = "pref_font_name";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        PreferenceManager.setDefaultValues(this, R.xml.aplpad_preferences, false);

        getFragmentManager().beginTransaction()
                .replace(android.R.id.content, new APLPadSettingsFragment())
                .commit();
    }

    public static class APLPadSettingsFragment extends PreferenceFragment {

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            addPreferencesFromResource(R.xml.aplpad_preferences);
            setFontNameSummary();
        }

        private final SharedPreferences.OnSharedPreferenceChangeListener mPrefsListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
            @Override
            public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
                if (key.equals(KEY_PREF_FONT_NAME)) {
                    setFontNameSummary();
                }
            }
        };

        private void setFontNameSummary(){
            ListPreference fontPref = (ListPreference) findPreference(KEY_PREF_FONT_NAME);
            // Set summary to be the user-description for the selected value
            fontPref.setSummary(fontPref.getEntry());

        }

        @Override
        public void onResume() {
            super.onResume();
            getPreferenceScreen().getSharedPreferences()
                    .registerOnSharedPreferenceChangeListener(mPrefsListener);
        }

        @Override
        public void onPause() {
            super.onPause();
            getPreferenceScreen().getSharedPreferences()
                    .unregisterOnSharedPreferenceChangeListener(mPrefsListener);
        }
    }
}
