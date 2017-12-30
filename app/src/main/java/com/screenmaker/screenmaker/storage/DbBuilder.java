package com.screenmaker.screenmaker.storage;


import android.arch.persistence.room.Room;

import com.screenmaker.screenmaker.App;

public class DbBuilder {

    private static final String DB_TITLE = "imagedatabase";

    public static DbInstance getDb(){
        return Room.databaseBuilder(App.getAppContext(),
                DbInstance.class, DB_TITLE).build();
    }

}
