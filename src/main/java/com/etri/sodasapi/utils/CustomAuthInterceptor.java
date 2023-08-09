package com.etri.sodasapi.utils;

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

    public static <Buffer> Map<String, String> requestToMap(final RequestBody requestBody) {

        if(requestBody == null){
            return null;
        }

        String str = "";
        Map<String, String> map = new HashMap<>();
        try(okio.Buffer buffer = new okio.Buffer()){
            requestBody.writeTo(buffer);
            str = buffer.readUtf8();
        } catch (final IOException e) {
            str = "Failed to convert body to string";
        }
        try {

            JSONObject jsonObject = new JSONObject(str);
            Iterator keys = jsonObject.keys();
            while (keys.hasNext()) {
                String key = (String) keys.next();
                map.put(key, jsonObject.getString(key));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return map;
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

        String signature = sign(originalRequest.method(), String.valueOf(epo), resource, originalRequest.body());


        HttpUrl modifiedURL = originalRequest.url().newBuilder()
                .addQueryParameter("Expires", String.valueOf(epo))
                .addQueryParameter("Signature", signature)
                .build();

        Request signedRequest = originalRequest.newBuilder()
                .url(modifiedURL)
                .build();



        System.out.println(toCurlCommand(signedRequest));

        System.out.println(signedRequest.toString());

        return chain.proceed(signedRequest);
    }

    private String sign(String httpVerb, String epo, String resource, RequestBody requestBody) {
        StringBuilder stringToSign = new StringBuilder(httpVerb + "\n\n\n" + epo + "\n");

/*        if(requestBody != null){
            stringToSign.append(hexDigest("{\"test\":true}"));
            stringToSign.append("\n");
        }*/

        //resource = resource.replaceFirst("/admin", "");

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

    private String jsonStringify(TestObject obj) {
        Gson gson = new Gson();
        return gson.toJson(obj);
    }

    // Corresponding class for the test object you are trying to stringify
    public class TestObject {
        private boolean test;

        public TestObject(boolean test) {
            this.test = test;
        }

        // Getter and Setter methods for 'test' (optional, but can be useful)
    }

    public static String toCurlCommand(Request request) {
        StringBuilder curlCmd = new StringBuilder("curl -X ")
                .append(request.method())
                .append(" ");

        for (String headerName : request.headers().names()) {
            for (String headerValue : request.headers(headerName)) {
                curlCmd.append("-H \"").append(headerName).append(": ").append(headerValue).append("\" ");
            }
        }

        if (request.body() != null) {
            // for the sake of simplicity, assuming the request body to be plain text
            // for binary or other types of request bodies, further processing might be required
            curlCmd.append("-d '").append(request.body().toString()).append("' ");
        }

        curlCmd.append(request.url());

        return curlCmd.toString();
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
