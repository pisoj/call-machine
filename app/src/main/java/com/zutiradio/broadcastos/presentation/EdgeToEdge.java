package com.zutiradio.broadcastos.presentation;

import android.support.annotation.NonNull;
import android.view.View;

public class EdgeToEdge {

    public static void applyInsets(View view) {
        if (view != null) {
            // Ensure drawing behind the system bars
            view.setFitsSystemWindows(false);

            view.setOnApplyWindowInsetsListener(new View.OnApplyWindowInsetsListener() {
                @NonNull
                @Override
                public android.view.WindowInsets onApplyWindowInsets(@NonNull View v, @NonNull android.view.WindowInsets insets) {
                    // For API 30+ use getInsets. For older use getSystemWindowInsetTop/Bottom
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                        android.graphics.Insets typeInsets = insets.getInsets(
                                android.view.WindowInsets.Type.systemBars()
                        );
                        v.setPadding(0, typeInsets.top, 0, typeInsets.bottom);
                    } else {
                        v.setPadding(0, insets.getSystemWindowInsetTop(), 0, insets.getSystemWindowInsetBottom());
                    }

                    // Return the insets so children don't consume them unexpectedly
                    return insets;
                }
            });
        }
    }
}
