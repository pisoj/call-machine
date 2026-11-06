package com.zutiradio.callmachine.presentation.about;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.zutiradio.callmachine.databinding.ScreenAboutBinding;

public class AboutViewMvpImpl implements AboutViewMvp {

    private ScreenAboutBinding b;

    public AboutViewMvpImpl(LayoutInflater inflater, ViewGroup container, AboutViewMvp.Listener listener) {
        b = ScreenAboutBinding.inflate(inflater, container, false);

        b.versionBtn.setOnClickListener(view -> listener.onGetVersionClicked());
        b.playGreetingBtn.setOnClickListener(view -> listener.onPlayGreetingClicked());
        b.testRootBtn.setOnClickListener(view -> listener.onTestRootClicked());
    }

    @Override
    public View getRootView() {
        return b.getRoot();
    }

    @Override
    public void hideVersionButton() {
        b.versionBtn.setVisibility(View.INVISIBLE);
    }

    @Override
    public void setVersionText(String msg) {
        b.versionTv.setText(msg);
    }

    @Override
    public void setTestRootResult(String msg, boolean isSuccess) {
        b.testRootTv.setText(msg);
        b.testRootContainer.setBackgroundColor(isSuccess ? 0xFF4CAF50 : 0xFFD33C31);
    }
}
