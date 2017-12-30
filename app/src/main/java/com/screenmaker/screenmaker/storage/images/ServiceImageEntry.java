package com.screenmaker.screenmaker.storage.images;

import com.screenmaker.screenmaker.storage.DbBuilder;

import java.util.List;


public class ServiceImageEntry {


    private DaoImageEntry dbDaoImageEntry;


    public ServiceImageEntry() {
        dbDaoImageEntry = DbBuilder.getDb().gDaoImageEntry();
    }

    public long[] insertImage(ImageEntry... entries){
        return dbDaoImageEntry.insertImage(entries);
    }

    public List<ImageEntry> getAllImages(){
        return dbDaoImageEntry.getAllImageEntries();
    }

    public void clearAll(){
        dbDaoImageEntry.deleteImages();
    }


}
