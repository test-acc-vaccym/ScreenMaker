package com.screenmaker.screenmaker.storage.cryptoinfo;

import android.util.Log;

import com.screenmaker.screenmaker.storage.DbBuilder;
import com.screenmaker.screenmaker.utils.KeyCryptoUtils;

import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.UnrecoverableEntryException;
import java.security.cert.CertificateException;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

/**
 * Created by user on 30.12.17.
 */

public class ServiceCryptoInfo {

    private DaoCryptoInfo daoCryptoInfo;

    public ServiceCryptoInfo() {
        daoCryptoInfo = DbBuilder.getDb().gDaoCryptoInfo();
    }

    public long[] insertKey(CryptoInfo info){
        return daoCryptoInfo.insertImage(info);
    }

    public CryptoInfo getCryptoInfo(String keyTitle){
        return daoCryptoInfo.getAllImageEntries(keyTitle);
    }

    public long[] encryptAndInsert(String keyTitle, String keyToEncrypt, String keyAlias){
        try {
            CryptoInfo cryptoInfo = KeyCryptoUtils.encrypt(keyTitle, keyToEncrypt.getBytes(), keyAlias);
            return daoCryptoInfo.insertImage(cryptoInfo);
        } catch (NoSuchAlgorithmException | InvalidAlgorithmParameterException | NoSuchProviderException | InvalidKeyException | NoSuchPaddingException | BadPaddingException | IllegalBlockSizeException | IOException e) {
            Log.e("myLogs", "encryptAndInsert " + e.toString());
            return new long[]{-1};
        }
    }

    public String getAndDecrypt(String keyTitle){
        CryptoInfo cryptoInfo = getCryptoInfo(keyTitle);
        try {
            return new String(KeyCryptoUtils.decrypt(cryptoInfo));
        } catch (KeyStoreException | UnrecoverableEntryException | NoSuchAlgorithmException | IOException | CertificateException | InvalidAlgorithmParameterException | NoSuchPaddingException | InvalidKeyException | BadPaddingException | IllegalBlockSizeException e) {
            e.printStackTrace();
            return null;
        }
    }

}
