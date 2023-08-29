package com.etri.sodasapi.config;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public final class ObjectStorageConfig {

    @Value("${MGR_ENDPOINT}")
    private String mgrEndpoint;

    @Value("${RGW_ENDPOINT}")
    private String rgwEndpoint;

    @Value("${RGW_ADMIN_ACCESS}")
    private String rgwAdminAccess;

    @Value("${RGW_ADMIN_SECRET}")
    private String rgwAdminSecret;

    @Value("${RGW_ADMIN_UID}")
    private String rgwAdminUID;

    public String getMgrEndpoint() {
        return mgrEndpoint;
    }

    public String getRgwEndpoint() {
        return rgwEndpoint;
    }

    public String getRgwAdminAccess() {
        return rgwAdminAccess;
    }

    public String getRgwAdminSecret() {
        return rgwAdminSecret;
    }

    public String getRgwAdminUID(){
        return getRgwAdminUID();
    }
}
