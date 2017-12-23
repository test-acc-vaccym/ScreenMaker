package com.screenmaker.screenmaker.storage;

import android.arch.persistence.room.*;
import android.arch.persistence.room.Dao;

import java.util.List;


@Dao
public interface DaoImageEntry {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public long[] insertUsers(ImageEntry... imageEntries);


    @Query("SELECT * FROM ImageEntry")
    public List<ImageEntry> getImage();

    @Query("DELETE FROM ImageEntry")
    public void clearAll();

}
