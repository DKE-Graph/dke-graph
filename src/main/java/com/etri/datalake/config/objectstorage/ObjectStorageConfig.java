package com.etri.datalake.config.objectstorage;


import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "object-storage")
public final class ObjectStorageConfig {

    private String mgrEndpoint;
    private String rgwEndpoint;
    private String rgwAdminAccess;
    private String rgwAdminSecret;

    public void setMgrEndpoint(String mgrEndpoint) {
        this.mgrEndpoint = mgrEndpoint;
    }

    public void setRgwEndpoint(String rgwEndpoint) {
        this.rgwEndpoint = rgwEndpoint;
    }

    public void setRgwAdminAccess(String rgwAdminAccess) {
        this.rgwAdminAccess = rgwAdminAccess;
    }

    public void setRgwAdminSecret(String rgwAdminSecret) {
        this.rgwAdminSecret = rgwAdminSecret;
    }

    public void setRgwAdminUID(String rgwAdminUID) {
        this.rgwAdminUID = rgwAdminUID;
    }

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
