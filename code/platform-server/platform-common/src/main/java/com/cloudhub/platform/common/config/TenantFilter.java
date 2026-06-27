package com.cloudhub.platform.common.config;

import com.cloudhub.platform.common.util.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@Component
public class TenantFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        try {
            String auth = request.getHeader("Authorization");
            if (auth != null && auth.startsWith("Bearer ")) {
                String token = auth.substring(7);
                try {
                    JwtUtil.JwtClaims claims = JwtUtil.getAll(token);
                    if (claims != null) {
                        if (claims.userId() != null) TenantContextHolder.setUserId(Long.parseLong(claims.userId()));
                        if (claims.tenantId() != null) TenantContextHolder.setTenantId(claims.tenantId());
                    }
                } catch (Exception e) {
                    log.error("JWT token parsing failed, tenant context will be empty (multi-tenant isolation may be bypassed). tokenPrefix={}..., error={}",
                            token.length() > 10 ? token.substring(0, 10) : token, e.getMessage());
                }
            }
            chain.doFilter(request, response);
        } finally {
            TenantContextHolder.clear();
        }
    }
}
