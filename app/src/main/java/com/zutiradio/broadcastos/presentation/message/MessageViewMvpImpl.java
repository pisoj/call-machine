package com.zutiradio.broadcastos.presentation.message;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.zutiradio.broadcastos.databinding.ScreenMessageBinding;
import com.zutiradio.broadcastos.presentation.EdgeToEdge;

public class MessageViewMvpImpl implements MessageViewMvp {

    private ScreenMessageBinding b;

    public MessageViewMvpImpl(LayoutInflater inflater, ViewGroup container, MessageViewMvp.Listener listener) {
        b = ScreenMessageBinding.inflate(inflater, container, false);

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
