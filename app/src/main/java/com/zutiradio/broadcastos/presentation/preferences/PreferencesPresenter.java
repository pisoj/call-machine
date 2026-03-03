package com.zutiradio.broadcastos.presentation.preferences;

import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.support.annotation.Nullable;

import com.zutiradio.broadcastos.R;
import com.zutiradio.broadcastos.presentation.message.MessagePresenter;

public class PreferencesPresenter extends EdgeToEdgePreferenceFragment {

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        String key = preference.getKey();
        if (key == null) return super.onPreferenceTreeClick(preferenceScreen, preference);
        switch (key) {
            case "about":
                getFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragment_container_view, new MessagePresenter())
                        .addToBackStack(null)
                        .commit();
                break;
        }
        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }
}
