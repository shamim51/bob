package com.bob.api.config;

import com.bob.api.observability.ClientRouteContextInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final ClientRouteContextInterceptor clientRouteContextInterceptor;

    public WebMvcConfig(ClientRouteContextInterceptor clientRouteContextInterceptor) {
        this.clientRouteContextInterceptor = clientRouteContextInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(clientRouteContextInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns("/actuator/**");
    }
}
