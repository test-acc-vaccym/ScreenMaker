package com.screenmaker.screenmaker.storage.cryptoinfo;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.OnConflictStrategy;
import android.arch.persistence.room.Query;


@Dao
public interface DaoCryptoInfo {


    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public long[] insertImage(CryptoInfo... cryptoInfos);

    @Query("select * from CryptoInfo where keyTitle = :keyTitleStr")
    public CryptoInfo getAllImageEntries(String keyTitleStr);

}
