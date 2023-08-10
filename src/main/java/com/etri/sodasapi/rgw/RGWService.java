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
import com.etri.sodasapi.common.*;
import com.etri.sodasapi.common.Quota;
import com.etri.sodasapi.config.Constants;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.twonote.rgwadmin4j.RgwAdmin;
import org.twonote.rgwadmin4j.RgwAdminBuilder;
import org.twonote.rgwadmin4j.model.*;
import software.amazon.awssdk.core.exception.SdkClientException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RGWService {
    private final Constants constants;
    private RgwAdmin rgwAdmin;
    private SodasRgwAdmin sodasRgwAdmin;

    private synchronized RgwAdmin getRgwAdmin() {
        if (this.rgwAdmin == null) {
            rgwAdmin = new RgwAdminBuilder().accessKey(constants.getRgwAdminAccess())
                    .secretKey(constants.getRgwAdminSecret())
                    .endpoint(constants.getRgwEndpoint() + "/admin")
                    .build();
        }
        return rgwAdmin;
    }

    private SodasRgwAdmin getSodasRgwAdmin(){
        if(this.sodasRgwAdmin == null){
            sodasRgwAdmin = new SodasRgwAdmin(constants);
        }
        return sodasRgwAdmin;
    }


    public List<SBucket> getBuckets(Key key) {
        AmazonS3 conn = getClient(key);
        List<Bucket> buckets = conn.listBuckets();
        List<SBucket> bucketList = new ArrayList<>();

        for (Bucket mybucket : buckets) {
            bucketList.add(new SBucket(mybucket.getName(), mybucket.getCreationDate()));
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

//        System.out.println(mybucket.getName() + " " + conn.getBucketAcl(mybucket.getName()));
//
//        AccessControlList accessControlList = conn.getBucketAcl(mybucket.getName());
//        // 기존 Grant를 가져올 Canonical ID 또는 AWS 계정 ID
//        String existingCanonicalId = "foo_user";
//
//// 기존 Grant 찾기
//        Grantee existingGrant = null;
//        for (Grant grant : accessControlList.getGrants()) {
//            if (grant.getGrantee() instanceof CanonicalGrantee) {
//                String canonicalId = ((CanonicalGrantee) grant.getGrantee()).getIdentifier();
//                if (existingCanonicalId.equals(canonicalId)) {
//                    existingGrant = grant.getGrantee();
//                    break;
//                }
//            }
//        }
//
//        if (existingGrant != null) {
//            // 기존 Grant 삭제
//            accessControlList.revokeAllPermissions(existingGrant);
//
//            // 변경할 새로운 Grant 생성
//            String newCanonicalId = "foo_user"; // 새로운 Canonical ID 또는 AWS 계정 ID를 지정합니다.
//            Grantee newGrant = new CanonicalGrantee("foo_user");
//
//            // 새로운 Grant 추가
//            accessControlList.grantPermission(newGrant, Permission.Read);
//            accessControlList.grantPermission(newGrant, Permission.Write);
//
//            // 수정된 ACL을 버킷에 설정
//            conn.setBucketAcl("foo-test-bucket", accessControlList);
//            System.out.println(mybucket.getName() + " " + conn.getBucketAcl(mybucket.getName()));

    }

    public void deleteBucket(Key key, String bucketName) {
        AmazonS3 conn = getClient(key);

        List<BObject> objectList = getObjects(key, bucketName);

        for (BObject bObject : objectList) {
            conn.deleteObject(bucketName, bObject.getObjectName());
        }

        conn.deleteBucket(bucketName);
    }

    public void deleteObject(Key key, String bucketName, String object) {
        AmazonS3 conn = getClient(key);

        conn.deleteObject(bucketName, object);
    }

    private synchronized AmazonS3 getClient(Key key) {
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
        System.out.println(conn.putObject(bucketName, file.getOriginalFilename(), input, new ObjectMetadata()));
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

    public Map<String, Long> getIndividualBucketQuota(String bucketName) {
        RgwAdmin rgwAdmin = getRgwAdmin();

        Optional<BucketInfo> bucketInfo = rgwAdmin.getBucketInfo(bucketName);
        BucketInfo bucketInfo1 = bucketInfo.get();

        Map<String, Long> individualBucketQuota = new HashMap<>();

        individualBucketQuota.put("max-size-kb", bucketInfo1.getBucketQuota().getMaxSizeKb());
        individualBucketQuota.put("max-objects", bucketInfo1.getBucketQuota().getMaxObjects());

        return individualBucketQuota;
    }

    public void getBucketInfo(String bucketName){
        RgwAdmin rgwAdmin = getRgwAdmin();

        long usage =  rgwAdmin.getBucketInfo(bucketName).get().getUsage().getRgwMain().getSize();

        System.out.println(usage);
    }


    public Quota setIndividualBucketQuota(String uid, String bucketName, Quota quota) {
        RgwAdmin rgwAdmin = getRgwAdmin();

        rgwAdmin.setIndividualBucketQuota(uid, bucketName, Long.parseLong(quota.getMax_objects()), Long.parseLong(quota.getMax_size_kb()));

        return quota;
    }

    public Double quotaUtilizationInfo(String bucketName) {
        RgwAdmin rgwAdmin = getRgwAdmin();

        Optional<BucketInfo> bucketInfo = rgwAdmin.getBucketInfo(bucketName);
        BucketInfo bucketInfo1 = bucketInfo.get();
        return (((double) bucketInfo1.getUsage().getRgwMain().getSize_actual() / (bucketInfo1.getBucketQuota().getMaxSizeKb() * 1024)) * 100);
    }

    public List<SubUser> createSubUser(String uid, SSubUser subUser) {
        RgwAdmin rgwAdmin = getRgwAdmin();
        Map<String, String> subUserParam = new HashMap<>();
        subUserParam.put("access-key", subUser.getAccessKey());
        subUserParam.put("secret-key", subUser.getSecretKey());
        subUserParam.put("key-type", "s3");
        subUserParam.put("access", SubUser.Permission.NONE.toString());
        return rgwAdmin.createSubUser(uid, subUser.getSubUid(), subUserParam);
    }

    public String subUserInfo(String uid, String subUid) {
        RgwAdmin rgwAdmin = getRgwAdmin();
        Optional<SubUser> optionalSubUser = rgwAdmin.getSubUserInfo(uid, subUid);

        SubUser subUser = optionalSubUser.get();

        return subUser.getPermission().toString();
    }

    public void setSubUserPermission(String uid, String subUid, String permission) {
        RgwAdmin rgwAdmin = getRgwAdmin();

        rgwAdmin.setSubUserPermission(uid, subUid, SubUser.Permission.valueOf(permission.toUpperCase()));

    }

    public void deleteSubUser(String uid, String subUid, Key key) {
        RgwAdmin rgwAdmin = getRgwAdmin();
        rgwAdmin.removeS3CredentialFromSubUser(uid, subUid, key.getAccessKey());
        rgwAdmin.removeSubUser(uid, subUid);
    }

    public void alterSubUserKey(String uid, String subUid, Key key) {
        RgwAdmin rgwAdmin = getRgwAdmin();
        rgwAdmin.createS3CredentialForSubUser(uid, subUid, key.getAccessKey(), key.getSecretKey());
    }

    // nodejs 코드에서 입력 파라미터로 uid만을 받게 설계돼 있어서 우리도 key 빼야할지 고민해봐야함.
    public void createS3Credential(String uid, Key key){
        RgwAdmin rgwAdmin = getRgwAdmin();

        rgwAdmin.createS3Credential(uid, key.getAccessKey(), key.getSecretKey());
    }

    public void createS3Credential(String uid){
        RgwAdmin rgwAdmin = getRgwAdmin();

        rgwAdmin.createS3Credential(uid);
    }

    public void deleteS3Credential(String uid, String accessKey){
        RgwAdmin rgwAdmin = getRgwAdmin();

        rgwAdmin.removeS3Credential(uid, accessKey);
    }

    public List<S3Credential> getS3Credential(String uid){
        RgwAdmin rgwAdmin = getRgwAdmin();
        Optional<User> userInfo = rgwAdmin.getUserInfo(uid);

        return userInfo.map(User::getS3Credentials).orElse(null);
    }

    public String getUserRatelimit(String uid){
        SodasRgwAdmin sodasRgwAdmin = getSodasRgwAdmin();

        return sodasRgwAdmin.getUserRateLimit(uid);
    }

    public String setUserRatelilmit(String uid, RateLimit rateLimit){
        SodasRgwAdmin sodasRgwAdmin = getSodasRgwAdmin();


        return sodasRgwAdmin.setUserRateLimit(uid, rateLimit);
    }


    public Map<String, List<?>> getFileList(Key key, String bucketName, String prefix){

        String actualPrefix = (prefix != null) ? prefix : "";
        final AmazonS3 s3 = getClient(key);

        try {
            ListObjectsRequest listObjectsRequest = new ListObjectsRequest()
                    .withBucketName(bucketName)
                    .withDelimiter("/")
                    .withPrefix(actualPrefix);

            ObjectListing objectListing = s3.listObjects(listObjectsRequest);

            // 현재 디렉토리의 폴더만 가져옴
            List<String> folderList = objectListing.getCommonPrefixes()
                    .stream()
                    .filter(commonPrefix -> commonPrefix.startsWith(actualPrefix))
                    .collect(Collectors.toList());

            // 현재 디렉토리의 파일만 가져옴
            List<S3ObjectSummary> fileList = objectListing.getObjectSummaries()
                    .stream()
                    .filter(objectSummary -> objectSummary.getKey().startsWith(actualPrefix) && !folderList.contains(objectSummary.getKey() + "/"))
                    .collect(Collectors.toList());

            Map<String, List<?>> result = new HashMap<>();
            result.put("folders", folderList);
            result.put("files", fileList);

            return result;

        } catch (AmazonS3Exception | SdkClientException e) {
            e.printStackTrace();
        }

        return null;
    }
}