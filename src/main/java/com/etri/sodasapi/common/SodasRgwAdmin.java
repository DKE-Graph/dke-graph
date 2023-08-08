package com.etri.sodasapi.common;

import com.etri.sodasapi.config.Constants;
import com.etri.sodasapi.utils.CustomAuthInterceptor;
import com.google.gson.Gson;
import okhttp3.*;
import org.twonote.rgwadmin4j.impl.RgwAdminException;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;


public class SodasRgwAdmin {
    private final Constants constants;
    private final OkHttpClient client;

    public SodasRgwAdmin(Constants constants){
        this.constants = constants;
        this.client = new OkHttpClient.Builder()
                .addInterceptor(new CustomAuthInterceptor(constants.getRgwAdminAccess(), constants.getRgwAdminSecret()))
                .build();
    }

    public String getUserRateLimit(String uid){
        HttpUrl url = HttpUrl.parse(constants.getRgwEndpoint()+ "/admin")
                .newBuilder()
                .addPathSegment("ratelimit")
                .addQueryParameter("ratelimit-scope", "user")
                .addQueryParameter("uid", uid)
                .addQueryParameter("AWSAccessKeyId", constants.getRgwAdminAccess())
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
        HttpUrl url = HttpUrl.parse(constants.getRgwEndpoint()+ "/admin")
                .newBuilder()
                .addPathSegment("ratelimit")
                .addQueryParameter("ratelimit-scope", "user")
                .addQueryParameter("uid", uid)
                .addQueryParameter("AWSAccessKeyId", constants.getRgwAdminAccess())
                .addQueryParameter("format", "json")
                .build();


        Gson gson = new Gson();
        String jsonData = gson.toJson(toMap(rateLimit));
        RequestBody requestBody = RequestBody.create(jsonData, MediaType.parse("application/json; charset=utf-8"));


        Request request = new Request.Builder()
                .post(requestBody)
                .url(url)
                .build();

        return safeCall(request);
    }

    private static Map<String, Object> toMap(RateLimit rateLimit) {
        Map<String, Object> result = new HashMap<>();

        result.put("enabled", rateLimit.getEnabled());
        result.put("max-read-bytes", rateLimit.getMaxReadBytes());
        result.put("max-write-bytes", rateLimit.getMaxWriteBytes());
        result.put("max-read-opts", rateLimit.getMaxReadOpts());
        result.put("max-write-opts", rateLimit.getMaxWriteOpts());

        return result;
    }

    private String safeCall(Request request) {

        System.out.println(request.toString());
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
