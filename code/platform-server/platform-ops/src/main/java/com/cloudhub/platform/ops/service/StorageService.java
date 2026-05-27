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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class StorageService {

    private final StorageConfigMapper storageConfigMapper;

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
        config.setAccessKey((String) params.get("accessKey"));
        config.setSecretKey((String) params.get("secretKey"));
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
        if (params.containsKey("accessKey")) config.setAccessKey((String) params.get("accessKey"));
        if (params.containsKey("secretKey")) config.setSecretKey((String) params.get("secretKey"));
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
