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
import com.etri.sodasapi.config.Constants;
import lombok.RequiredArgsConstructor;
import org.apache.tomcat.util.bcel.Const;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.twonote.rgwadmin4j.RgwAdmin;
import org.twonote.rgwadmin4j.RgwAdminBuilder;
import org.twonote.rgwadmin4j.model.UsageInfo;
import org.twonote.rgwadmin4j.model.usage.Summary;
import org.yaml.snakeyaml.scanner.Constant;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.*;

@Service
@RequiredArgsConstructor
public class RGWService {
    private final Constants constants;


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
                .withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(constants.getRgwEndpoint(), Regions.DEFAULT_REGION.getName()))
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



        rgwAdmin();


        System.out.println("hello");

        String accessKey = constants.getRgwAdminAccess();
        String secretKey = constants.getRgwAdminSecret();
        String bucket = "signature";
        String endpoint = constants.getRgwEndpoint();
        String verb = "GET";
        String path = "/admin/bucket";
        String expiryMinutes = "10";

        String url = getSignedUrl(verb, path, accessKey, secretKey, bucket, endpoint, expiryMinutes);
        System.out.println(url);





    }

    public void setBucketQuota(String bucket, String maxSizeKb, String enabled){
        try{
            final String uid = constants.getRgwAdminUID();

            Map<String, String> queryParameters = new HashMap<>();
            queryParameters.put("uid", uid);
            queryParameters.put("bucket", bucket);
            queryParameters.put("max-size-kb", maxSizeKb);
            queryParameters.put("enabled", enabled);

            StringBuilder sb = new StringBuilder();

            for (Map.Entry<String, String> entry : queryParameters.entrySet()){
                if(sb.length()>0){
                    sb.append("&");
                }
                sb.append(URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8.toString()));
                sb.append("=");
                sb.append(URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8.toString()));
            }


            String query = "uid=sodas_dev_user&bucket&max-size-kb=5000&enabled=true";
            String encodeQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);
            System.out.println(encodeQuery);
        }catch (Exception ex){
            ex.printStackTrace();
        }
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

    public void rgwAdmin(){
        RgwAdmin rgwAdmin = new RgwAdminBuilder().accessKey("sodas_dev_access")
                .secretKey("sodas_dev_secret")
                .endpoint("http://object-storage.rook.221.154.134.31.traefik.me:10017/admin")
                .build();

        rgwAdmin.setBucketQuota("sodas_dev_user", 1000, 4998);
        //System.out.println(rgwAdmin.getBucketQuota("sodas_dev_user").toString());

        rgwAdmin.setIndividualBucketQuota("sodas_dev_user", "foo-test-bucket", 10, 5999);
        rgwAdmin.getBucketInfo("foo-test-bucket").stream().peek(System.out::println);

        UsageInfo userUsage = rgwAdmin.getUserUsage("sodas_dev_user").get();
        List<Summary> hoho = userUsage.getSummary();
        System.out.println(hoho);
    }
}
