package com.screenmaker.screenmaker;




import com.screenmaker.screenmaker.storage.cryptoinfo.CryptoInfo;
import com.screenmaker.screenmaker.utils.KeyCryptoUtils;
import org.junit.Test;

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

import static org.junit.Assert.*;

public class KeyCryptoUtilsTest {


    @Test
    public void testEncrypt() throws IOException, NoSuchPaddingException, InvalidKeyException,
            NoSuchAlgorithmException, IllegalBlockSizeException, BadPaddingException, NoSuchProviderException,
            InvalidAlgorithmParameterException, CertificateException, UnrecoverableEntryException, KeyStoreException {

        String testString = "test key";
        CryptoInfo cryptoInfo = KeyCryptoUtils.encrypt("keyTitle", testString.getBytes("UTF-8"), "alias");
        System.out.println("cryptoInfo " + cryptoInfo);
        assertNotNull(cryptoInfo);
        assertNotNull(cryptoInfo.getCipheredData());
        assertNotNull(cryptoInfo.getInitializationVector());

        byte[] decryptedData = KeyCryptoUtils.decrypt(cryptoInfo);

        assertNotNull(decryptedData);
        assertEquals(testString, new String(decryptedData, "UTF-8"));
    }

}