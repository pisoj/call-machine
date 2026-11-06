package com.zutiradio.callmachine.presentation.about;

import com.zutiradio.callmachine.presentation.ViewMvp;

public interface AboutViewMvp extends ViewMvp {

    /**
     * This interface should be implemented by classes which instantiate MessageViewMvp in
     * order to get notifications about input events
     */
    interface Listener {
        void onGetVersionClicked();

        void onPlayGreetingClicked();

        void onTestRootClicked();
    }

    void hideVersionButton();

    void setVersionText(String msg);

    void setTestRootResult(String msg, boolean isSuccess);
}
