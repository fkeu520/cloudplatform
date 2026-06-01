package com.cloudhub.platform.common.util;

import javax.crypto.Cipher;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * RSA 非对称加密工具类
 * 用于密码传输加密（前端公钥加密，后端私钥解密）
 */
public class RsaUtil {

    private static final String RSA_ALGORITHM = "RSA";
    private static final int KEY_SIZE = 2048;

    /** 缓存密钥对（生产环境应从安全存储读取） */
    private static final Map<String, KeyPair> KEY_PAIR_CACHE = new ConcurrentHashMap<>();
    private static final String DEFAULT_KEY_ID = "default";

    static {
        // 启动时生成默认密钥对
        generateKeyPair(DEFAULT_KEY_ID);
    }

    /**
     * 生成 RSA 密钥对
     */
    public static KeyPair generateKeyPair(String keyId) {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance(RSA_ALGORITHM);
            generator.initialize(KEY_SIZE);
            KeyPair keyPair = generator.generateKeyPair();
            KEY_PAIR_CACHE.put(keyId, keyPair);
            return keyPair;
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("RSA 密钥生成失败", e);
        }
    }

    /**
     * 获取公钥（Base64 编码）
     */
    public static String getPublicKey(String keyId) {
        KeyPair keyPair = KEY_PAIR_CACHE.get(keyId);
        if (keyPair == null) {
            keyPair = generateKeyPair(keyId);
        }
        return Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded());
    }

    /**
     * 获取默认公钥
     */
    public static String getPublicKey() {
        return getPublicKey(DEFAULT_KEY_ID);
    }

    /**
     * 使用私钥解密数据
     */
    public static String decrypt(String encryptedData, String keyId) {
        try {
            KeyPair keyPair = KEY_PAIR_CACHE.get(keyId);
            if (keyPair == null) {
                throw new RuntimeException("密钥对不存在: " + keyId);
            }
            Cipher cipher = Cipher.getInstance(RSA_ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, keyPair.getPrivate());
            byte[] decryptedBytes = cipher.doFinal(Base64.getDecoder().decode(encryptedData));
            return new String(decryptedBytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("RSA 解密失败", e);
        }
    }

    /**
     * 使用默认私钥解密
     */
    public static String decrypt(String encryptedData) {
        return decrypt(encryptedData, DEFAULT_KEY_ID);
    }

    /**
     * 使用公钥加密数据
     */
    public static String encrypt(String data, String publicKeyBase64) {
        try {
            byte[] keyBytes = Base64.getDecoder().decode(publicKeyBase64);
            X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
            KeyFactory factory = KeyFactory.getInstance(RSA_ALGORITHM);
            PublicKey publicKey = factory.generatePublic(spec);
            Cipher cipher = Cipher.getInstance(RSA_ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, publicKey);
            byte[] encryptedBytes = cipher.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(encryptedBytes);
        } catch (Exception e) {
            throw new RuntimeException("RSA 加密失败", e);
        }
    }
}
