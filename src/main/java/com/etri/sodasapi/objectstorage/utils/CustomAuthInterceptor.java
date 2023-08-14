package com.etri.sodasapi.objectstorage.utils;

import com.google.common.base.CharMatcher;
import com.google.gson.Gson;
import okhttp3.*;
import okio.Buffer;
import org.jetbrains.annotations.NotNull;
import org.springframework.boot.configurationprocessor.json.JSONObject;

import java.io.IOException;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

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


        String signature = sign(originalRequest.method(), String.valueOf(epo), resource, originalRequest.body());
        HttpUrl modifiedURL = originalRequest.url().newBuilder()
                .addQueryParameter("Expires", String.valueOf(epo))
                .addQueryParameter("Signature", signature)
                .build();
        Request signedRequest = originalRequest.newBuilder()
                .url(modifiedURL)
                .build();

        return chain.proceed(signedRequest);
    }

    private String sign(String httpVerb, String epo, String resource, RequestBody requestBody) {
        StringBuilder stringToSign = new StringBuilder(httpVerb + "\n\n\n" + epo + "\n");
        stringToSign.append(resource);

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

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    public String hexDigest(String message) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(message.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not found", e);
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
