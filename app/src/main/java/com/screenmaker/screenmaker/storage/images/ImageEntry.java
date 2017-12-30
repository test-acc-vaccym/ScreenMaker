package com.screenmaker.screenmaker.storage.images;

import android.arch.persistence.room.Entity;
import android.arch.persistence.room.Ignore;
import android.arch.persistence.room.PrimaryKey;

/**
 * Created by user on 29.12.17.
 */
@Entity(tableName = "ImageEntry")
public class ImageEntry {

    @PrimaryKey(autoGenerate = true)
    private long id;

    private byte[] imageData;

    protected ImageEntry(long id, byte[] imageData) {
        this.id = id;
        this.imageData = imageData;
    }

    @Ignore
    public ImageEntry(byte[] imageData) {
        this.imageData = imageData;
    }

    public long getId() {
        return id;
    }

    public byte[] getImageData() {
        return imageData;
    }

}
