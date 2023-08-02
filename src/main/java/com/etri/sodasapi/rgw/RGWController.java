package com.etri.sodasapi.rgw;

import com.amazonaws.services.s3.model.Bucket;
import com.etri.sodasapi.common.BObject;
import com.etri.sodasapi.common.Key;
import com.etri.sodasapi.common.Quota;
import com.etri.sodasapi.common.SBucket;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.twonote.rgwadmin4j.model.S3Credential;

import java.io.IOException;
import java.net.URL;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/object-storage")
public class RGWController {
    private final RGWService rgwService;

    /*
        Permission - Data - List
     */
    @Operation(summary = "bucket 조회", description = "key 값을 확인하여 해당 bucket을 조회합니다")
    @GetMapping("/bucket")
    public ResponseEntity<List<SBucket>> getBuckets(@Parameter(name = "key", description = "해당 key 값") @RequestBody Key key){
        if(rgwService.validAccess(key)){
            return ResponseEntity.status(HttpStatus.OK).body(rgwService.getBuckets(key));
        }
        else{
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ArrayList<>());
        }
    }

    /*
        Permission - Data - Create
     */
    @Operation(summary = "bucket 생성", description = "key 값과 bucket 값을 주어 bucket을 생성합니다")
    @PostMapping("/bucket/{bucketName}")
    public ResponseEntity<Bucket> createBucket(@Parameter(name = "key", description = "해당 key 값") @RequestBody Key key,
                                               @Parameter(name = "bucketName", description = "해당 bucketName") @PathVariable String bucketName){
        if(rgwService.validAccess(key)){
            return ResponseEntity.status(HttpStatus.OK).body(rgwService.createBucket(key, bucketName));
        }
        else{
            return ResponseEntity.status(HttpStatus.OK).body(rgwService.createBucket(key, bucketName));
        }
    }

    /*
        Permission - Data - Delete
     */
    @Operation(summary = "bucket 삭제", description = "key값을 확인하여 해당 bucket을 삭제합니다")
    @DeleteMapping("/bucket/{bucketName}")
    public void deleteBucket(@Parameter(name = "key", description = "해당 key 값") @RequestBody Key key,
                             @Parameter(name = "bucketName", description = "해당 bucketName") @PathVariable String bucketName){
        if(rgwService.validAccess(key)){
            rgwService.deleteBucket(key, bucketName);
        }
        else{
            rgwService.deleteBucket(key, bucketName);
        }
    }

    /*
        Data - List
     */
    @Operation(summary = "Object 조회", description = "key 값과 bucketName을 확인하여 해당 Objects를 조회합니다")
    @GetMapping("/bucket/{bucketName}")
    public ResponseEntity<List<BObject>> getObjects(@Parameter(name = "key", description = "해당 key 값") @RequestBody Key key,
                                                    @Parameter(name = "bucketName", description = "해당 bucketName") @PathVariable String bucketName)
            throws NoSuchAlgorithmException, InvalidKeyException {
        if(rgwService.validAccess(key)){
            return ResponseEntity.status(HttpStatus.OK).body(rgwService.getObjects(key, bucketName));
        }
        else{
            return ResponseEntity.status(HttpStatus.OK).body(rgwService.getObjects(key, bucketName));
        }
    }

    /*
        Data - Delete
     */
    @Operation(summary = "Object 삭제", description = "key값과 bucketName을 확인하여 해당 Object를 삭제합니다")
    @DeleteMapping("/bucket/{bucketName}/{object}")
    public void deleteObject(@Parameter(name = "key", description = "해당 key 값") @RequestBody Key key,
                             @Parameter(name = "bucketName", description = "해당 bucketName") @PathVariable String bucketName,
                             @Parameter(name = "object", description = "해당 object") @PathVariable String object){
        if(rgwService.validAccess(key)){
            rgwService.deleteObject(key, bucketName, object);
        }
        else{
            rgwService.deleteObject(key, bucketName, object);
        }
    }

    /*
        Data - Create
     */
    @Operation(summary = "object 업로드")
    @PostMapping("/bucket/object")
    public String objectUpload(@RequestParam("file") MultipartFile file, @RequestParam("bucketName") String bucketName,
                               @RequestParam("accessKey") String accessKey, @RequestParam("secretKey") String secretKey) throws IOException {
        Key key = new Key(accessKey, secretKey);

        rgwService.objectUpload(file, bucketName, key);

        return file.getOriginalFilename();
    }

    /*
        Data - Get
     */
    @Operation(summary = "object 의 url 다운로드")
    @GetMapping("/bucket/{bucketName}/{object}")
    public URL objectDownUrl(@RequestBody Key key, @PathVariable String bucketName, @PathVariable String object){
        return rgwService.objectDownUrl(key, bucketName, object);
    }

    @Operation(summary = "테스트용 api")
    @GetMapping("/bucket/test")
    public List<S3Credential> test() {
        return rgwService.getS3Credential("foo_user");
        //rgwService.getBucketInfo("foo-test-bucket");
        //rgwService.setIndividualBucketQuota(
        //        "foo_user",
        //        "foo-test-bucket",
        //        new Quota("true", "3", "1000", "bucket"));
    }

    /*
        Quota 반환 하기
     */
    @GetMapping("/bucket/quota/{bucketName}")
    public long getIndividualBucketQuota(@PathVariable String bucketName){
        return rgwService.getIndividualBucketQuota(bucketName);
    }

    @PostMapping("/bucket/quota/{bucketName}/{uid}")
    public Quota setIndividualBucketQuota(@PathVariable String bucketName, @PathVariable String uid, @RequestBody Quota quota){
        return rgwService.setIndividualBucketQuota(uid, bucketName, quota);
    }

    @GetMapping("/bucket/quota/{bucketName}/utilization")
    public Double quotaUtilizationInfo(@PathVariable String bucketName){
        return rgwService.quotaUtilizationInfo(bucketName);
    }

    @GetMapping("/credential")
    public List<S3Credential> getCredential(@PathVariable String uid) {
        return rgwService.getS3Credential(uid);
    }

    @PostMapping("/credential")
    public void createCredential(@PathVariable String uid){
        rgwService.createS3Credential(uid);
    }

    // TODO: 자신의 subuser만 제어 가능하도록 valid access key 함수 넣어야 하는지?
    @DeleteMapping("/credential")
    public void deleteCredential(@PathVariable String uid, @PathVariable Key key){
        rgwService.deleteS3Credential(uid, key.getAccessKey());
    }
}
