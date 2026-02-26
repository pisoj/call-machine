package com.zutiradio.broadcastos.testing;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.zutiradio.broadcastos.databinding.ScreenMessageBinding;

public class MessageViewMvpImpl implements MessageViewMvp {

    private ScreenMessageBinding b;

    public MessageViewMvpImpl(LayoutInflater inflater, ViewGroup container, MessageViewMvp.Listener listener) {
        b = ScreenMessageBinding.inflate(inflater, container, false);

        b.button.setOnClickListener(view -> listener.onUpdateClicked());
    }

    @Override
    public View getRootView() {
        return b.getRoot();
    }

    @Override
    public void hideUpdateButton() {
        b.button.setVisibility(View.INVISIBLE);
    }

    @Override
    public void setMessage(String msg) {
        b.textView.setText(msg);
    }
}
