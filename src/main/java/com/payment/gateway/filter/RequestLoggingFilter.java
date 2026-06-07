package com.payment.gateway.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@Order(1)
@Slf4j
public class RequestLoggingFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        long startTime = System.currentTimeMillis();

        String method = request.getMethod();
        String uri = request.getRequestURI();
        String clientIp = getClientIp(request);

        try {
            // Pass request down the chain
            filterChain.doFilter(request, response);
        } finally {
            // This runs AFTER the response is sent
            long duration = System.currentTimeMillis() - startTime;
            int status = response.getStatus();

            // Log format: METHOD /path STATUS duration_ms [IP]
            if (status >= 400) {
                log.warn(">>> {} {} {} {}ms [{}]",
                    method, uri, status, duration, clientIp);
            } else {
                log.info(">>> {} {} {} {}ms [{}]",
                    method, uri, status, duration, clientIp);
            }
        }
    }

    private String getClientIp(HttpServletRequest request) {
        // Check for proxy headers first (when behind load balancer)
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}

// This filter logs the HTTP method, request URI, response status, processing time, and client IP address for each incoming request. It uses SLF4J for logging and is ordered to run early in the filter chain.
// Note: Ensure that the logging configuration is set up to capture INFO and WARN level logs for this to work effectively.
// Example log output:
// INFO  >>> POST /api/payments 200 150ms [192.168.1.1]
// WARN  >>> POST /api/payments 500 200ms [192.168.1.1]
// This implementation provides a basic structure for request logging. Depending on the application's needs, you may want to enhance it by adding more details (e.g., request parameters, user agent) or by integrating with a centralized logging system.
// Remember to handle sensitive information carefully in logs, especially in a payment gateway context. Avoid logging sensitive data such as credit card numbers or personal information.
// To use this filter, simply include it in your Spring Boot application. It will automatically log all incoming HTTP requests and their corresponding responses.
// You can further customize the logging format or add additional information as needed.
// Note: This filter is designed to be simple and efficient. For more complex logging requirements, consider using a more robust logging framework or integrating with a monitoring solution like ELK Stack or Prometheus.
// End of RequestLoggingFilter.java