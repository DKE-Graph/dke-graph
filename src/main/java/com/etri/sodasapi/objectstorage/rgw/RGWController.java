package com.etri.sodasapi.objectstorage.rgw;

import com.amazonaws.services.s3.model.Bucket;
import com.etri.sodasapi.objectstorage.common.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/object-storage")
public class RGWController {
    private final RGWService rgwService;

    /*
        Permission - Data - List
        버킷 정보를 읽어옴
     */
    @GetMapping("/bucket")
    public ResponseEntity<List<SBucket>> getBuckets(Key key){
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

    // TODO: 2023-08-03T18:25:19.870+09:00  WARN 50273 --- [nio-8080-exec-7] c.amazonaws.services.s3.AmazonS3Client   : No content length specified for stream data.  Stream contents will be buffered in memory and could result in out of memory errors.
    // TODO: 2023-08-03T18:25:20.944+09:00  WARN 50273 --- [nio-8080-exec-7] com.amazonaws.util.Base64                : JAXB is unavailable. Will fallback to SDK implementation which may be less performant.If you are using Java 9+, you will need to include javax.xml.bind:jaxb-api as a dependency.
    // TODO: 파일 업로드할 때 이런 오류 발생
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
    public void test() {
        Key key = new Key("MB9VKP4AC9TZPV1UDEO4", "UYScnoXxLtmAemx4gAPjByZmbDnaYuOPOdpG7vMw");
        String bucketName = "foo-test-bucket";
        String prefix = "test";

        rgwService.getFileList(key, bucketName, prefix);
    }

    @GetMapping("/bucket/quota/rate-limit/{uid}")
    public String getUserRateLimit(@PathVariable String uid){
        return rgwService.getUserRatelimit(uid);
    }

    @PostMapping("/bucket/quota/rate-limit/{uid}")
    public String setUserRateLimit(@PathVariable String uid, @RequestBody RateLimit rateLimit){
        return rgwService.setUserRateLimit(uid, rateLimit);
    }

    @Operation(summary = "prefix 경로의 폴더 및 파일 리스트 반환")
    @PostMapping("/bucket/{bucketName}/files")
    public Map<String, List<?>> getFileList(@RequestBody Key key, @PathVariable String bucketName, @RequestParam(required = false) String prefix){
        return rgwService.getFileList(key, bucketName, prefix);
    }

    /*
        Quota 반환 하기
        벼킷 각각의 크기 받아오기
     */
    @GetMapping("/bucket/quota/{bucketName}")
    public Map<String, Long> getIndividualBucketQuota(@PathVariable String bucketName){
        return rgwService.getIndividualBucketQuota(bucketName);
    }

    /*
        버킷 각각의 크기 설정하기
     */
    @PostMapping("/bucket/quota/{bucketName}/{uid}")
    public Quota setIndividualBucketQuota(@PathVariable String bucketName, @PathVariable String uid, @RequestBody Quota quota){
        return rgwService.setIndividualBucketQuota(uid, bucketName, quota);
    }

    /*
        버킷 사용도 %로 계산하여 출력
     */
    @GetMapping("/bucket/quota/{bucketName}/utilization")
    public Double quotaUtilizationInfo(@PathVariable String bucketName){
        return rgwService.quotaUtilizationInfo(bucketName);
    }

    /*
        서브 유저 생성
     */
    @PostMapping("/bucket/subuser/{uid}")
    public void createSubUser(@PathVariable("uid") String uid, @RequestBody SSubUser subUser){
        rgwService.createSubUser(uid, subUser);
    }

    /*
        서브 유저의 권한 정보 출력
     */
    @GetMapping("/bucket/subuser/{uid}/{subUid}")
    public String subUserInfo(@PathVariable("uid") String uid, @PathVariable("subUid") String subUid){
        return rgwService.subUserInfo(uid, subUid);
    }

    /*
        서브 유저의 권한 수정
     */
    @PostMapping("/bucket/subuser/{uid}/{subUid}")
    public void setSubUserPermission(@PathVariable("uid") String uid, @PathVariable("subUid") String subUid, @RequestBody String permission){
        rgwService.setSubUserPermission(uid, subUid, permission);
    }

    /*
        서브 유저 삭제
     */
    @DeleteMapping("/bucket/subuser/{uid}/{subUid}")
    public void deleteSubUser(@PathVariable("uid") String uid, @PathVariable("subUid") String subUid, @RequestBody Key key){
        rgwService.deleteSubUser(uid, subUid, key);
    }

    /*
        서브 유저의 엑세스키와 시크릿 키 변경
     */
    @PostMapping("/bucket/subuser/{uid}/{subUid}/key")
    public void alterSubUserKey(@PathVariable("uid") String uid, @PathVariable("subUid") String subUid, @RequestBody Key key) {
        rgwService.alterSubUserKey(uid, subUid, key);
    }

    /*
       Credential - List
       uid를 파라미터로 받아 S3Credential list를 반환하는 api
     */
    @GetMapping("/credential/{uid}")
    public List<S3Credential> getCredential(@PathVariable String uid) {
        return rgwService.getS3Credential(uid);
    }

    /*
        Credential - Create
        uid를 파라미터로 받아 S3Credential을 생성하는 api
     */
    @PostMapping("/credential/{uid}")
    public void createCredential(@PathVariable String uid){
        rgwService.createS3Credential(uid);
    }

    // TODO: 자신의 subuser만 제어 가능하도록 valid access key 함수 넣어야 하는지?
    /*
        Credential - Delete
        uid와 key를 받아 S3Credential을 삭제하는 api
     */
    @DeleteMapping("/credential")
    public void deleteCredential(@PathVariable String uid, @PathVariable Key key){
        rgwService.deleteS3Credential(uid, key.getAccessKey());
    }
}
