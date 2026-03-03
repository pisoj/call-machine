package com.zutiradio.broadcastos;

import android.app.Activity;
import android.app.FragmentManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.window.OnBackInvokedCallback;

import com.zutiradio.broadcastos.presentation.EdgeToEdge;
import com.zutiradio.broadcastos.presentation.preferences.PreferencesPresenter;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (savedInstanceState == null) {
            getFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container_view, new PreferencesPresenter())
                    .commit();
        }

        registerOnBackInvokedCallback();
    }

    private void registerOnBackInvokedCallback() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return;
        getOnBackInvokedDispatcher().registerOnBackInvokedCallback(0, new OnBackInvokedCallback() {
            @Override
            public void onBackInvoked() {
                onBackPressed();
            }
        });
    }

    @Override
    public void onBackPressed() {
        FragmentManager fragmentManager = getFragmentManager();
        if (fragmentManager.getBackStackEntryCount() > 0) {
            fragmentManager.popBackStack();
        } else {
            super.onBackPressed();
        }
    }
}
