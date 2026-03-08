package com.zutiradio.callmachine.presentation.preferences;

import android.app.Dialog;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.support.annotation.Nullable;
import android.view.View;
import android.view.Window;
import android.widget.ListView;

import com.zutiradio.callmachine.presentation.EdgeToEdgeHelpers;

public abstract class EdgeToEdgePreferenceFragment extends PreferenceFragment {

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        View listView = view.findViewById(android.R.id.list);
        EdgeToEdgeHelpers.applyInsets(listView);
        if (listView instanceof ListView) {
            ((ListView) listView).setClipToPadding(false);
        }
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        boolean result = super.onPreferenceTreeClick(preferenceScreen, preference);
        applyInsetsToNestedPreferenceScreen(preference);
        return result;
    }

    private static void applyInsetsToNestedPreferenceScreen(Preference preference) {
        if (preference instanceof PreferenceScreen) {
            PreferenceScreen screen = (PreferenceScreen) preference;
            Dialog dialog = screen.getDialog();

            if (dialog != null) {
                Window window = dialog.getWindow();
                if (window != null) {
                    // Find the view inside the dialog (usually ListView)
                    // the content is often identified by android.R.id.list
                    View dialogListView = window.getDecorView().findViewById(android.R.id.list);
                    if (dialogListView != null) {
                        dialogListView.setFitsSystemWindows(false);
                        if (dialogListView instanceof ListView) {
                            ((ListView) dialogListView).setClipToPadding(false);
                        }
                        EdgeToEdgeHelpers.applyInsets(dialogListView);
                    }
                }
            }
        }
    }
}
