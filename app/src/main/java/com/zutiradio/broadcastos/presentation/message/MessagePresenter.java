package com.zutiradio.broadcastos.presentation.message;

import android.app.Fragment;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.zutiradio.broadcastos.BuildConfig;
import com.zutiradio.broadcastos.InCallPlayer;

import java.io.File;

public class MessagePresenter extends Fragment implements MessageViewMvp.Listener {

    private MessageViewMvp messageViewMvp;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        messageViewMvp = new MessageViewMvpImpl(inflater, container, this);
        return messageViewMvp.getRootView();
    }

    @Override
    public void onGetVersionClicked() {
        messageViewMvp.setMessage("BroadcastOS version: v" + BuildConfig.VERSION_NAME);
        messageViewMvp.hideUpdateButton();
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
