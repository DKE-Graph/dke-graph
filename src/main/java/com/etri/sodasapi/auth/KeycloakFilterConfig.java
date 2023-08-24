package com.etri.sodasapi.auth;

import jakarta.servlet.Filter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class KeycloakFilterConfig {

    @Bean
    public FilterRegistrationBean verifyUserFilter(){
        FilterRegistrationBean<Filter> filterRegistrationBean = new
                FilterRegistrationBean<>();
        filterRegistrationBean.setFilter(new KeycloakFilter(new KeycloakAdapter()));
        filterRegistrationBean.setOrder(1);
        filterRegistrationBean.addUrlPatterns("/object-storage");
        return filterRegistrationBean;
    }
}
