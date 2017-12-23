package com.screenmaker.screenmaker.storage;


import java.util.List;

public class ServiceImageEntry {

    private DaoImageEntry daoImageEntry;


    public ServiceImageEntry() {
        daoImageEntry = DbBuilder.getDbInstance().getDaoImageEntry();
    }

    public long[] insertImage(ImageEntry entry){
        return daoImageEntry.insertUsers(entry);
    }

    public List<ImageEntry> getAllEntries(){
        return daoImageEntry.getImage();
    }

    public void clearAll(){
        daoImageEntry.clearAll();
    }

}
