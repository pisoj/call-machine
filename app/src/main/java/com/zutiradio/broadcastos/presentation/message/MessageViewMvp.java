package com.zutiradio.broadcastos.presentation.message;

import com.zutiradio.broadcastos.presentation.ViewMvp;

public interface MessageViewMvp extends ViewMvp {

    /**
     * This interface should be implemented by classes which instantiate MessageViewMvp in
     * order to get notifications about input events
     */
    interface Listener {
        void onGetVersionClicked();

        void onPlayNowClicked();

        void onDoNowClicked();
    }

    void hideUpdateButton();

    void setMessage(String msg);
}
