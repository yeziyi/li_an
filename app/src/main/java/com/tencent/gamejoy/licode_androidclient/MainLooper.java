package com.tencent.gamejoy.licode_androidclient;


import android.os.Handler;
import android.os.Looper;

/**
 * Author: donnyliu
 */
public class MainLooper extends Handler {
    private static MainLooper instance = new MainLooper();

    public static MainLooper getInstance() {
        return instance;
    }

    public static void runOnUiThread(Runnable runnable) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            runnable.run();
        } else {
            getInstance().post(runnable);
        }
    }

    private MainLooper() {
        super(Looper.getMainLooper());
    }
}