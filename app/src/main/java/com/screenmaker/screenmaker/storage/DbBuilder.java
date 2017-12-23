package com.screenmaker.screenmaker.storage;


import android.arch.persistence.room.Room;

import com.screenmaker.screenmaker.App;

public class DbBuilder {

    private static final String TITLE_DB = "imagestorage";

    public static DbInstance getDbInstance(){
        return Room.databaseBuilder(App.getAppContext(),
                DbInstance.class, TITLE_DB).build();
    }

}
