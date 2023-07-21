package com.etri.sodasapi.rgw;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.Protocol;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.Bucket;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.etri.sodasapi.common.BObject;
import com.etri.sodasapi.common.Key;
import com.etri.sodasapi.common.SBucket;
import com.etri.sodasapi.config.Constants;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RGWService {

    @Value("${MGR_ENDPOINT}")
    public String MGR_ENDPOINT;

    @Value("${RGW_ENDPOINT}")
    public String RGW_ENDPOINT;

    public List<SBucket> getBuckets(Key key) {
        String accessKey = key.getAccessKey();
        String secretKey = key.getSecretKey();

        AWSCredentials credentials = new BasicAWSCredentials(accessKey, secretKey);

        ClientConfiguration clientConfig = new ClientConfiguration();
        clientConfig.setProtocol(Protocol.HTTP);

        AmazonS3 conn = new AmazonS3Client(credentials, clientConfig);

        conn.setEndpoint(RGW_ENDPOINT);

        List<Bucket> buckets = conn.listBuckets();
        List<SBucket> bucketList = new ArrayList<>();

        for (Bucket mybucket : buckets) {
            bucketList.add(new SBucket(mybucket.getName(), mybucket.getCreationDate()));
        }

        return bucketList;
    }

    public boolean validAccess(Key key) {
        return true;
    }

    public List<BObject> getObjects(Key key, String bucketName) {
        String accessKey = key.getAccessKey();
        String secretKey = key.getSecretKey();
        AWSCredentials credentials = new BasicAWSCredentials(accessKey, secretKey);

        ClientConfiguration clientConfig = new ClientConfiguration();
        clientConfig.setProtocol(Protocol.HTTP);

        AmazonS3 conn = new AmazonS3Client(credentials, clientConfig);
        conn.setEndpoint(RGW_ENDPOINT);

        ObjectListing objects = conn.listObjects("test-bucket");

//        System.out.println(objects);
        List<BObject> objectList = new ArrayList<>();
//
//        do {
//            for (S3ObjectSummary objectSummary : objects.getObjectSummaries()) {
//                objectList.add(new BObject(objectSummary.getKey(), objectSummary.getSize(), objectSummary.getLastModified()));
//            }
//            objects = conn.listNextBatchOfObjects(objects);
//        } while (objects.isTruncated());

        return objectList;
    }

    public Bucket createBucket(Key key, String bucketName) {
        String accessKey = key.getAccessKey();
        String secretKey = key.getSecretKey();

        AWSCredentials credentials = new BasicAWSCredentials(accessKey, secretKey);

        ClientConfiguration clientConfig = new ClientConfiguration();
        clientConfig.setProtocol(Protocol.HTTP);

        AmazonS3 conn = new AmazonS3Client(credentials, clientConfig);

        conn.setEndpoint(RGW_ENDPOINT);

        return conn.createBucket(bucketName);
    }

    public void deleteBucket(Key key, String bucketName) {
        String accessKey = key.getAccessKey();
        String secretKey = key.getSecretKey();
        AWSCredentials credentials = new BasicAWSCredentials(accessKey, secretKey);

        ClientConfiguration clientConfig = new ClientConfiguration();
        clientConfig.setProtocol(Protocol.HTTP);

        AmazonS3 conn = new AmazonS3Client(credentials, clientConfig);
        conn.setEndpoint(RGW_ENDPOINT);

        System.out.println(bucketName);

        conn.deleteBucket(bucketName);
    }
}
