package com.zutiradio.broadcastos.testing;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.zutiradio.broadcastos.R;

public class MessageViewMvpImpl extends AbstractViewMvp implements MessageViewMvp {

    private final Button button;
    private final TextView message;

    public MessageViewMvpImpl(LayoutInflater inflater, ViewGroup container, MessageViewMvp.Listener listener) {
        setRootView(inflater.inflate(R.layout.screen_message, container, false));

        button = findViewById(R.id.button);
        message = findViewById(R.id.textView);

        button.setOnClickListener(view -> {
            listener.onUpdateClicked();
        });
    }

    @Override
    public void hideUpdateButton() {
        button.setVisibility(View.INVISIBLE);
    }

    @Override
    public void setMessage(String msg) {
        message.setText(msg);
    }
}
