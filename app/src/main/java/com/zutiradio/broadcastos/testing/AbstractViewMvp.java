package com.zutiradio.broadcastos.testing;

import android.content.Context;
import android.support.annotation.IdRes;
import android.support.annotation.StringRes;
import android.view.View;

import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Set;

public abstract class AbstractViewMvp implements ViewMvp {

    private View rootView;

    /**
     * Set the root android view of this MVP view
     */
    protected void setRootView(@NotNull View rootView) {
        this.rootView = rootView;
    }

    @Override
    public View getRootView() {
        return rootView;
    }

    protected <T extends View> T findViewById(@IdRes int id) {
        return (T) rootView.findViewById(id);
    }

    protected Context getContext() {
        return rootView.getContext();
    }

    protected String getString(@StringRes int id) {
        return getContext().getString(id);
    }
}
