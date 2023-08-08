package com.etri.sodasapi.utils;

import okhttp3.HttpUrl;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

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

        long epochSeconds = System.currentTimeMillis() / 1000;
        long expiry = 15 * 60; // For example, if you want 15 minutes like JavaScript.
        long epo = epochSeconds + expiry;
        String resource = originalRequest.url().encodedPath();

/*        if (resource.startsWith("/admin")) {
            resource = resource.substring(6);
        }*/

        String signature = sign(originalRequest.method(), String.valueOf(epo), resource);

        HttpUrl modifiedURL = originalRequest.url().newBuilder()
                .addQueryParameter("Expires", String.valueOf(epo))
                .addQueryParameter("AWSAccessKeyId", accessKey) // Assuming you have 'accessKey' as a member variable or you can get it from some method.
                .addQueryParameter("Signature", signature)
                .build();

        Request signedRequest = originalRequest.newBuilder()
                .url(modifiedURL)
                .build();

        System.out.println(signedRequest.toString());

        return chain.proceed(signedRequest);
    }

    private String sign(String httpVerb, String epo, String resource) {
        StringBuilder stringToSign = new StringBuilder(httpVerb + "\n\n\n" + epo + "\n" + resource);

        try {
            Mac mac = Mac.getInstance("HmacSHA1");
            byte[] keyBytes = secretKey.getBytes(StandardCharsets.UTF_8);
            SecretKeySpec signingKey = new SecretKeySpec(keyBytes, "HmacSHA1");
            mac.init(signingKey);
            byte[] signBytes = mac.doFinal(stringToSign.toString().getBytes(StandardCharsets.UTF_8));
            String signature = encodeBase64(signBytes);
            //return "AWS " + accessKey + ":" + signature;
            return signature;
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
