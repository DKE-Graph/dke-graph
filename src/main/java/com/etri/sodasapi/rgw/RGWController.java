package com.etri.sodasapi.rgw;

import com.amazonaws.services.s3.model.Bucket;
import com.etri.sodasapi.common.BObject;
import com.etri.sodasapi.common.Key;
import com.etri.sodasapi.common.SBucket;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URL;
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
    @GetMapping("/bucket")
    public ResponseEntity<List<SBucket>> getBuckets(@RequestBody Key key){
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
    @PostMapping("/bucket/{bucketName}")
    public ResponseEntity<Bucket> createBucket(@RequestBody Key key, @PathVariable String bucketName){
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
    @GetMapping("/bucket/{bucketName}")
    public ResponseEntity<List<BObject>> getObjects(@RequestBody Key key, @PathVariable String bucketName){
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
}
