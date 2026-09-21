package com.lms.apigateway.configuration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.gateway.config.GatewayProperties;
import org.springframework.cloud.gateway.filter.FilterDefinition;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.http.HttpMethod;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Comparator;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "jwt.signer-key=0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef",
        "eureka.client.enabled=false",
        "spring.cloud.discovery.enabled=false"
})
class GatewayRouteConfigurationTest {

    @Autowired
    RouteLocator routeLocator;

    @Autowired
    GatewayProperties gatewayProperties;

    @Test
    void publicCourseReadRequestsMatchPublicReadRoute() {
        assertThat(bestRouteId(HttpMethod.GET, "/api/course/courses")).isEqualTo("course-public-read");
        assertThat(bestRouteId(HttpMethod.GET, "/api/course/categories")).isEqualTo("course-public-read");
        assertThat(bestRouteId(HttpMethod.GET, "/api/course/courses/course-1")).isEqualTo("course-public-read");
        assertThat(bestRouteId(HttpMethod.GET, "/api/course/courses/course-1/structure"))
                .isEqualTo("course-public-read");
        assertThat(bestRouteId(HttpMethod.GET, "/api/course/courses/course-1/students/count"))
                .isEqualTo("course-public-read");
    }

    @Test
    void privateCourseReadShortcutsStayAuthenticated() {
        assertThat(bestRouteId(HttpMethod.GET, "/api/course/courses/my"))
                .isEqualTo("course-private-read-shortcuts");
        assertThat(bestRouteId(HttpMethod.GET, "/api/course/courses/enrolled"))
                .isEqualTo("course-private-read-shortcuts");

        List<String> filters = filterNames("course-private-read-shortcuts");
        assertThat(filters).contains("AuthFilter");
        assertThat(filters).doesNotContain("RequestRateLimiter");
    }

    @Test
    void courseMutationsStillMatchAuthenticatedRoute() {
        assertThat(bestRouteId(HttpMethod.POST, "/api/course/courses")).isEqualTo("course-service");
        assertThat(bestRouteId(HttpMethod.PUT, "/api/course/courses/course-1")).isEqualTo("course-service");
        assertThat(bestRouteId(HttpMethod.PATCH, "/api/course/courses/course-1/lock")).isEqualTo("course-service");
        assertThat(bestRouteId(HttpMethod.DELETE, "/api/course/courses/course-1")).isEqualTo("course-service");

        assertThat(filterNames("course-service")).contains("AuthFilter");
        assertThat(filterNames("course-service")).doesNotContain("RequestRateLimiter");
    }

    @Test
    void publicReadRoutesStripSpoofedGatewayHeaders() {
        assertThat(bestRouteId(HttpMethod.GET, "/api/blog/posts/public")).isEqualTo("blog-public");
        assertThat(bestRouteId(HttpMethod.GET, "/api/blog/tags")).isEqualTo("blog-public");
        assertThat(bestRouteId(HttpMethod.GET, "/api/identity/users/user-1/profile"))
                .isEqualTo("identity-public-profile");

        assertPublicReadFilters("course-public-read");
        assertPublicReadFilters("blog-public");
        assertPublicReadFilters("identity-public-profile");
    }

    @Test
    void sensitiveRoutesKeepAuthFilter() {
        assertThat(filterNames("identity-public")).doesNotContain("AuthFilter", "RequestRateLimiter");
        assertThat(filterNames("identity-storage")).contains("AuthFilter");
        assertThat(filterNames("storage-service")).contains("AuthFilter");
        assertThat(filterNames("assignment-quiz-ai-service")).contains("AuthFilter");
        assertThat(filterNames("assignment-testcase-ai-service")).contains("AuthFilter");
        assertThat(filterNames("chatbot-service")).contains("AuthFilter");
    }

    private String bestRouteId(HttpMethod method, String path) {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.method(method, path).build());

        return routeLocator.getRoutes()
                .filter(route -> Boolean.TRUE.equals(Mono.from(route.getPredicate().apply(exchange)).block()))
                .sort(Comparator.comparingInt(Route::getOrder))
                .map(Route::getId)
                .next()
                .block();
    }

    private void assertPublicReadFilters(String routeId) {
        List<String> filters = filterNames(routeId);
        assertThat(filters).doesNotContain("RequestRateLimiter", "OptionalRequestRateLimiter", "AuthFilter");

        String filterDefinitions = routeDefinition(routeId).getFilters().toString();
        assertThat(filterDefinitions)
                .contains("Authorization")
                .contains("X-User-Id")
                .contains("X-User-Role")
                .contains("X-User-Email")
                .contains("X-Gateway-Authenticated");
    }

    private List<String> filterNames(String routeId) {
        return routeDefinition(routeId).getFilters().stream()
                .map(FilterDefinition::getName)
                .toList();
    }

    private RouteDefinition routeDefinition(String routeId) {
        return gatewayProperties.getRoutes().stream()
                .filter(route -> route.getId().equals(routeId))
                .findFirst()
                .orElseThrow();
    }
}
