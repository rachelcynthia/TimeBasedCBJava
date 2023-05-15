package org.example;

import com.sun.net.httpserver.HttpServer;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.vavr.control.Try;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.function.Supplier;

public class Service {

    CircuitBreakerConfig config = CircuitBreakerConfig
            .custom()
            .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.TIME_BASED)
            .minimumNumberOfCalls(2)
            .slidingWindowSize(10)
            .slowCallRateThreshold(50.0f)
            .slowCallDurationThreshold(Duration.ofSeconds(1))
            .waitDurationInOpenState(Duration.ofMinutes(1))
            .build();

    CircuitBreakerRegistry circuitBreakerRegistry =
            CircuitBreakerRegistry.of(config);

    CircuitBreaker circuitBreaker = circuitBreakerRegistry
            .circuitBreaker("circuit breaker");


    void callApi() throws IOException {
        ExternalService externalService = new ExternalService();
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
        server.createContext("/", (exchange -> {

            if ("GET".equals(exchange.getRequestMethod())) {

                // Will wrap this external service call in circuit breaker
                Supplier<String> decoratedSupplier = CircuitBreaker
                        .decorateSupplier(circuitBreaker, externalService::callExternalService);

                Try<String> responseText = Try.ofSupplier(decoratedSupplier);

                exchange.sendResponseHeaders(200, String.valueOf(responseText).getBytes().length);
                OutputStream output = exchange.getResponseBody();
                output.write(String.valueOf(responseText).getBytes());
                output.flush();
            } else {
                exchange.sendResponseHeaders(405, -1);
            }
            exchange.close();
        }));
        circuitBreaker.getEventPublisher().onEvent(event -> {
            System.out.println("State change {}" + event);
        });
        server.setExecutor(null);
        server.start();
    }
}
