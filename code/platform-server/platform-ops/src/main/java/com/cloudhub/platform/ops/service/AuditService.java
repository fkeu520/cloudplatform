package com.cloudhub.platform.ops.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cloudhub.platform.ops.domain.entity.LoginLog;
import com.cloudhub.platform.ops.domain.entity.OperLog;
import com.cloudhub.platform.ops.domain.mapper.OperLogMapper;
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
        return loginLogService.page(username, status, startTime, endTime, pageNum, pageSize);
    }

    public Map<String, Object> searchElk(String keyword, String startTime, String endTime, int from, int size) {
        try {
            String host = esUri.replace("http://", "").replace("https://", "");
            String scheme = esUri.startsWith("https") ? "https" : "http";
            int port = host.contains(":") ? Integer.parseInt(host.split(":")[1]) : 9200;
            host = host.split(":")[0];

            RestClient client = RestClient.builder(new HttpHost(host, port, scheme)).build();

            StringBuilder query = new StringBuilder();
            query.append("{\"query\":{\"bool\":{\"must\":[");
            if (keyword != null && !keyword.isBlank()) {
                query.append("{\"multi_match\":{\"query\":\"").append(keyword).append("\",\"fields\":[\"message\",\"level\",\"service\"]}}");
            } else {
                query.append("{\"match_all\":{}}");
            }
            if (startTime != null && endTime != null) {
                    query.append(",{\"range\":{\"@timestamp\":{\"gte\":\"").append(startTime)
                        .append("\",\"lte\":\"").append(endTime).append("\"}}}");
            }
            query.append("]}},\"from\":").append(from).append(",\"size\":").append(size)
                    .append(",\"sort\":[{\"@timestamp\":{\"order\":\"desc\"}}]}");

            Request request = new Request("POST", "/platform-logs-*/_search");
            request.setJsonEntity(query.toString());

            org.elasticsearch.client.Response response = client.performRequest(request);
            int statusCode = response.getStatusLine().getStatusCode();

            Map<String, Object> result = new HashMap<>();
            result.put("status", statusCode);
            try (Scanner scanner = new Scanner(response.getEntity().getContent())) {
                result.put("body", scanner.useDelimiter("\\A").hasNext() ? scanner.next() : "{}");
            }
            client.close();
            return result;
        } catch (IOException e) {
            log.warn("ELK查询失败: {}", e.getMessage());
            Map<String, Object> error = new HashMap<>();
            error.put("status", 500);
            error.put("error", "ELK查询失败: " + e.getMessage());
            return error;
        }
    }
}
