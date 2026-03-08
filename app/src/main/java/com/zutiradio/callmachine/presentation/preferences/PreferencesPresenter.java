package com.zutiradio.callmachine.presentation.preferences;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.MultiSelectListPreference;
import android.preference.Preference;
import android.preference.PreferenceGroup;
import android.preference.PreferenceScreen;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.Toast;

import com.zutiradio.callmachine.R;
import com.zutiradio.callmachine.wetradio.WetRadioUploader;
import com.zutiradio.callmachine.files.FileHelpers;
import com.zutiradio.callmachine.files.FileProvider;
import com.zutiradio.callmachine.presentation.about.AboutPresenter;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Arrays;
import java.util.Set;

public class PreferencesPresenter extends EdgeToEdgePreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener {

    private static final int GREETING_FILE_REQUEST_CODE = 1;
    private static final int TIMEOUT_FILE_REQUEST_CODE = 2;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);
        getPreferenceManager().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
        initPreferenceValues();
        adjustSupportedSampleRates();
        initPreferenceSummaries(getPreferenceScreen());
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        boolean result = super.onPreferenceTreeClick(preferenceScreen, preference);
        String key = preference.getKey();
        if (key == null) return result;

        if (key.equals("greeting_audio_file") || key.equals("timeout_audio_file")) {
            Intent fileIntent = new Intent(Intent.ACTION_GET_CONTENT);
            fileIntent.addCategory(Intent.CATEGORY_OPENABLE);
            fileIntent.setType("audio/*");

            Intent recorderIntent = new Intent(MediaStore.Audio.Media.RECORD_SOUND_ACTION);

            Intent chooser = new Intent(Intent.ACTION_CHOOSER);
            chooser.putExtra(Intent.EXTRA_TITLE, "Provide an audio file");
            chooser.putExtra(Intent.EXTRA_INTENT, fileIntent);
            chooser.putExtra(Intent.EXTRA_INITIAL_INTENTS, new Intent[]{recorderIntent});
            startActivityForResult(chooser, key.equals("greeting_audio_file") ? GREETING_FILE_REQUEST_CODE : TIMEOUT_FILE_REQUEST_CODE);
        } else if (key.equals("about")) {
            getFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container_view, new AboutPresenter())
                    .addToBackStack(null)
                    .commit();
        } else if (key.equals("wet_radio_open_last_server_response")) {
            File file = WetRadioUploader.getLastResponseFile(getContext());
            if (file.exists()) {
                FileProvider.openHtmlFromPrivateCache(getContext(), WetRadioUploader.LAST_RESPONSE_FILE_NAME);
            }
        }
        return result;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != Activity.RESULT_OK) return;

        if (requestCode == GREETING_FILE_REQUEST_CODE || requestCode == TIMEOUT_FILE_REQUEST_CODE) {
            Uri uri = data.getData();
            String fileName = getFileNameFromContentProvider(uri);
            if (fileName == null) {
                fileName = uri.getPath();
            }
            FileHelpers.copyUriToFileAsync(getContext(),
                    uri,
                    new File(getContext().getFilesDir(), requestCode == GREETING_FILE_REQUEST_CODE ? "greeting" : "timeout"),
                    new FileHelpers.CopyFilesAsyncCallback() {
                        @Override
                        public void onSuccess() {
                            Toast.makeText(getContext(), requestCode == GREETING_FILE_REQUEST_CODE ? R.string.greeting_file_successfully_saved : R.string.timeout_file_successfully_saved, Toast.LENGTH_LONG).show();
                        }
                        @Override
                        public void onError(Throwable e) {
                            Toast.makeText(getContext(), requestCode == GREETING_FILE_REQUEST_CODE ? R.string.failed_to_save_greeting_file : R.string.failed_to_save_timeout_file, Toast.LENGTH_LONG).show();
                            Log.e(getClass().getName(), "Failed to save audio file to app private storage", e);
                        }
                    });
            getPreferenceManager().getSharedPreferences().edit()
                    .putString(requestCode == GREETING_FILE_REQUEST_CODE ? "greeting_audio_file" : "timeout_audio_file", fileName)
                    .apply();
        }
    }

    private void initPreferenceValues() {
        ListPreference destinationDirectoryPref = (ListPreference) getPreferenceScreen().findPreference("destination_directory");
        String[] possibleDestinationDirs = getPossibleDestinationDirectories();
        destinationDirectoryPref.setEntries(possibleDestinationDirs);
        destinationDirectoryPref.setEntryValues(possibleDestinationDirs);
        if (destinationDirectoryPref.getValue() == null) destinationDirectoryPref.setValue(possibleDestinationDirs[0]);
    }

    @NonNull
    private String[] getPossibleDestinationDirectories() {
        File[] mediaDirs = getContext().getExternalMediaDirs();
        File[] dirs = Arrays.copyOf(mediaDirs, mediaDirs.length + 1);
        dirs[mediaDirs.length] = getContext().getFilesDir();
        return Arrays.stream(dirs).map(File::getAbsolutePath).toArray(String[]::new);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, @Nullable String key) {
        if (key == null) return;
        setPreferenceSummary(key);
        if (key.equals("message_encoding")) adjustSupportedSampleRates();
    }

    private void adjustSupportedSampleRates() {
        ListPreference codecPreference = (ListPreference) getPreferenceScreen().findPreference("message_encoding");
        ListPreference sampleRatePreference = (ListPreference) getPreferenceScreen().findPreference("message_sample_rate");
        ListPreference bitratePreference = (ListPreference) getPreferenceScreen().findPreference("message_bitrate");
        if (codecPreference.getValue().startsWith("1/")) { // If AMR-NB
            bitratePreference.setEnabled(false);
            sampleRatePreference.setEnabled(false);
            bitratePreference.setValue("0");
            sampleRatePreference.setValue("0"); // Only supports 8k
        } else if (codecPreference.getValue().startsWith("2/")) { // If AMR-WB
            bitratePreference.setEnabled(false);
            sampleRatePreference.setEnabled(false);
            bitratePreference.setValue("0");
            sampleRatePreference.setValue("0"); // Only supports 16k
        } else {
            bitratePreference.setEnabled(true);
            sampleRatePreference.setEnabled(true);
        }
        if (codecPreference.getValue().startsWith("4/")) { // If HE-AAC
            sampleRatePreference.setEntries(R.array.message_sample_rate_entries_he_aac);
            sampleRatePreference.setEntryValues(R.array.message_sample_rate_entry_values_he_aac);
            if (sampleRatePreference.getValue().equals("0") || sampleRatePreference.getValue().equals("8000")) {
                sampleRatePreference.setValue("16000");
            }
        } else {
            sampleRatePreference.setEntries(R.array.message_sample_rate_entries);
            sampleRatePreference.setEntryValues(R.array.message_sample_rate_entry_values);
        }
        setPreferenceSummary(bitratePreference, bitratePreference.getKey());
        setPreferenceSummary(sampleRatePreference, sampleRatePreference.getKey());
    }

    private void initPreferenceSummaries(@NonNull PreferenceGroup preferences) {
        for (int i = 0; i < preferences.getPreferenceCount(); i++) {
            Preference pref = preferences.getPreference(i);

            if (pref instanceof PreferenceGroup) {
                initPreferenceSummaries((PreferenceGroup) pref);
                continue;
            }

            String key = pref.getKey();
            if (key == null) continue;
            setPreferenceSummary(pref, key);
        }
    }

    private void setPreferenceSummary(@NotNull Preference pref, @NotNull String key) {
        switch (key) {
            case "destination_directory":
            case "message_max_duration":
            case "message_cooldown_interval":
            case "message_encoding":
            case "message_bitrate":
            case "message_sample_rate":
            case "incall_music_bus_number": {
                ListPreference preference = (ListPreference) pref;
                if (!preference.isEnabled()) {
                    preference.setSummary(R.string.not_supported_by_selected_codec);
                    break;
                }
                preference.setSummary(preference.getEntry());
                break;
            }
            case "allowed_country_codes": {
                MultiSelectListPreference preference = (MultiSelectListPreference) pref;
                Set<String> selectedCountryCodes = preference.getValues();
                if (selectedCountryCodes.isEmpty()) {
                    preference.setSummary(R.string.all_country_codes_allowed);
                    return;
                }
                preference.setSummary(String.join(", ", selectedCountryCodes.stream().map(value -> "+" + value).toArray(String[]::new)));
                break;
            }
            case "greeting_audio_file":
            case "timeout_audio_file":
                pref.setSummary(getPreferenceManager().getSharedPreferences().getString(key, (String) getText(R.string.no_file_selected)));
                break;
            case "wet_radio_open_last_server_response":
                File file = WetRadioUploader.getLastResponseFile(getContext());
                if (file.exists()) {
                    pref.setSummary(getString(R.string.last_modified_x, DateTimeFormatter
                            .ofLocalizedDateTime(FormatStyle.SHORT)
                            .withZone(ZoneId.systemDefault())
                            .format(Instant.ofEpochMilli(file.lastModified()))
                    ));
                } else {
                    pref.setSummary(R.string.no_response_saved_yet);
                }
                break;
            case "wet_radio_base_url":
                EditTextPreference preference = (EditTextPreference) pref;
                preference.setSummary(preference.getText());
        }
    }

    private void setPreferenceSummary(@NotNull String key) {
        setPreferenceSummary(getPreferenceScreen().findPreference(key), key);
    }

    @Nullable
    private String getFileNameFromContentProvider(Uri uri) {
        Cursor cursor = getContext().getContentResolver().query(uri, new String[]{OpenableColumns.DISPLAY_NAME}, null, null, null);
        int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
        String fileName = null;
        if (cursor.moveToFirst() && nameIndex != -1) {
            fileName = cursor.getString(nameIndex);
        }
        cursor.close();
        return fileName;
    }
}
