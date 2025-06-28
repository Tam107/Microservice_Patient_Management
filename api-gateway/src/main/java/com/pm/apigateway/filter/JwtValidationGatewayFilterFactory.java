package com.pm.apigateway.filter;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;


@Component
public class JwtValidationGatewayFilterFactory extends
        AbstractGatewayFilterFactory<Object> {

    private final WebClient webClient;

    /**
     * Constructs an instance of JwtValidationGatewayFilterFactory.
     *
     * @param webClientBuilder the WebClient.Builder used to create a WebClient instance
     * @param authServiceUrl   the base URL of the authentication service used to validate JWT
     */
    public JwtValidationGatewayFilterFactory(WebClient.Builder webClientBuilder,
                                             @Value("${auth.service.url}") String authServiceUrl) {
        this.webClient = webClientBuilder.baseUrl(authServiceUrl).build();
    }


    /**
     * Applies a GatewayFilter that validates a JWT (JSON Web Token) for authorization.
     * The filter checks if the request contains a valid JWT in the Authorization header.
     * If the token is missing or invalid, an HTTP 401 Unauthorized response is returned.
     * If valid, the filter forwards the request to the designated chain after validating
     * the token with an external authentication service.
     *
     * @param config configuration object, which is currently unused but may provide additional
     *               configuration options in the future.
     * @return a {@link GatewayFilter} that validates the JWT and proceeds with the request processing chain.
     */
    @Override
    public GatewayFilter apply(Object config) {
        return (exchange, chain) -> {
            String token = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
            if (token == null || !token.startsWith("Bearer ")) {
                exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                return exchange.getResponse().setComplete();
            }
        return webClient.get()
                .uri("/validate")
                .header(HttpHeaders.AUTHORIZATION,  token)
                .retrieve()
                .toBodilessEntity()
                .then(chain.filter(exchange));
        };
    }
}
