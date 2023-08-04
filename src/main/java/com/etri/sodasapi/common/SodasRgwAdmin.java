package com.etri.sodasapi.common;

import com.etri.sodasapi.config.Constants;
import lombok.RequiredArgsConstructor;
import okhttp3.*;
import org.twonote.rgwadmin4j.RgwAdmin;
import org.twonote.rgwadmin4j.RgwAdminBuilder;
import org.twonote.rgwadmin4j.impl.RgwAdminException;
import org.twonote.rgwadmin4j.impl.RgwAdminImpl;
import org.twonote.rgwadmin4j.model.*;
import org.twonote.rgwadmin4j.model.Quota;
import software.amazon.awssdk.services.xray.model.Http;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RequiredArgsConstructor
public class SodasRgwAdmin {

    private RgwAdmin rgwAdmin;
    private final Constants constants;
    private final OkHttpClient client;

    public RgwAdmin getRgwAdmin() {
        if (this.rgwAdmin == null) {
            rgwAdmin = new RgwAdminBuilder().accessKey(constants.getRgwAdminAccess())
                    .secretKey(constants.getRgwAdminSecret())
                    .endpoint(constants.getRgwEndpoint() + "/admin")
                    .build();
        }
        new (access)
        return rgwAdmin;
    }

    public void getUserRateLimit(String uid){
        HttpUrl url = HttpUrl.parse(constants.getMgrEndpoint() + "/admin")
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
        System.out.println(result);
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
