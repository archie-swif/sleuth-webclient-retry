package com.example.demo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.time.Duration;

import static org.springframework.web.reactive.function.server.RequestPredicates.GET;

@SpringBootApplication
public class SleuthRetryApplication {
    private static Logger log = LoggerFactory.getLogger(SleuthRetryApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(SleuthRetryApplication.class, args);
    }

    @Autowired
    WebClient client;

    @Bean
    public WebClient webClient(@Value("${other.server.port}") String port) {
        return WebClient.create("http://localhost:" + port + "/");
    }

    @Bean
    public RouterFunction<ServerResponse> router() {
        return
                RouterFunctions
                        .route(GET("/test"), serverRequest -> {

                            log.info("Creating request mono");

                            Mono<String> resp = client
                                    .get()
                                    .uri("/error")
                                    .retrieve()
                                    .bodyToMono(String.class)
                                    .retryBackoff(3, Duration.ofMillis(100))
                                    .doOnError(throwable -> log.error("Error response", throwable));

                            log.info("Request mono is created");

                            return ServerResponse.ok().body(resp, String.class);
                        })

                        .andRoute(GET("/error"), serverRequest -> {
                            log.info("Returning 500");
                            return ServerResponse.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
                        });
    }
}
