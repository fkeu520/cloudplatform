package com.cloudhub.platform.ops.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cloudhub.platform.common.exception.BizException;
import com.cloudhub.platform.ops.domain.entity.StorageConfig;
import com.cloudhub.platform.ops.domain.mapper.StorageConfigMapper;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.ListBucketsArgs;
import io.minio.messages.Bucket;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class StorageService {

    private final StorageConfigMapper storageConfigMapper;

    @Value("${platform.storage.encrypt-key:change-me-in-production!}")
    private String encryptKey;

    /** O9: AES-GCM 密钥（由 encryptKey 派生） */
    private SecretKeySpec aesKey;

    @PostConstruct
    public void init() {
        if ("change-me-in-production!".equals(encryptKey)) {
            log.warn("O9: platform.storage.encrypt-key 使用默认值! accessKey/secretKey 将明文存储。生产环境请设置此密钥。");
        }
        try {
            byte[] keyBytes = encryptKey.getBytes(StandardCharsets.UTF_8);
            // SHA-256 派生 256 位密钥
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
            aesKey = new SecretKeySpec(md.digest(keyBytes), "AES");
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize storage encryption key", e);
        }
    }

    /** O9: AES-GCM 加密 */
    private String encrypt(String plaintext) {
        if (plaintext == null) return null;
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            byte[] iv = new byte[12];
            new SecureRandom().nextBytes(iv);
            GCMParameterSpec spec = new GCMParameterSpec(128, iv);
            cipher.init(Cipher.ENCRYPT_MODE, aesKey, spec);
            byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            ByteBuffer buf = ByteBuffer.allocate(iv.length + ciphertext.length);
            buf.put(iv);
            buf.put(ciphertext);
            return Base64.getEncoder().encodeToString(buf.array());
        } catch (Exception e) {
            throw new RuntimeException("Storage credential encryption failed", e);
        }
    }

    /** O9: AES-GCM 解密 */
    private String decrypt(String ciphertextB64) {
        if (ciphertextB64 == null) return null;
        try {
            ByteBuffer buf = ByteBuffer.wrap(Base64.getDecoder().decode(ciphertextB64));
            byte[] iv = new byte[12];
            buf.get(iv);
            byte[] ciphertext = new byte[buf.remaining()];
            buf.get(ciphertext);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, aesKey, new GCMParameterSpec(128, iv));
            return new String(cipher.doFinal(ciphertext), StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.warn("Storage credential decryption failed (may be plaintext from before encryption was added)", e);
            return ciphertextB64;
        }
    }

    public IPage<StorageConfig> page(String keyword, int pageNum, int pageSize) {
        LambdaQueryWrapper<StorageConfig> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.isNotBlank(keyword)) {
            wrapper.like(StorageConfig::getName, keyword).or().like(StorageConfig::getEndpoint, keyword);
        }
        wrapper.orderByAsc(StorageConfig::getSort);
        return storageConfigMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
    }

    public List<StorageConfig> list() {
        return storageConfigMapper.selectList(new LambdaQueryWrapper<StorageConfig>().orderByAsc(StorageConfig::getSort));
    }

    public StorageConfig getById(Long id) {
        return storageConfigMapper.selectById(id);
    }

    public void create(Map<String, Object> params) {
        StorageConfig config = new StorageConfig();
        config.setName((String) params.get("name"));
        config.setEndpoint((String) params.get("endpoint"));
        config.setAccessKey(encrypt((String) params.get("accessKey")));
        config.setSecretKey(encrypt((String) params.get("secretKey")));
        config.setRegion((String) params.get("region"));
        config.setIsSecure(params.get("isSecure") != null ? (Boolean) params.get("isSecure") : false);
        config.setDefaultBucket((String) params.get("defaultBucket"));
        config.setStatus(1);
        config.setRemark((String) params.get("remark"));
        config.setSort(params.get("sort") != null ? Integer.parseInt(params.get("sort").toString()) : 0);
        storageConfigMapper.insert(config);
    }

    public void update(Long id, Map<String, Object> params) {
        StorageConfig config = storageConfigMapper.selectById(id);
        if (config == null) throw new BizException("存储配置不存在");
        if (params.containsKey("name")) config.setName((String) params.get("name"));
        if (params.containsKey("endpoint")) config.setEndpoint((String) params.get("endpoint"));
        if (params.containsKey("accessKey")) config.setAccessKey(encrypt((String) params.get("accessKey")));
        if (params.containsKey("secretKey")) config.setSecretKey(encrypt((String) params.get("secretKey")));
        if (params.containsKey("region")) config.setRegion((String) params.get("region"));
        if (params.containsKey("isSecure")) config.setIsSecure((Boolean) params.get("isSecure"));
        if (params.containsKey("defaultBucket")) config.setDefaultBucket((String) params.get("defaultBucket"));
        if (params.containsKey("remark")) config.setRemark((String) params.get("remark"));
        if (params.containsKey("sort")) config.setSort(Integer.parseInt(params.get("sort").toString()));
        storageConfigMapper.updateById(config);
    }

    public void delete(Long id) {
        storageConfigMapper.deleteById(id);
    }

    public List<Bucket> listBuckets(Long configId) {
        StorageConfig config = storageConfigMapper.selectById(configId);
        if (config == null) throw new BizException("存储配置不存在");
        try {
            MinioClient client = buildClient(config);
            return client.listBuckets(ListBucketsArgs.builder().build());
        } catch (Exception e) {
            throw new BizException("获取Bucket列表失败: " + e.getMessage());
        }
    }

    public void createBucket(Long configId, String bucketName) {
        StorageConfig config = storageConfigMapper.selectById(configId);
        if (config == null) throw new BizException("存储配置不存在");
        try {
            MinioClient client = buildClient(config);
            boolean exists = client.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
            if (exists) throw new BizException("Bucket已存在: " + bucketName);
            client.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            throw new BizException("创建Bucket失败: " + e.getMessage());
        }
    }

    public boolean testConnection(Long configId) {
        StorageConfig config = storageConfigMapper.selectById(configId);
        if (config == null) throw new BizException("存储配置不存在");
        try {
            MinioClient client = buildClient(config);
            client.listBuckets(ListBucketsArgs.builder().build());
            return true;
        } catch (Exception e) {
            log.warn("MinIO连接测试失败: configId={}, error={}", configId, e.getMessage());
            return false;
        }
    }

    private MinioClient buildClient(StorageConfig config) {
        return MinioClient.builder()
                .endpoint(config.getEndpoint())
                .credentials(config.getAccessKey(), config.getSecretKey())
                .build();
    }
}
