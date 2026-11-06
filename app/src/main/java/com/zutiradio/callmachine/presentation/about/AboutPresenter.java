package com.zutiradio.callmachine.presentation.about;

import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.zutiradio.callmachine.BuildConfig;
import com.zutiradio.callmachine.call.InCallPlayer;

import java.io.File;

public class AboutPresenter extends Fragment implements AboutViewMvp.Listener {

    private AboutViewMvp aboutViewMvp;

    private Thread testRootThread;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        aboutViewMvp = new AboutViewMvpImpl(inflater, container, this);
        return aboutViewMvp.getRootView();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (testRootThread != null) {
            testRootThread.interrupt();
            testRootThread = null;
        }
    }

    @Override
    public void onGetVersionClicked() {
        aboutViewMvp.setVersionText("Call Machine: v" + BuildConfig.VERSION_NAME);
        aboutViewMvp.hideVersionButton();
    }

    @Override
    public void onPlayGreetingClicked() {
        InCallPlayer icp = new InCallPlayer(getContext(), getContext().getSharedPreferences(getContext().getPackageName()+"_preferences", Context.MODE_PRIVATE), null);
        icp.play(new File(getContext().getFilesDir(), "greeting"));
    }

    @Override
    public void onTestRootClicked() {
        testRootThread = new Thread(() -> {
            try {
                String result = InCallPlayer.runCommand(new String[]{"su", "-c", "whoami"});
                Log.i(getClass().getName(), "Root test exit code: " + result);
                new Handler(getContext().getMainLooper()).post(() -> {
                    aboutViewMvp.setTestRootResult(result, result.startsWith("root"));
                });
            } catch (Exception e) {
                Log.e(getClass().getName(), "Failed to perform root test", e);
                Context ctx = getContext();
                if (ctx == null) return;
                new Handler(ctx.getMainLooper()).post(() -> {
                    aboutViewMvp.setTestRootResult(e.getLocalizedMessage(), false);
                });
            }
        });
        testRootThread.start();
    }
}
