package com.cloudhub.platform.common.util;

import lombok.extern.slf4j.Slf4j;

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
 *
 * <p>C1: 生产环境应通过环境变量 {@code RSA_PRIVATE_KEY} / {@code RSA_PUBLIC_KEY}
 * 或系统属性 {@code platform.rsa.private-key} / {@code platform.rsa.public-key}
 * 配置持久密钥对，避免重启后前端公钥失效。</p>
 */
@Slf4j
public class RsaUtil {

    private static final String RSA_ALGORITHM = "RSA";
    private static final int KEY_SIZE = 2048;

    /**
     * RSA/ECB/PKCS1Padding 加密变换。
     *
     * <p><b>安全风险：</b>PKCS1Padding 存在已知的填充预言机攻击
     * (Bleichenbacher attack, CVE-2006-4339)，攻击者可利用 RSA 解密
     * 的失败响应逐字节恢复明文。本工具类用于密码传输加密：前端使用
     * 公钥加密密码，后端使用私钥解密。密码长度极短（通常 &le; 32 字节），
     * 在 HTTPS 传输前提下风险可控。</p>
     *
     * <p><b>迁移路径：</b>如需更高安全等级，应替换为 OAEP 填充：</p>
     * <pre>{@code
     * private static final String CIPHER_TRANSFORMATION = "RSA/ECB/OAEPWithSHA-256AndMGF1Padding";
     * }</pre>
     * <p>注意：切换至 OAEP 后，前端加密脚本需同步更新填充模式，
     * 否则后端解密将抛出 {@code BadPaddingException}。
     * OAEP 需要更低的数据长度上限（2048 位密钥下约 190 字节），
     * 对密码传输场景无影响。</p>
     *
     * <p>本常量仅用于 {@link Cipher#getInstance(String)} 的加密/解密操作。
     * 密钥生成仍使用 {@link #RSA_ALGORITHM}（即 "RSA" 算法名）。</p>
     */
    private static final String CIPHER_TRANSFORMATION = "RSA/ECB/PKCS1Padding";

    /** 缓存密钥对（生产环境应从安全存储读取） */
    private static final Map<String, KeyPair> KEY_PAIR_CACHE = new ConcurrentHashMap<>();
    private static final String DEFAULT_KEY_ID = "default";

    static {
        // C1: 优先从配置加载持久密钥对，避免重启后前端公钥失效
        String privateKeyB64 = getConfig("RSA_PRIVATE_KEY", "platform.rsa.private-key");
        String publicKeyB64 = getConfig("RSA_PUBLIC_KEY", "platform.rsa.public-key");
        if (privateKeyB64 != null && publicKeyB64 != null) {
            loadKeyPair(DEFAULT_KEY_ID, privateKeyB64, publicKeyB64);
            log.info("RSA key pair loaded from config (persistent across restarts)");
        } else {
            generateKeyPair(DEFAULT_KEY_ID);
            log.warn("RSA key pair generated on startup (non-persistent). Set RSA_PRIVATE_KEY/RSA_PUBLIC_KEY env vars for production to prevent restart-induced key changes.");
        }
    }

    /** 从环境变量或系统属性读取配置 */
    private static String getConfig(String envName, String propName) {
        String val = System.getenv(envName);
        if (val != null && !val.isBlank()) return val;
        val = System.getProperty(propName);
        if (val != null && !val.isBlank()) return val;
        return null;
    }

    /**
     * 从 Base64 编码的密钥对恢复 KeyPair
     */
    private static void loadKeyPair(String keyId, String privateKeyB64, String publicKeyB64) {
        try {
            byte[] privBytes = Base64.getDecoder().decode(privateKeyB64);
            byte[] pubBytes = Base64.getDecoder().decode(publicKeyB64);
            KeyFactory keyFactory = KeyFactory.getInstance(RSA_ALGORITHM);
            PrivateKey privateKey = keyFactory.generatePrivate(new PKCS8EncodedKeySpec(privBytes));
            PublicKey publicKey = keyFactory.generatePublic(new X509EncodedKeySpec(pubBytes));
            KEY_PAIR_CACHE.put(keyId, new KeyPair(publicKey, privateKey));
        } catch (Exception e) {
            throw new RuntimeException("Failed to load RSA key pair from config", e);
        }
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
            Cipher cipher = Cipher.getInstance(CIPHER_TRANSFORMATION);
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
            Cipher cipher = Cipher.getInstance(CIPHER_TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, publicKey);
            byte[] encryptedBytes = cipher.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(encryptedBytes);
        } catch (Exception e) {
            throw new RuntimeException("RSA 加密失败", e);
        }
    }
}
