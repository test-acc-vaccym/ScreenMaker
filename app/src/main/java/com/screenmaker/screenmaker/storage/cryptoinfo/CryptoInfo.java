package com.screenmaker.screenmaker.storage.cryptoinfo;

import android.arch.persistence.room.Entity;
import android.arch.persistence.room.PrimaryKey;
import android.support.annotation.NonNull;

import java.util.Arrays;

/**
 * Created by user on 24.12.17.
 */

@Entity(tableName = "CryptoInfo")
public class CryptoInfo {

    @PrimaryKey
    @NonNull
    private String keyTitle;

    private String alias;

    private byte[] initializationVector;

    private byte[] cipheredData;

    public CryptoInfo(String keyTitle, String alias, byte[] initializationVector, byte[] cipheredData) {
        this.keyTitle = keyTitle;
        this.alias = alias;
        this.initializationVector = initializationVector;
        this.cipheredData = cipheredData;
    }

    public String getKeyTitle() {
        return keyTitle;
    }

    public String getAlias() {
        return alias;
    }

    public byte[] getInitializationVector() {
        return initializationVector;
    }

    public byte[] getCipheredData() {
        return cipheredData;
    }

    @Override
    public String toString() {
        return "CryptoInfo{" +
                "keyTitle='" + keyTitle + '\'' +
                ", alias='" + alias + '\'' +
                ", initializationVector=" + Arrays.toString(initializationVector) +
                ", cipheredData=" + Arrays.toString(cipheredData) +
                '}';
    }
}
