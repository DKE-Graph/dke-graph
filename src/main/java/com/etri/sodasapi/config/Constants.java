package com.etri.sodasapi.config;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class Constants {

    @Value("${MGR_ENDPOINT}")
    public String MGR_ENDPOINT;

    @Value("${RGW_ENDPOINT")
    public String RGW_ENPOINT;
}
