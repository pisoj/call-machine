package com.zutiradio.broadcastos.testing;

import android.view.View;

public interface ViewMvp {

    /**
     * Get the root Android View which is used internally by this MVP View for presenting data
     * to the user.<br>
     * The returned Android View might be used by an MVP Presenter in order to query or alter the
     * properties of either the root Android View itself, or any of its child Android View's.
     * @return root Android View of this MVP View
     */
    View getRootView();
}
