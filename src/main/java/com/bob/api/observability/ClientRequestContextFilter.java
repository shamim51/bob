package com.bob.api.observability;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Adds safe client and request metadata to every log emitted while an HTTP request is handled.
 * Datadog log injection plus LogstashEncoder export these MDC entries as searchable attributes.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class ClientRequestContextFilter extends OncePerRequestFilter {

    static final String USER_AGENT = "user_agent.original";
    static final String CLIENT_ADDRESS = "client.address.anonymized";
    static final String HTTP_METHOD = "http.request.method";

    private static final int USER_AGENT_MAX_LENGTH = 512;
    private static final int ATTRIBUTE_MAX_LENGTH = 128;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        Map<String, String> attributes = new LinkedHashMap<>();
        attributes.put(USER_AGENT, sanitize(request.getHeader("User-Agent"), USER_AGENT_MAX_LENGTH));
        attributes.put(CLIENT_ADDRESS, anonymizeAddress(request.getRemoteAddr()));
        attributes.put(HTTP_METHOD, sanitize(request.getMethod(), ATTRIBUTE_MAX_LENGTH));

        Map<String, String> previousValues = new LinkedHashMap<>();
        attributes.forEach((key, value) -> {
            previousValues.put(key, MDC.get(key));
            if (value != null) {
                MDC.put(key, value);
            } else {
                MDC.remove(key);
            }
        });

        try {
            filterChain.doFilter(request, response);
        } finally {
            previousValues.forEach((key, value) -> {
                if (value == null) {
                    MDC.remove(key);
                } else {
                    MDC.put(key, value);
                }
            });
        }
    }

    static String sanitize(String value, int maxLength) {
        if (value == null || value.isBlank()) {
            return null;
        }

        StringBuilder sanitized = new StringBuilder(Math.min(value.length(), maxLength));
        for (int i = 0; i < value.length() && sanitized.length() < maxLength; i++) {
            char character = value.charAt(i);
            sanitized.append(Character.isISOControl(character) ? ' ' : character);
        }
        return sanitized.toString().trim();
    }

    /**
     * Retains only the network prefix so logs remain useful for traffic analysis without
     * storing a complete client IP address: /24 for IPv4 and /64 for IPv6.
     */
    static String anonymizeAddress(String value) {
        String sanitized = sanitize(value, ATTRIBUTE_MAX_LENGTH);
        if (sanitized == null) {
            return null;
        }

        if (!sanitized.contains(":")) {
            String[] octets = sanitized.split("\\.", -1);
            if (octets.length != 4) {
                return null;
            }
            for (String octet : octets) {
                try {
                    int number = Integer.parseInt(octet);
                    if (number < 0 || number > 255) {
                        return null;
                    }
                } catch (NumberFormatException exception) {
                    return null;
                }
            }
            return octets[0] + "." + octets[1] + "." + octets[2] + ".0";
        }

        String literal = sanitized.contains("%")
                ? sanitized.substring(0, sanitized.indexOf('%'))
                : sanitized;
        if (!literal.matches("[0-9A-Fa-f:.]+")) {
            return null;
        }

        try {
            byte[] address = InetAddress.getByName(literal).getAddress();
            if (address.length == 4) {
                address[3] = 0;
            } else {
                Arrays.fill(address, 8, address.length, (byte) 0);
            }
            return InetAddress.getByAddress(address).getHostAddress();
        } catch (UnknownHostException exception) {
            return null;
        }
    }
}
