package com.etri.sodasapi.rgw;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.*;
import com.etri.sodasapi.common.BObject;
import com.etri.sodasapi.common.Key;
import com.etri.sodasapi.common.SBucket;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RGWService {

    @Value("${RGW_ENDPOINT}")
    private String RGW_ENDPOINT;

    public List<SBucket> getBuckets(Key key) {
        AmazonS3 conn = getClient(key);

        List<Bucket> buckets = conn.listBuckets();
        List<SBucket> bucketList = new ArrayList<>();

        for (Bucket mybucket : buckets) {
            bucketList.add(new SBucket(mybucket.getName(), mybucket.getCreationDate()));
            System.out.println(mybucket.getName() + " " + conn.getBucketAcl(mybucket.getName()));
        }

        return bucketList;
    }

    public List<BObject> getObjects(Key key, String bucketName) {
        AmazonS3 conn = getClient(key);

        ObjectListing objects = conn.listObjects(bucketName);

//        System.out.println(objects);
        List<BObject> objectList = new ArrayList<>();

        do {
            for (S3ObjectSummary objectSummary : objects.getObjectSummaries()) {
                objectList.add(new BObject(objectSummary.getKey(), objectSummary.getSize(), objectSummary.getLastModified()));
            }
            objects = conn.listNextBatchOfObjects(objects);
        } while (objects.isTruncated());

        return objectList;
    }

    public Bucket createBucket(Key key, String bucketName) {
        AmazonS3 conn = getClient(key);

        return conn.createBucket(bucketName);
    }

    public void deleteBucket(Key key, String bucketName) {
        AmazonS3 conn = getClient(key);

        List<BObject> objectList = getObjects(key, bucketName);

        for(BObject bObject : objectList) {
            conn.deleteObject(bucketName, bObject.getObjectName());
        }

        conn.deleteBucket(bucketName);
    }

    public void deleteObject(Key key, String bucketName, String object) {
        AmazonS3 conn = getClient(key);

        conn.deleteObject(bucketName, object);
    }

    private synchronized AmazonS3 getClient(Key key){
        AmazonS3 amazonS3;

        String accessKey = key.getAccessKey();
        String secretKey = key.getSecretKey();

        AWSCredentials awsCredentials = new BasicAWSCredentials(accessKey, secretKey);
        ClientConfiguration clientConfiguration = new ClientConfiguration();
        return amazonS3 = AmazonS3ClientBuilder
                .standard()
                .withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(RGW_ENDPOINT, Regions.DEFAULT_REGION.getName()))
                .withPathStyleAccessEnabled(true)
                .withClientConfiguration(clientConfiguration)
                .withCredentials(new AWSStaticCredentialsProvider(awsCredentials))
                .build();
    }

    public void objectUpload(MultipartFile file, String bucketName, Key key) throws IOException {
        AmazonS3 conn = getClient(key);

        ByteArrayInputStream input = new ByteArrayInputStream(file.getBytes());
        conn.putObject(bucketName, file.getOriginalFilename(), input, new ObjectMetadata());
    }

    // TODO: 2023.7.22 Keycloak과 연동해 관리자 확인하는 코드 추가해야 함.
    public boolean validAccess(Key key) {
        return true;
    }

    public URL objectDownUrl(Key key, String bucketName, String object) {
        AmazonS3 conn = getClient(key);

        GeneratePresignedUrlRequest request = new GeneratePresignedUrlRequest(bucketName, object);

        System.out.println(conn.generatePresignedUrl(request));
        return conn.generatePresignedUrl(request);
    }

    public void getBucketQuota(Key key, String bucketName) throws NoSuchAlgorithmException, InvalidKeyException {
        AmazonS3 conn = getClient(key);

        String accessKey = "sodas_dev_access";
        String secretKey = "sodas_dev_secret";
        String bucket = "signature";
        String endpoint = "http://object-storage.rook.221.154.134.31.traefik.me:10017";
        String verb = "GET";
        String path = "/admin/bucket";
        String expiryMinutes = "10";

        String url = getSignedUrl(verb, path, accessKey, secretKey, bucket, endpoint, expiryMinutes);
        System.out.println(url);

        AmazonS3 s3Client = AmazonS3ClientBuilder.standard().build();

        java.util.Date expiration = new java.util.Date();
        long expTimeMillis = expiration.getTime();
        expTimeMillis += 1000 * 60 * 60;
        expiration.setTime(expTimeMillis);

        GeneratePresignedUrlRequest generatePresignedUrlRequest =
                new GeneratePresignedUrlRequest("hello");

        GeneratePresignedUrlRequest generatePresignedUrlRequest =
                new GeneratePresignedUrlRequest(bucketName, );

        generatePresignedUrlRequest

    }

    public String getSignedUrl(String verb, String path, String accessKey, String secretKey, String bucket, String endpoint, String expiryMinutes) throws NoSuchAlgorithmException, InvalidKeyException, NoSuchAlgorithmException, InvalidKeyException {

        long expiryEpoch = (System.currentTimeMillis() / 1000) + (Integer.parseInt(expiryMinutes) * 60);

        String canonicalizedResource = "/admin/user";
        String stringToSign = verb + "\n\n\n" + expiryEpoch + "\n" + canonicalizedResource;

        Mac mac = Mac.getInstance("HmacSHA1");
        mac.init(new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), "HmacSHA1"));
        byte[] signatureBytes = mac.doFinal(stringToSign.getBytes(StandardCharsets.UTF_8));
        String signature = URLEncoder.encode(Base64.getEncoder().encodeToString(signatureBytes), StandardCharsets.UTF_8);

        return endpoint + canonicalizedResource + "?AWSAccessKeyId=" + accessKey + "&uid=sodas_dev_user&quota&quota-type=bucket" + "&Expires=" + expiryEpoch + "&Signature=" + signature;
    }
}
