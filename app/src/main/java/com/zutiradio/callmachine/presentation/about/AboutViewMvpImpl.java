package com.zutiradio.callmachine.presentation.about;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.zutiradio.callmachine.databinding.ScreenAboutBinding;

public class AboutViewMvpImpl implements AboutViewMvp {

    private ScreenAboutBinding b;

    public AboutViewMvpImpl(LayoutInflater inflater, ViewGroup container, AboutViewMvp.Listener listener) {
        b = ScreenAboutBinding.inflate(inflater, container, false);

        b.getVersionBtn.setOnClickListener(view -> listener.onGetVersionClicked());
        b.playNowBtn.setOnClickListener(view -> listener.onPlayNowClicked());
        b.donow.setOnClickListener(view -> listener.onDoNowClicked());
    }

    @Override
    public View getRootView() {
        return b.getRoot();
    }

    @Override
    public void hideUpdateButton() {
        b.getVersionBtn.setVisibility(View.INVISIBLE);
    }

    @Override
    public void setMessage(String msg) {
        b.textView.setText(msg);
    }
}
