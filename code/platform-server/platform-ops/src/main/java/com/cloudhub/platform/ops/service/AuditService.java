package com.cloudhub.platform.ops.service;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cloudhub.platform.common.config.TenantContextHolder;
import com.cloudhub.platform.ops.domain.entity.LoginLog;
import com.cloudhub.platform.ops.domain.entity.OperLog;
import com.cloudhub.platform.ops.domain.mapper.OperLogMapper;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpHost;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.RestClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditService {

    private final LoginLogService loginLogService;
    private final OperLogMapper operLogMapper;

    @Value("${spring.elasticsearch.uris:http://localhost:9200}")
    private String esUri;

    /** O2: 复用 RestClient 实例（线程安全，支持连接池） */
    private RestClient esClient;

    public IPage<OperLog> operLogPage(String title, String operName, Integer businessType, Integer status,
                                      String startTime, String endTime, int pageNum, int pageSize) {
        LambdaQueryWrapper<OperLog> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.isNotBlank(title)) {
            wrapper.like(OperLog::getTitle, title);
        }
        if (StringUtils.isNotBlank(operName)) {
            wrapper.like(OperLog::getOperName, operName);
        }
        if (businessType != null) {
            wrapper.eq(OperLog::getBusinessType, businessType);
        }
        if (status != null) {
            wrapper.eq(OperLog::getStatus, status);
        }
        if (StringUtils.isNotBlank(startTime) && StringUtils.isNotBlank(endTime)) {
            wrapper.between(OperLog::getOperTime, startTime, endTime);
        }
        wrapper.orderByDesc(OperLog::getOperTime);
        return operLogMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
    }

    public IPage<LoginLog> loginLogPage(String username, Integer status, String startTime, String endTime,
                                        int pageNum, int pageSize) {
        return loginLogService.page(username, status, startTime, endTime, pageNum, pageSize, TenantContextHolder.getTenantId());
    }

    public Map<String, Object> searchElk(String keyword, String startTime, String endTime, int from, int size) {
        try {
            RestClient client = getClient();

            // O3: 使用 fastjson2 构建 JSON 体，替代手动字符串拼接 + escapeJson
            JSONObject query = new JSONObject();
            JSONObject bool = new JSONObject();
            JSONArray must = new JSONArray();

            if (StringUtils.isNotBlank(keyword)) {
                JSONObject multiMatch = new JSONObject();
                multiMatch.put("multi_match", JSONObject.of(
                    "query", keyword,
                    "fields", new String[]{"message", "level", "service"}
                ));
                must.add(multiMatch);
            } else {
                must.add(JSONObject.of("match_all", new JSONObject()));
            }

            if (startTime != null && endTime != null) {
                JSONObject range = new JSONObject();
                range.put("@timestamp", JSONObject.of("gte", startTime, "lte", endTime));
                must.add(JSONObject.of("range", range));
            }

            bool.put("must", must);
            query.put("query", JSONObject.of("bool", bool));
            query.put("from", from);
            query.put("size", size);
            query.put("sort", new JSONArray(){{ add(JSONObject.of("@timestamp", JSONObject.of("order", "desc"))); }});

            Request request = new Request("POST", "/platform-logs-*/_search");
            request.setJsonEntity(query.toJSONString());

            org.elasticsearch.client.Response response = client.performRequest(request);
            int statusCode = response.getStatusLine().getStatusCode();

            Map<String, Object> result = new HashMap<>();
            result.put("status", statusCode);
            try (Scanner scanner = new Scanner(response.getEntity().getContent())) {
                result.put("body", scanner.useDelimiter("\\A").hasNext() ? scanner.next() : "{}");
            }
            return result;
        } catch (IOException e) {
            log.warn("ELK查询失败: {}", e.getMessage());
            Map<String, Object> error = new HashMap<>();
            error.put("status", 500);
            error.put("error", "ELK查询失败: " + e.getMessage());
            return error;
        }
    }

    /** O2: 获取或创建 ES RestClient（单例，线程安全，支持连接池） */
    private RestClient getClient() {
        if (esClient == null) {
            synchronized (this) {
                if (esClient == null) {
                    String host = esUri.replace("http://", "").replace("https://", "");
                    String scheme = esUri.startsWith("https") ? "https" : "http";
                    int port = host.contains(":") ? Integer.parseInt(host.split(":")[1]) : 9200;
                    host = host.split(":")[0];
                    esClient = RestClient.builder(new HttpHost(host, port, scheme)).build();
                    log.info("ES RestClient 已创建: {}://{}:{}", scheme, host, port);
                }
            }
        }
        return esClient;
    }

    @PreDestroy
    public void closeClient() {
        if (esClient != null) {
            try {
                esClient.close();
                log.info("ES RestClient 已关闭");
            } catch (IOException e) {
                log.warn("ES RestClient 关闭异常", e);
            }
        }
    }
}
