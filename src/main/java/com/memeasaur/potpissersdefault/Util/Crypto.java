package com.memeasaur.potpissersdefault.Util;

import javax.crypto.*;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import static com.memeasaur.potpissersdefault.Util.Serialization.IO.Methods.handlePotpissersExceptions;

public class Crypto {
    public static final SecretKey IP_REFERRAL_REFERRER_KEY = new SecretKeySpec(System.getenv("JAVA_AES_IP_REFERRAL_REFERRER_KEY").getBytes(StandardCharsets.UTF_8), "AES");

    public static final SecretKey IP_REFERRAL_IP_HMAC_KEY = new SecretKeySpec(System.getenv("JAVA_AES_IP_REFERRAL_IP_KEY").getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    public static final SecretKey CURRENT_PUNISHMENTS_IP_HMAC_KEY = new SecretKeySpec(System.getenv("JAVA_AES_CURRENT_PUNISHMENTS_IP_KEY").getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    public static final SecretKey CURRENT_DEATHBANS_IP_HMAC_KEY = new SecretKeySpec(System.getenv("JAVA_AES_CURRENT_DEATHBANS_IP_KEY").getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    public static final SecretKey CURRENT_BANDITS_IP_HMAC_KEY = new SecretKeySpec(System.getenv("JAVA_AES_CURRENT_BANDITS_IP_KEY").getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    // TODO -> current punishments/deathbans/bandits correspond to plaintext id values, might not matter at all

    public static byte[] getHmacBytes(SecretKey secretKey, String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(secretKey);
            return mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        } catch (InvalidKeyException | NoSuchAlgorithmException e) {
            handlePotpissersExceptions(null, e);
            throw new RuntimeException(e);
        }
    }

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final GCMParameterSpec GCM_SPEC = new GCMParameterSpec(128, new byte[12]);
    public static byte[] getAesGcmFixedIvBytes(SecretKey key, String data) {
        try {
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, key, GCM_SPEC);

            return cipher.doFinal(data.getBytes(StandardCharsets.UTF_8));
        } catch (IllegalBlockSizeException | BadPaddingException | NoSuchAlgorithmException | NoSuchPaddingException |
                 InvalidKeyException | InvalidAlgorithmParameterException e) {
            handlePotpissersExceptions(null, e);
        }
        throw new RuntimeException("encrypt err");
    }
    public static String getAesGcmFixedIvString(SecretKey key, byte[] data) {
        try {
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, key, GCM_SPEC);

            return new String(cipher.doFinal(data), StandardCharsets.UTF_8);
        } catch (IllegalBlockSizeException | BadPaddingException | InvalidKeyException |
                 InvalidAlgorithmParameterException | NoSuchAlgorithmException | NoSuchPaddingException e) {
            handlePotpissersExceptions(null, e);
        }
        throw new RuntimeException("decrypt err");
    }
//    private static T handleAbstractCrypto TODO
}
