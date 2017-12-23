package com.screenmaker.screenmaker.storage;

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.PrimaryKey;

/**
 * Created by user on 13.12.17.
 */
@Entity(tableName = "ImageEntry")
public class ImageEntry {

    @PrimaryKey(autoGenerate = true)
    private long id;

    //@ColumnInfo(typeAffinity=ColumnInfo.BLOB)
    private byte[] imageBytes;

    public ImageEntry(byte[] imageBytes) {
        this.imageBytes = imageBytes;
    }

    public byte[] getImageBytes() {
        return imageBytes;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }
}
