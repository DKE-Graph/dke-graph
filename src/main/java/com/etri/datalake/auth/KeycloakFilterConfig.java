package com.etri.datalake.auth;

import jakarta.servlet.Filter;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class KeycloakFilterConfig {
    private final KeycloakAdapter keycloakAdapter;

    @Bean
    public FilterRegistrationBean verifyUserFilter(){
        FilterRegistrationBean<Filter> filterRegistrationBean = new
                FilterRegistrationBean<>();
        filterRegistrationBean.setFilter(new KeycloakFilter(keycloakAdapter));
        filterRegistrationBean.setOrder(1);
        filterRegistrationBean.addUrlPatterns("/object-storage/*");
        return filterRegistrationBean;
    }
}
