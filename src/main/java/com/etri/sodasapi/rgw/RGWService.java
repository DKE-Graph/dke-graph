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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
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

            AccessControlList accessControlList = conn.getBucketAcl(mybucket.getName());
            // 기존 Grant를 가져올 Canonical ID 또는 AWS 계정 ID
            String existingCanonicalId = "foo_user";

// 기존 Grant 찾기
            Grantee existingGrant = null;
            for (Grant grant : accessControlList.getGrants()) {
                if (grant.getGrantee() instanceof CanonicalGrantee) {
                    String canonicalId = ((CanonicalGrantee) grant.getGrantee()).getIdentifier();
                    if (existingCanonicalId.equals(canonicalId)) {
                        existingGrant = grant.getGrantee();
                        break;
                    }
                }
            }

            if (existingGrant != null) {
                // 기존 Grant 삭제
                accessControlList.revokeAllPermissions(existingGrant);

                // 변경할 새로운 Grant 생성
                String newCanonicalId = "foo_user"; // 새로운 Canonical ID 또는 AWS 계정 ID를 지정합니다.
                Grantee newGrant = new CanonicalGrantee("foo_user");

                // 새로운 Grant 추가
                accessControlList.grantPermission(newGrant, Permission.Read);
                accessControlList.grantPermission(newGrant, Permission.Write);

                // 수정된 ACL을 버킷에 설정
                conn.setBucketAcl("foo-test-bucket", accessControlList);
                System.out.println(mybucket.getName() + " " + conn.getBucketAcl(mybucket.getName()));
            }
//            List<Grant> grants = new ArrayList<>();
//            grants.add(new Grant(new CanonicalGrantee("foo_user"), Permission.Read));
//
//            accessControlList = (AccessControlList) grants;
//
//            conn.setBucketAcl(mybucket.getName(), accessControlList);
//
//
//            System.out.println(mybucket.getName() + " " + conn.getBucketAcl(mybucket.getName()));
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
                System.out.println(objectSummary.getKey() + " " + conn.getObjectAcl(bucketName, objectSummary.getKey()));
            }
            objects = conn.listNextBatchOfObjects(objects);
        } while (objects.isTruncated());
        return objectList;
    }

    public Bucket createBucket(Key key, String bucketName) {
        AmazonS3 conn = getClient(key);
        Bucket newBucket = conn.createBucket(bucketName);
        return newBucket;
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
}
