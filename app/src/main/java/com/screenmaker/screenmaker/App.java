package com.screenmaker.screenmaker;

import android.app.Application;

/**
 * Created by user on 13.12.17.
 */

public class App extends Application {

    private static App instance;

    public App(){
        instance = this;
    }

    public static App getAppContext(){
        return instance;
    }
}
