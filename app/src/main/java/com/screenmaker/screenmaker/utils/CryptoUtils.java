package com.screenmaker.screenmaker.utils;


import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Log;

import com.screenmaker.screenmaker.model.CryptoInfo;

import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.UnrecoverableEntryException;
import java.security.cert.CertificateException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

public class CryptoUtils {

    private final String ENCRYPTION_ALGORITHM_PCKS5 = "AES/GCM/NoPadding";

    public CryptoInfo encrypt(byte[] dataToEncrypt) throws NoSuchAlgorithmException, InvalidAlgorithmParameterException, NoSuchProviderException,
            InvalidKeyException, NoSuchPaddingException, BadPaddingException, IllegalBlockSizeException, IOException {

        SecretKey secretKey = getKey("alias");
        Log.e("myLogs", "encrypt " + secretKey);
        final Cipher cipher = Cipher.getInstance(ENCRYPTION_ALGORITHM_PCKS5);
        cipher.init(Cipher.ENCRYPT_MODE, secretKey);

        byte[] initVector = cipher.getIV();
        Log.e("myLogs", "initVector " + secretKey);

        byte[] cipheredData = cipher.doFinal(dataToEncrypt);
        Log.e("myLogs", "cipheredData " + cipheredData);
        Log.e("myLogs", "cipheredData " + cipheredData.length);
        Log.e("myLogs", "cipheredData end");
        return new CryptoInfo(initVector, cipheredData);
    }

    public byte[] decrypt(CryptoInfo cryptoInfo) throws KeyStoreException, UnrecoverableEntryException, NoSuchAlgorithmException,
            IOException, CertificateException, NoSuchPaddingException, InvalidAlgorithmParameterException, InvalidKeyException,
            BadPaddingException, IllegalBlockSizeException {
        KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
        keyStore.load(null);
        final KeyStore.SecretKeyEntry secretKeyEntry = (KeyStore.SecretKeyEntry) keyStore
                .getEntry("alias", null);

        final SecretKey secretKey = secretKeyEntry.getSecretKey();
        final Cipher cipher = Cipher.getInstance(ENCRYPTION_ALGORITHM_PCKS5);
        final GCMParameterSpec spec = new GCMParameterSpec(128, cryptoInfo.getInitializationVector());
        cipher.init(Cipher.DECRYPT_MODE, secretKey, spec);
        byte[] bytes = cipher.doFinal(cryptoInfo.getCipheredData());
        Log.e("myLogs", "bytes " + bytes);
        Log.e("myLogs", "bytes " + bytes.length);
        Log.e("myLogs", "bytes " + bytes[0]);
        Log.e("myLogs", "bytes " + bytes[1]);
        return bytes;
    }

    private SecretKey getKey(String alias) throws NoSuchProviderException, NoSuchAlgorithmException,
            InvalidKeyException, InvalidAlgorithmParameterException, NoSuchPaddingException {

        final KeyGenerator keyGenerator = KeyGenerator
                .getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");

        final KeyGenParameterSpec keyGenParameterSpec = new KeyGenParameterSpec.Builder(alias,
                KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .build();

        keyGenerator.init(keyGenParameterSpec);

        return keyGenerator.generateKey();
    }



}
