package com.etri.sodasapi.common;

import com.etri.sodasapi.config.Constants;
import com.etri.sodasapi.utils.CustomAuthInterceptor;
import com.google.gson.Gson;
import okhttp3.*;
import org.twonote.rgwadmin4j.impl.RgwAdminException;

import java.io.IOException;
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
                .build();

        String jsonRateLimit = new Gson().toJson(rateLimit);

        Request request = new Request.Builder()
                .post(RequestBody.create(MediaType.parse("application/json; charset=utf-8"), jsonRateLimit))
                .url(url)
                .build();

        return safeCall(request);
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
