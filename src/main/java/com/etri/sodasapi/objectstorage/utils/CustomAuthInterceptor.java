package com.etri.sodasapi.objectstorage.utils;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;

public class CustomAuthInterceptor implements Interceptor {

    private final String accessKey;
    private final String secretKey;

    public CustomAuthInterceptor(String accessKey, String secretKey) {
        this.accessKey = accessKey;
        this.secretKey = secretKey;
    }

    @NotNull
    @Override
    public Response intercept(Chain chain) throws IOException {
        Request originalRequest = chain.request();

        String date = DateTimeFormatter.RFC_1123_DATE_TIME.format(ZonedDateTime.now(ZoneId.of("GMT")));
        String resource = originalRequest.url().encodedPath();

        String signature = sign(originalRequest.method(), date, resource);

        Request signedRequest = originalRequest.newBuilder()
                .header("Authorization", signature)
                .header("Date", date)
                .build();

        return chain.proceed(signedRequest);
    }

    private String sign(String httpVerb, String date, String resource) {
        StringBuilder stringToSign = new StringBuilder(httpVerb + "\n\n\n" + date + "\n" + resource);

        try {
            Mac mac = Mac.getInstance("HmacSHA1");
            byte[] keyBytes = secretKey.getBytes(StandardCharsets.UTF_8);
            SecretKeySpec signingKey = new SecretKeySpec(keyBytes, "HmacSHA1");
            mac.init(signingKey);
            byte[] signBytes = mac.doFinal(stringToSign.toString().getBytes(StandardCharsets.UTF_8));
            String signature = encodeBase64(signBytes);
            return "AWS " + accessKey + ":" + signature;
        } catch (Exception e) {
            throw new RuntimeException("MAC CALC FAILED.", e);
        }
    }

    private static String encodeBase64(byte[] data) {
        String base64 = Base64.getEncoder().encodeToString(data);
        if (base64.endsWith("\r\n")) {
            base64 = base64.substring(0, base64.length() - 2);
        }
        if (base64.endsWith("\n")) {
            base64 = base64.substring(0, base64.length() - 1);
        }

        return base64;
    }
}
