package com.zutiradio.broadcastos.testing;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.zutiradio.broadcastos.BuildConfig;

public class MessagePresenter extends Fragment implements MessageViewMvp.Listener {

    private MessageViewMvp messageViewMvp;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        messageViewMvp = new MessageViewMvpImpl(inflater, container, this);

        return messageViewMvp.getRootView();
    }

    @Override
    public void onUpdateClicked() {
        messageViewMvp.setMessage("BroadcastOS version: v" + BuildConfig.VERSION_NAME);
        messageViewMvp.hideUpdateButton();
    }
}
