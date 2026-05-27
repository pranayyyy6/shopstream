package com.shopstream.gateway.filter;

import com.shopstream.gateway.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

@Component
@Slf4j
/*
 * AbstractGatewayFilterFactory — Spring Cloud Gateway's way
 * of creating reusable filters you reference by name in yml.
 * We reference it as "AuthenticationFilter" in application.yml
 */
public class AuthenticationFilter
        extends AbstractGatewayFilterFactory<AuthenticationFilter.Config> {

    private final JwtUtil jwtUtil;

    // Paths that don't need a token
    private static final List<String> PUBLIC_PATHS = List.of(
            "/api/auth/login",
            "/api/auth/register",
            "/actuator"
    );

    public AuthenticationFilter(JwtUtil jwtUtil) {
        super(Config.class);
        this.jwtUtil = jwtUtil;
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            String path = request.getPath().toString();

            // Skip auth check for public endpoints
            boolean isPublic = PUBLIC_PATHS.stream()
                    .anyMatch(path::startsWith);

            if (isPublic) {
                return chain.filter(exchange);
            }

            // Check Authorization header exists
            if (!request.getHeaders().containsKey(HttpHeaders.AUTHORIZATION)) {
                log.warn("Missing Authorization header for path: {}", path);
                return onError(exchange, HttpStatus.UNAUTHORIZED,
                        "Missing Authorization header");
            }

            String authHeader = request.getHeaders()
                    .getFirst(HttpHeaders.AUTHORIZATION);

            // Must be Bearer token
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return onError(exchange, HttpStatus.UNAUTHORIZED,
                        "Invalid Authorization format");
            }

            String token = authHeader.substring(7);

            // Validate the JWT
            if (!jwtUtil.isTokenValid(token)) {
                log.warn("Invalid JWT token for path: {}", path);
                return onError(exchange, HttpStatus.UNAUTHORIZED,
                        "Invalid or expired token");
            }

            // Extract user info and forward to downstream service
            // Downstream services can read these headers
            // to know WHO is making the request
            String email = jwtUtil.extractEmail(token);
            String role = jwtUtil.extractRole(token);

            log.info("Authenticated request — user: {}, role: {}, path: {}",
                    email, role, path);

            /*
             * Mutate the request — add user info as headers.
             * Product Service can read X-User-Email to know
             * which user is browsing, without touching the JWT.
             * Services stay decoupled from auth logic.
             */
            ServerHttpRequest mutatedRequest = request.mutate()
                    .header("X-User-Email", email)
                    .header("X-User-Role", role)
                    .build();

            return chain.filter(exchange.mutate()
                    .request(mutatedRequest)
                    .build());
        };
    }

    private Mono<Void> onError(ServerWebExchange exchange,
                               HttpStatus status, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(status);
        response.getHeaders().add("Content-Type", "application/json");

        byte[] bytes = ("{\"error\":\"" + message + "\"}").getBytes();
        var buffer = response.bufferFactory().wrap(bytes);
        return response.writeWith(Mono.just(buffer));
    }

    // Config class required by AbstractGatewayFilterFactory
    // Add config fields here if needed (e.g. excluded paths per route)
    public static class Config {
    }
}