package com.zutiradio.broadcastos.testing;

public interface MessageViewMvp extends ViewMvp {

    /**
     * This interface should be implemented by classes which instantiate MessageViewMvp in
     * order to get notifications about input events
     */
    interface Listener {
        void onUpdateClicked();
    }

    void hideUpdateButton();

    void setMessage(String msg);
}
