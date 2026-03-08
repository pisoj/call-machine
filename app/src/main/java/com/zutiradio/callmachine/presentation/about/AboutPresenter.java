package com.zutiradio.callmachine.presentation.about;

import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.zutiradio.callmachine.BuildConfig;
import com.zutiradio.callmachine.call.InCallPlayer;

import java.io.File;

public class AboutPresenter extends Fragment implements AboutViewMvp.Listener {

    private AboutViewMvp aboutViewMvp;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        aboutViewMvp = new AboutViewMvpImpl(inflater, container, this);
        return aboutViewMvp.getRootView();
    }

    @Override
    public void onGetVersionClicked() {
        aboutViewMvp.setMessage("BroadcastOS version: v" + BuildConfig.VERSION_NAME);
        aboutViewMvp.hideUpdateButton();
    }

    @Override
    public void onPlayNowClicked() {
        InCallPlayer icp = new InCallPlayer(getContext(), getContext().getSharedPreferences(getContext().getPackageName()+"_preferences", Context.MODE_PRIVATE), null);
        icp.play(new File(getContext().getFilesDir(), "greeting"));
    }

    @Override
    public void onDoNowClicked() {
    }
}
