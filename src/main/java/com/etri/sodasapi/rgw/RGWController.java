package com.etri.sodasapi.rgw;

import com.amazonaws.services.s3.model.Bucket;
import com.etri.sodasapi.common.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.checkerframework.framework.qual.RequiresQualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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
    @Operation(summary = "bucket 생성", description = "key값과 bucket값을 주어 bucket을 생성합니다")
    @PostMapping("/bucket/{bucketName}")
    public ResponseEntity<Bucket> createBucket(@Parameter(name = "key", description = "해당 key 값")@RequestBody Key key,@Parameter(name = "bucketName", description = "해당 bucketName") @PathVariable String bucketName){
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
    public void deleteBucket(@RequestBody Key key, @PathVariable String bucketName){
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
    public ResponseEntity<List<BObject>> getObjects(@Parameter(name = "key", description = "해당 key값") @RequestBody Key key,
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
    @DeleteMapping("/bucket/{bucketName}/{object}")
    public void deleteObject(@RequestBody Key key, @PathVariable String bucketName, @PathVariable String object){
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
    @PostMapping("/bucket/object")
    public String objectUpload(@RequestParam("file") MultipartFile file, @RequestParam("bucketName") String bucketName, @RequestParam("accessKey") String accessKey, @RequestParam("secretKey") String secretKey) throws IOException {
        Key key = new Key(accessKey, secretKey);

        rgwService.objectUpload(file, bucketName, key);

        return file.getOriginalFilename();
    }

    /*
        Data - Get
     */
    @GetMapping("/bucket/{bucketName}/{object}")
    public URL objectDownUrl(@RequestBody Key key, @PathVariable String bucketName, @PathVariable String object){
        return rgwService.objectDownUrl(key, bucketName, object);
    }

    /*
        Quota 반환 하기
     */
    @GetMapping("/bucket/quota/{bucketName}")
    public Map<String, Long> getIndividualBucketQuota(@PathVariable String bucketName){
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

    @PostMapping("/bucket/subuser/{uid}")
    public void createSubUser(@PathVariable("uid") String uid, @RequestBody SSubUser subUser){
        rgwService.createSubUser(uid, subUser);
    }

    @GetMapping("/bucket/subuser/{uid}/{subUid}")
    public String subUserInfo(@PathVariable("uid") String uid, @PathVariable("subUid") String subUid){
        return rgwService.subUserInfo(uid, subUid);
    }

    @PostMapping("/bucket/subuser/{uid}/{subUid}")
    public void setSubUserPermission(@PathVariable("uid") String uid, @PathVariable("subUid") String subUid, @RequestBody String permission){
        rgwService.setSubUserPermission(uid, subUid, permission);
    }

    @DeleteMapping("/bucket/subuser/{uid}/{subUid}")
    public void deleteSubUser(@PathVariable("uid") String uid, @PathVariable("subUid") String subUid, @RequestBody Key key){
        rgwService.deleteSubUser(uid, subUid, key);
    }

    @PostMapping("/bucket/subuser/{uid}/{subUid}/key")
    public void alterSubUserKey(@PathVariable("uid") String uid, @PathVariable("subUid") String subUid, @RequestBody Key key){
        rgwService.alterSubUserKey(uid, subUid, key);
    }
}
