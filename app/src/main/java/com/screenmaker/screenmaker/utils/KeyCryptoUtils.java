package com.screenmaker.screenmaker.utils;


import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Log;

import com.screenmaker.screenmaker.storage.cryptoinfo.CryptoInfo;

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

public class KeyCryptoUtils {

    private static final String ENCRYPTION_ALGORITHM_PCKS5 = "AES/GCM/NoPadding";
    private static final String KEY_GENERATOR_PROVIDER = "AndroidKeyStore";

    public static CryptoInfo encrypt(String ketTitle, byte[] dataToEncrypt, String alias) throws NoSuchAlgorithmException, InvalidAlgorithmParameterException, NoSuchProviderException,
            InvalidKeyException, NoSuchPaddingException, BadPaddingException, IllegalBlockSizeException, IOException {

        SecretKey secretKey = getKey(alias);
        Log.e("myLogs", "encrypt " + secretKey);
        final Cipher cipher = Cipher.getInstance(ENCRYPTION_ALGORITHM_PCKS5);
        cipher.init(Cipher.ENCRYPT_MODE, secretKey);

        byte[] initVector = cipher.getIV();
        Log.e("myLogs", "encrypt initVector " + secretKey);

        byte[] cipheredData = cipher.doFinal(dataToEncrypt);
        Log.e("myLogs", "encrypt cipheredData " + cipheredData);
        Log.e("myLogs", "encrypt cipheredData " + cipheredData.length);
        Log.e("myLogs", "encrypt cipheredData end");
        return new CryptoInfo(ketTitle, alias, initVector, cipheredData);
    }

    public static byte[] decrypt(CryptoInfo cryptoInfo) throws KeyStoreException, UnrecoverableEntryException, NoSuchAlgorithmException,
            IOException, CertificateException, NoSuchPaddingException, InvalidAlgorithmParameterException, InvalidKeyException,
            BadPaddingException, IllegalBlockSizeException {

        KeyStore keyStore = KeyStore.getInstance(KEY_GENERATOR_PROVIDER);
        keyStore.load(null);
        final KeyStore.SecretKeyEntry secretKeyEntry = (KeyStore.SecretKeyEntry) keyStore
                .getEntry(cryptoInfo.getAlias(), null);

        final SecretKey secretKey = secretKeyEntry.getSecretKey();
        final Cipher cipher = Cipher.getInstance(ENCRYPTION_ALGORITHM_PCKS5);

        final GCMParameterSpec spec = new GCMParameterSpec(128, cryptoInfo.getInitializationVector());
        cipher.init(Cipher.DECRYPT_MODE, secretKey, spec);
        byte[] bytes = cipher.doFinal(cryptoInfo.getCipheredData());
        Log.e("myLogs", "decrypt bytes " + bytes);
        Log.e("myLogs", "decrypt bytes " + bytes.length);
        return bytes;
    }

    private static SecretKey getKey(String alias) throws NoSuchProviderException, NoSuchAlgorithmException,
            InvalidKeyException, InvalidAlgorithmParameterException, NoSuchPaddingException {

        final KeyGenerator keyGenerator = KeyGenerator
                .getInstance(KeyProperties.KEY_ALGORITHM_AES, KEY_GENERATOR_PROVIDER);

        final KeyGenParameterSpec keyGenParameterSpec = new KeyGenParameterSpec.Builder(alias,
                KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .build();

        keyGenerator.init(keyGenParameterSpec);

        return keyGenerator.generateKey();
    }



}
