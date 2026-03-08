package com.zutiradio.callmachine.presentation.about;

import com.zutiradio.callmachine.presentation.ViewMvp;

public interface AboutViewMvp extends ViewMvp {

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
