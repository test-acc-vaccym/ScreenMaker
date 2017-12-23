package com.screenmaker.screenmaker.utils;


import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

public class ImageCryptoUtils {

    private static final String ALGORITHM_SECURE_RANDOM = "SHA1PRNG";
    private static final String ALGORITHM_PCKS5 = "AES/CBC/PKCS5Padding";
    private static final String ALGORITHM_SHA1 = "PBKDF2WithHmacSHA1";
    private static final String ALGORITHM_SECRET_KEY = "AES";

    private static final int ITERATIONS = 1024;
    private static final int KEY_LENGTH = 128;

    private String key;
    private String alias;

    public ImageCryptoUtils(String key, String alias) {
        this.key = key;
        this.alias = alias;
    }

    public byte[] encryptImage(byte[] imageInBytes) throws NoSuchPaddingException, InvalidKeyException,
            NoSuchAlgorithmException, IOException, InvalidAlgorithmParameterException, InvalidKeySpecException {

        Cipher cipher = getCipher(Cipher.ENCRYPT_MODE);

        ByteArrayInputStream bais = new ByteArrayInputStream(imageInBytes);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        CipherOutputStream cios = new CipherOutputStream(baos, cipher);

        byte[] buffer = new byte[16];
        int read;
        while ((read = bais.read(buffer)) != -1) {
            cios.write(buffer, 0, read);
            cios.flush();
        }

        cios.flush();
        cios.close();
        baos.close();
        bais.close();

        return baos.toByteArray();
    }

    public byte[] decryptImage(byte[] imageInBytesEncrypted) throws NoSuchPaddingException, InvalidKeyException,
            NoSuchAlgorithmException, IOException, InvalidAlgorithmParameterException, InvalidKeySpecException {

        Cipher cipher = getCipher(Cipher.DECRYPT_MODE);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ByteArrayInputStream bais = new ByteArrayInputStream(imageInBytesEncrypted);

        CipherInputStream ciis = new CipherInputStream(bais, cipher);

        byte buffer[] = new byte[16];
        int read;
        while ((read = ciis.read(buffer)) != -1) {
            baos.write(buffer, 0, read);
            baos.flush();
        }

        baos.flush();
        baos.close();
        bais.close();
        ciis.close();

        return baos.toByteArray();
    }


    private Cipher getCipher(int operationMode) throws NoSuchAlgorithmException, InvalidKeySpecException,
            UnsupportedEncodingException, NoSuchPaddingException, InvalidAlgorithmParameterException,
            InvalidKeyException {

        SecureRandom secureRandom = SecureRandom.getInstance(ALGORITHM_SECURE_RANDOM);
        IvParameterSpec ivParameterSpec = new IvParameterSpec(new byte[16]);

        Cipher cipher = Cipher.getInstance(ALGORITHM_PCKS5);

        cipher.init(
                operationMode,
                getSecretKey(ALGORITHM_SHA1),
                ivParameterSpec,
                secureRandom);
        return cipher;
    }

    private SecretKey getSecretKey(String secretKeyAlgorithm) throws NoSuchAlgorithmException,
            UnsupportedEncodingException, InvalidKeySpecException {

        SecretKeyFactory factory = SecretKeyFactory.getInstance(secretKeyAlgorithm);
        KeySpec spec = new PBEKeySpec(key.toCharArray(), alias.getBytes(), ITERATIONS, KEY_LENGTH);
        SecretKey tmp = factory.generateSecret(spec);

        return new SecretKeySpec(tmp.getEncoded(), ALGORITHM_SECRET_KEY);
    }



}
