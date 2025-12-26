package com.notification.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.stream.Collectors;

@Slf4j
@Component
public class LoggingFilter implements WebFilter {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        long startTime = System.currentTimeMillis();
        String method = exchange.getRequest().getMethod().toString();
        String path = exchange.getRequest().getPath().value();

        // ОПТИМИЗАЦИЯ: Избегаем String конкатенации в stream map
        String queryParams = exchange.getRequest().getQueryParams().entrySet().stream()
                .map(entry -> String.format("%s=%s", entry.getKey(), entry.getValue()))
                .collect(Collectors.joining("&"));

        log.info("HTTP REQUEST - Method: {}, Path: {}, QueryParams: {}", method, path, queryParams);

        return chain.filter(exchange).doAfterTerminate(() -> {
            long duration = System.currentTimeMillis() - startTime;
            int status = exchange.getResponse().getStatusCode() != null ?
                    exchange.getResponse().getStatusCode().value() : 0;

            log.info("HTTP RESPONSE - Method: {}, Path: {}, Status: {}, Duration: {}ms",
                    method, path, status, duration);
        });
    }
}
