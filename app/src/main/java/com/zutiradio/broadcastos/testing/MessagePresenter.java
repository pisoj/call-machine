package com.zutiradio.broadcastos.testing;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

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
        messageViewMvp.setMessage("I am going to hide the button now :)");
        messageViewMvp.hideUpdateButton();
    }
}
