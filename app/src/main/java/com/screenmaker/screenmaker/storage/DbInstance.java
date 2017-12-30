package com.screenmaker.screenmaker.storage;

import android.arch.persistence.room.Database;
import android.arch.persistence.room.RoomDatabase;

import com.screenmaker.screenmaker.storage.cryptoinfo.CryptoInfo;
import com.screenmaker.screenmaker.storage.cryptoinfo.DaoCryptoInfo;
import com.screenmaker.screenmaker.storage.images.DaoImageEntry;
import com.screenmaker.screenmaker.storage.images.ImageEntry;

/**
 * Created by user on 29.12.17.
 */

@Database(entities = {ImageEntry.class, CryptoInfo.class}, version = 1, exportSchema = false)
public abstract class DbInstance extends RoomDatabase {

    public abstract DaoImageEntry gDaoImageEntry();

    public abstract DaoCryptoInfo gDaoCryptoInfo();

}