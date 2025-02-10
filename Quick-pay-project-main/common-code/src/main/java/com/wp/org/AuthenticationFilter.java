package com.wp.org;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;

import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
@Order(1)
public class AuthenticationFilter extends OncePerRequestFilter {

    private final RestTemplate restTemplate=new RestTemplate();
    private final String userServiceAuthUrl="http://localhost:8080/user-service/validate";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {    	 
    	String requestPath = request.getRequestURI();
    	
    	if(requestPath.startsWith("/user-service/validate"))
    	{
    		filterChain.doFilter(request, response);
            return;
    	}
    	
    	String requestId = request.getHeader("requestId");
        if (requestId == null || requestId.isBlank()) {
            requestId = UUID.randomUUID().toString();
        }
        MDC.put("requestId", requestId);

    	if (requestPath.equals("/user-service/user") || requestPath.startsWith("/user-service/validate") || requestPath.startsWith("/transaction-service/status") || requestPath.startsWith("/wallet-service/payment-status-capture")) {
            filterChain.doFilter(request, response);
            return;
        }
        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (authHeader == null || !authHeader.startsWith("Basic ")) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized");
            MDC.clear();
            return;
        }

            String base64Credentials = authHeader.substring(6);
            String credentials = new String(Base64.getDecoder().decode(base64Credentials), StandardCharsets.UTF_8);
            String[] values = credentials.split(":", 2);

            LoginRequest loginRequest = new LoginRequest();
            loginRequest.setUsername(values[0]);
            loginRequest.setPassword(values[1]);
            HttpEntity<LoginRequest> requestEntity = new HttpEntity<>(loginRequest);

            ResponseEntity<String> authResponse = restTemplate.postForEntity(userServiceAuthUrl, requestEntity, String.class);

            if (!authResponse.getStatusCode().is2xxSuccessful()) {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid credentials");
                MDC.clear();
                return;
            }

            filterChain.doFilter(request, response);
            MDC.clear();
    }
}
