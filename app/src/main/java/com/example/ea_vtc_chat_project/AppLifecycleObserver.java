package com.example.ea_vtc_chat_project;

import android.app.Application;

import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.ProcessLifecycleOwner;

public class AppLifecycleObserver implements DefaultLifecycleObserver {

    private static boolean isAppInForeground = false;

    public static void init(Application application) {
        ProcessLifecycleOwner.get().getLifecycle().addObserver(new AppLifecycleObserver());
    }

    @Override
    public void onStart(LifecycleOwner owner) {
        isAppInForeground = true;
    }

    @Override
    public void onStop(LifecycleOwner owner) {
        isAppInForeground = false;
    }

    public static boolean isAppInForeground() {
        return isAppInForeground;
    }
}




