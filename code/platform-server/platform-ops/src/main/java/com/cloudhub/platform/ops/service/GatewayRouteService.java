package com.cloudhub.platform.ops.service;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cloudhub.platform.common.exception.BizException;
import com.cloudhub.platform.ops.domain.entity.GatewayRoute;
import com.cloudhub.platform.ops.domain.mapper.GatewayRouteMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class GatewayRouteService {

    private final GatewayRouteMapper gatewayRouteMapper;
    private final RestTemplate restTemplate;

    @Value("${gateway.sync-url:http://host.docker.internal:8083/gateway/route/sync}")
    private String gatewaySyncUrl;

    public IPage<GatewayRoute> page(String keyword, Integer status, int pageNum, int pageSize) {
        LambdaQueryWrapper<GatewayRoute> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.isNotBlank(keyword)) {
            wrapper.like(GatewayRoute::getRouteName, keyword).or().like(GatewayRoute::getRouteId, keyword);
        }
        if (status != null) {
            wrapper.eq(GatewayRoute::getStatus, status);
        }
        wrapper.orderByAsc(GatewayRoute::getOrderNo);
        return gatewayRouteMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
    }

    public List<GatewayRoute> list(Integer status) {
        LambdaQueryWrapper<GatewayRoute> wrapper = new LambdaQueryWrapper<>();
        if (status != null) wrapper.eq(GatewayRoute::getStatus, status);
        wrapper.orderByAsc(GatewayRoute::getOrderNo);
        return gatewayRouteMapper.selectList(wrapper);
    }

    public GatewayRoute getById(Long id) {
        return gatewayRouteMapper.selectById(id);
    }

    public void create(Map<String, Object> params) {
        GatewayRoute route = new GatewayRoute();
        route.setRouteId((String) params.get("routeId"));
        route.setRouteName((String) params.get("routeName"));
        route.setUri((String) params.get("uri"));
        route.setPredicates((String) params.get("predicates"));
        route.setFilters((String) params.get("filters"));
        route.setOrderNo(params.get("orderNo") != null ? Integer.parseInt(params.get("orderNo").toString()) : 0);
        route.setStatus(1);
        route.setRemark((String) params.get("remark"));
        gatewayRouteMapper.insert(route);
        syncToGateway();
    }

    public void update(Long id, Map<String, Object> params) {
        GatewayRoute route = gatewayRouteMapper.selectById(id);
        if (route == null) throw new BizException("路由不存在");
        if (params.containsKey("routeName")) route.setRouteName((String) params.get("routeName"));
        if (params.containsKey("uri")) route.setUri((String) params.get("uri"));
        if (params.containsKey("predicates")) route.setPredicates((String) params.get("predicates"));
        if (params.containsKey("filters")) route.setFilters((String) params.get("filters"));
        if (params.containsKey("orderNo")) route.setOrderNo(Integer.parseInt(params.get("orderNo").toString()));
        if (params.containsKey("remark")) route.setRemark((String) params.get("remark"));
        gatewayRouteMapper.updateById(route);
        syncToGateway();
    }

    public void delete(Long id) {
        gatewayRouteMapper.deleteById(id);
        syncToGateway();
    }

    public void toggleStatus(Long id) {
        GatewayRoute route = gatewayRouteMapper.selectById(id);
        if (route == null) throw new BizException("路由不存在");
        route.setStatus(route.getStatus() == 1 ? 0 : 1);
        gatewayRouteMapper.updateById(route);
        syncToGateway();
    }

    private void syncToGateway() {
        try {
            List<GatewayRoute> enabledRoutes = list(1);
            List<Map<String, Object>> routeDefs = new ArrayList<>();
            for (GatewayRoute r : enabledRoutes) {
                Map<String, Object> def = new LinkedHashMap<>();
                def.put("id", r.getRouteId());
                def.put("uri", URI.create(r.getUri()));
                def.put("order", r.getOrderNo());

                if (StringUtils.isNotBlank(r.getPredicates())) {
                    def.put("predicates", JSONArray.parse(r.getPredicates()));
                }
                if (StringUtils.isNotBlank(r.getFilters())) {
                    def.put("filters", JSONArray.parse(r.getFilters()));
                }
                routeDefs.add(def);
            }
            restTemplate.postForEntity(gatewaySyncUrl, routeDefs, String.class);
            log.info("网关路由同步成功: {}条", routeDefs.size());
        } catch (Exception e) {
            log.warn("网关路由同步失败: {}", e.getMessage());
        }
    }
}
