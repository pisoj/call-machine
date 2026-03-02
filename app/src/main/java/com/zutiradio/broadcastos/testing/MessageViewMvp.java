package com.zutiradio.broadcastos.testing;

public interface MessageViewMvp extends ViewMvp {

    /**
     * This interface should be implemented by classes which instantiate MessageViewMvp in
     * order to get notifications about input events
     */
    interface Listener {
        void onGetVersionClicked();

        void onPlayNowClicked();
    }

    void hideUpdateButton();

    void setMessage(String msg);
}
