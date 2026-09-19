package com.bob.api.observability;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.AsyncHandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;

/**
 * Adds the matched Spring route template to controller and service logs without recording
 * path-variable values such as IDs or tokens.
 */
@Component
public class ClientRouteContextInterceptor implements AsyncHandlerInterceptor {

    static final String HTTP_ROUTE = "http.route";

    private static final String APPLIED_ATTRIBUTE = ClientRouteContextInterceptor.class.getName() + ".applied";
    private static final String PREVIOUS_VALUE_ATTRIBUTE = ClientRouteContextInterceptor.class.getName() + ".previousValue";
    private static final int ROUTE_MAX_LENGTH = 512;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        Object routeAttribute = request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
        String route = ClientRequestContextFilter.sanitize(
                routeAttribute == null ? null : routeAttribute.toString(),
                ROUTE_MAX_LENGTH);

        request.setAttribute(APPLIED_ATTRIBUTE, Boolean.TRUE);
        String previousValue = MDC.get(HTTP_ROUTE);
        if (previousValue != null) {
            request.setAttribute(PREVIOUS_VALUE_ATTRIBUTE, previousValue);
        }

        if (route == null) {
            MDC.remove(HTTP_ROUTE);
        } else {
            MDC.put(HTTP_ROUTE, route);
        }
        return true;
    }

    @Override
    public void afterCompletion(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler,
            Exception exception) {
        restorePreviousValue(request);
    }

    @Override
    public void afterConcurrentHandlingStarted(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler) {
        restorePreviousValue(request);
    }

    private void restorePreviousValue(HttpServletRequest request) {
        if (!Boolean.TRUE.equals(request.getAttribute(APPLIED_ATTRIBUTE))) {
            return;
        }

        Object previousValue = request.getAttribute(PREVIOUS_VALUE_ATTRIBUTE);
        if (previousValue == null) {
            MDC.remove(HTTP_ROUTE);
        } else {
            MDC.put(HTTP_ROUTE, previousValue.toString());
        }
        request.removeAttribute(APPLIED_ATTRIBUTE);
        request.removeAttribute(PREVIOUS_VALUE_ATTRIBUTE);
    }
}
