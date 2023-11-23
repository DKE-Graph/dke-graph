package com.etri.datalake.objectstorage.constants;

import com.etri.datalake.config.objectstorage.ObjectStorageConfig;
import com.etri.datalake.objectstorage.utils.CustomAuthInterceptor;
import okhttp3.*;
import org.springframework.stereotype.Component;
import org.twonote.rgwadmin4j.impl.RgwAdminException;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Component
public class SodasRgwAdmin {
    private final ObjectStorageConfig objectStorageConfig;
    private final OkHttpClient client;

    public SodasRgwAdmin(ObjectStorageConfig objectStorageConfig){
        this.objectStorageConfig = objectStorageConfig;
        this.client = new OkHttpClient.Builder()
                .addInterceptor(new CustomAuthInterceptor(objectStorageConfig.getRgwAdminAccess(), objectStorageConfig.getRgwAdminSecret()))
                .build();
    }

    public String getUserRateLimit(String uid){
        HttpUrl url = HttpUrl.parse(objectStorageConfig.getRgwEndpoint()+ "/admin")
                .newBuilder()
                .addPathSegment("ratelimit")
                .addQueryParameter("ratelimit-scope", "user")
                .addQueryParameter("uid", uid)
                .addQueryParameter("AWSAccessKeyId", objectStorageConfig.getRgwAdminAccess())
                .build();

        Request request = new Request.Builder()
                .get()
                .url(url)
                .header("Accept", "application/vnd.ceph.api.v1.0+json")
                .build();

        String result = safeCall(request);
        return result;
    }

    public String setUserRateLimit(String uid, RateLimit rateLimit){
        HttpUrl url = HttpUrl.parse(objectStorageConfig.getRgwEndpoint()+ "/admin")
                .newBuilder()
                .addPathSegment("ratelimit")
                .addQueryParameter("ratelimit-scope", "user")
                .addQueryParameter("uid", uid)
                .addQueryParameter("AWSAccessKeyId", objectStorageConfig.getRgwAdminAccess())
                .addQueryParameter("format", "json")
                .addQueryParameter("enabled", rateLimit.getEnabled())
                .addQueryParameter("max-read-bytes", String.valueOf(rateLimit.getMaxReadBytes()))
                .addQueryParameter("max-write-bytes", String.valueOf(rateLimit.getMaxWriteBytes()))
                .addQueryParameter("max-read-ops", String.valueOf(rateLimit.getMaxReadOps()))
                .addQueryParameter("max-write-ops", String.valueOf(rateLimit.getMaxWriteOps()))
                .build();


        RequestBody emptyBody = RequestBody.create(new byte[0]);
        Request request = new Request.Builder()
                .post(emptyBody)
                .url(url)
                .build();

        return safeCall(request);
    }

    private static Map<String, Object> toMap(RateLimit rateLimit) {
        Map<String, Object> result = new HashMap<>();

        result.put("enabled", rateLimit.getEnabled());
        result.put("max-read-bytes", rateLimit.getMaxReadBytes());
        result.put("max-write-bytes", rateLimit.getMaxWriteBytes());
        result.put("max-read-opts", rateLimit.getMaxReadOps());
        result.put("max-write-opts", rateLimit.getMaxWriteOps());

        return result;
    }

    private String safeCall(Request request) {

        try (Response response = client.newCall(request).execute()) {
            if (response.code() == 404) {
                return null;
            }
            if (!response.isSuccessful()) {
                throw new IOException("Unexpected code " + response);
            }
            ResponseBody body = response.body();
            if (body != null) {
                return response.body().string();
            } else {
                return null;
            }
        } catch (IOException e) {
            throw new RgwAdminException(500, "IOException", e);
        }
    }

}