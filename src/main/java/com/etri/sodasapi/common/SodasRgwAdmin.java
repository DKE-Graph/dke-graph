package com.etri.sodasapi.common;

import com.etri.sodasapi.config.Constants;
import com.etri.sodasapi.utils.CustomAuthInterceptor;
import okhttp3.*;
import org.twonote.rgwadmin4j.impl.RgwAdminException;

import java.io.IOException;


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
        HttpUrl url = HttpUrl.parse(constants.getRgwEndpoint())
                .newBuilder()
                .addPathSegment("admin")
                .addPathSegment("ratelimit")
                .addQueryParameter("ratelimit-scope", "user")
                .addQueryParameter("uid", uid)
                .build();

        Request request = new Request.Builder()
                .get()
                .url(url)
                .build();

        String result = safeCall(request);
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
