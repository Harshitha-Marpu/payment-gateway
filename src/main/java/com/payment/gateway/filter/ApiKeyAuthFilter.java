package com.payment.gateway.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.payment.gateway.dto.ErrorResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
@Slf4j
public class ApiKeyAuthFilter extends OncePerRequestFilter {

    // Reads the API key from application.properties
    @Value("${payment.api.key}")
    private String validApiKey;

    private final ObjectMapper objectMapper = new ObjectMapper();

    // These paths don't need an API key
    private static final List<String> PUBLIC_PATHS = List.of(
        "/actuator",
        "/swagger-ui",
        "/swagger-ui.html",
        "/swagger-ui/index.html",
        "/api-docs",
        "/v3/api-docs",
        "/api-docs/swagger-config"
    );

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String requestPath = request.getRequestURI();

        // Skip auth for public paths
        if (isPublicPath(requestPath)) {
            filterChain.doFilter(request, response);
            return;
        }

        // Extract API key from header
        String apiKey = request.getHeader("X-API-Key");

        // Validate API key
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("Request to {} rejected - missing API key", requestPath);
            writeErrorResponse(response, "Missing API key", request);
            return;
        }

        if (!validApiKey.equals(apiKey)) {
            log.warn("Request to {} rejected - invalid API key", requestPath);
            writeErrorResponse(response, "Invalid API key", request);
            return;
        }

        // API key is valid — pass request to next filter/controller
        log.debug("Request to {} authenticated successfully", requestPath);
        filterChain.doFilter(request, response);
    }

    private boolean isPublicPath(String path) {
        return PUBLIC_PATHS.stream().anyMatch(path::startsWith);
    }

    private void writeErrorResponse(HttpServletResponse response,
                                    String message,
                                    HttpServletRequest request) throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        ErrorResponse error = ErrorResponse.of(
            "UNAUTHORIZED",
            message,
            request.getRequestURI()
        );

        // Manually serialize to JSON since we're outside Spring MVC
        objectMapper.findAndRegisterModules();
        response.getWriter().write(objectMapper.writeValueAsString(error));
    }
}
// This filter checks for the presence of a valid API key in the "X-API-Key" header for all incoming requests except those to public paths (like health checks). If the API key is missing or invalid, it responds with a 401 Unauthorized status and a JSON error message. Valid requests are passed down the filter chain.
// The filter uses SLF4J for logging and is ordered to run early in the filter chain (before other filters).
// The valid API key is read from the application.properties file, allowing for easy configuration without code changes.
// Note: Ensure that the "payment.api.key" property is set in your application.properties file for this filter to work correctly.
// Example application.properties entry:
// payment.api.key=your-secure-api-key-here
// Example log output for unauthorized access:
// WARN  >>> Request to /api/payments rejected - missing API key
// WARN  >>> Request to /api/payments rejected - invalid API key
// Example JSON error response for unauthorized access:
// {
//   "error": "UNAUTHORIZED",
//   "message": "Missing API key",
//   "path": "/api/payments"
// }
// Note: This filter should be registered in the Spring context and will automatically apply to all incoming requests. Make sure to test it with valid and invalid API keys to ensure it behaves as expected.
// Remember to handle sensitive information carefully in logs, especially in a payment gateway context. Avoid logging sensitive data such as credit card numbers or personal information.
// To use this filter, simply include it in your Spring Boot application. It will automatically check for the API key on incoming requests and enforce authentication as needed.
// You can further customize the error response format or add additional logging as needed.
// This implementation provides a basic structure for API key authentication. Depending on the application's needs, you may want to enhance it by adding support for multiple API keys, integrating with a database or external service for key validation, or implementing rate limiting based on the API key.
// Note: Ensure that the logging configuration is set up to capture WARN and DEBUG level logs for this to work effectively.
// This filter is designed to protect the API endpoints by requiring clients to provide a valid API key in the request header. It allows for easy configuration of the valid API key through application properties and provides clear logging for authentication attempts.
// Note: In a production environment, consider using a more secure method for storing and validating API keys, such as hashing the keys or using a secure vault. Additionally, consider implementing rate limiting to prevent abuse of the API key.
// End of ApiKeyAuthFilter.java