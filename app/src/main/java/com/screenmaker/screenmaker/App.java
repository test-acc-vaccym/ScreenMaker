package com.screenmaker.screenmaker;

import android.app.Application;


public class App extends Application {

    public static final String IMAGE_ENCRYPTION_KEY_TITLE = "ImageEncyptionKeyTitle";
    public static final String IMAGE_ENCRYPTION_ALIAS_TITLE = "ImageEncyptionAliasTitle";

    private static App instance;

    public App(){
        instance = this;
    }

    public static App getAppContext(){
        return instance;
    }

}
