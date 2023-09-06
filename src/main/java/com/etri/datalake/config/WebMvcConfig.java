package com.etri.datalake.config;

import com.etri.datalake.auth.GetIdFromTokenArgumentResolver;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {
    private final GetIdFromTokenArgumentResolver getIdFromTokenArgumentResolver;

    public WebMvcConfig(GetIdFromTokenArgumentResolver getIdFromTokenArgumentResolver) {
        this.getIdFromTokenArgumentResolver = getIdFromTokenArgumentResolver;
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers){
        resolvers.add(getIdFromTokenArgumentResolver);
    }
}
