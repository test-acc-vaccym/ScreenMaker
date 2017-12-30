package com.screenmaker.screenmaker.storage.images;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.OnConflictStrategy;
import android.arch.persistence.room.Query;

import com.screenmaker.screenmaker.storage.images.ImageEntry;

import java.util.List;

/**
 * Created by user on 29.12.17.
 */

@Dao
public interface DaoImageEntry {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public long[] insertImage(ImageEntry... entries);

    @Query("select * from ImageEntry")
    public List<ImageEntry> getAllImageEntries();

    @Query("delete from ImageEntry")
    public void deleteImages();

}
