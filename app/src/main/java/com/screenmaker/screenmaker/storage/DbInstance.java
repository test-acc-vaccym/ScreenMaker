package com.screenmaker.screenmaker.storage;

import android.arch.persistence.room.Database;
import android.arch.persistence.room.RoomDatabase;

/**
 * Created by user on 13.12.17.
 */

@Database(entities = {ImageEntry.class}, version = 1, exportSchema = false)
public abstract class DbInstance extends RoomDatabase {

    public abstract DaoImageEntry getDaoImageEntry();

}
