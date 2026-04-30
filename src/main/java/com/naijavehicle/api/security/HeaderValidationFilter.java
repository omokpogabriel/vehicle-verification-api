package com.naijavehicle.api.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@Slf4j
public class HeaderValidationFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        
        String path = request.getRequestURI();
        
        // Skip validation for actuator or other internal paths if necessary
        if (path.startsWith("/actuator")) {
            filterChain.doFilter(request, response);
            return;
        }

        String ipAddress = request.getHeader("ipaddress");
        String location = request.getHeader("location");
        String appInstallationId = request.getHeader("appInstallationId");

        if (!StringUtils.hasText(ipAddress) || !StringUtils.hasText(location) || !StringUtils.hasText(appInstallationId)) {
            log.warn("Missing required headers in request to {}. IP: {}, Location: {}, AppId: {}", 
                    path, ipAddress, location, appInstallationId);
            
            response.setContentType("application/json");
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("{\"error\": \"Missing required headers: ipaddress, location, and appInstallationId must be provided in the request headers.\"}");
            return;
        }

        filterChain.doFilter(request, response);
    }
}
