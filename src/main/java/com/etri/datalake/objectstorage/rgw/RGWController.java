package com.etri.datalake.objectstorage.rgw;

import com.amazonaws.services.s3.model.Bucket;
import com.etri.datalake.auth.GetIdFromToken;
import com.etri.datalake.objectstorage.common.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.twonote.rgwadmin4j.model.Quota;
import org.twonote.rgwadmin4j.model.S3Credential;
import org.twonote.rgwadmin4j.model.SubUser;
import org.twonote.rgwadmin4j.model.User;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Map;


@RestController
@RequiredArgsConstructor
@RequestMapping("/object-storage")
public class RGWController {
    private final RGWService rgwService;
    private final String PF_ADMIN = "/organization/default_org/roles/platform_admin";

    /*
        Permission - Data - List
        버킷 정보를 읽어옴
     */
    @Operation(summary = "bucket 조회", description = "key 값을 읽어 해당 key값의 bucket을 조회합니다", responses = {
            @ApiResponse(responseCode = "200", description = "bucket 조회 성공", content = @Content(mediaType = "application/json", schema = @Schema(implementation = SBucket.class))),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 리소스 접근")})
    @GetMapping("/bucket")
    public ResponseEntity<List<SBucket>> getBuckets(@GetIdFromToken Map<String, Object> userInfo) {

        return ResponseEntity.status(HttpStatus.OK).body(rgwService.getBuckets((S3Credential) userInfo.get("credential")));
    }

    /*
        Permission - Data - Create
     */
    @Operation(summary = "bucket 생성", description = "key 값과 버킷 이름을 입력하여 bucket을 생성합니다", responses = {
            @ApiResponse(responseCode = "200", description = "bucket 생성 성공", content = @Content(mediaType = "application/json", schema = @Schema(implementation = SBucket.class))),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 리소스 접근")})
    @PostMapping("/bucket/{bucketName}")
    public ResponseEntity<Bucket> createBucket(
            @Parameter(name = "key", description = "해당 key 값") @GetIdFromToken Map<String, Object> userInfo,
                                               @Parameter(name = "bucketName", description = "버킷 이름") @PathVariable String bucketName) {
        return ResponseEntity.status(HttpStatus.OK).body(rgwService.createBucket((S3Credential) userInfo.get("credential"), bucketName));
    }

    /*
        Permission - Data - Delete
     */
    @Operation(summary = "bucket 삭제", description = "key값을 확인하여 해당 bucket을 삭제합니다", responses = {
            @ApiResponse(responseCode = "200", description = "bucket 삭제 성공"),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 리소스 접근")})
    @PostMapping("/bucket/{bucketName}/delete")
    public ResponseEntity<?> deleteBucket(@Parameter(name = "key", description = "해당 key 값") @GetIdFromToken Map<String, Object> userInfo,
                             @Parameter(name = "bucketName", description = "버킷 이름") @PathVariable String bucketName) {
        rgwService.deleteBucket((S3Credential) userInfo.get("credential"), bucketName);
        return ResponseEntity.ok().build();
    }

    /*
        Data - List
     */
    @Operation(summary = "Object 조회", description = "key 값과 버킷 이름을 확인하여 해당 Objects를 조회합니다", responses = {
            @ApiResponse(responseCode = "200", description = "Object 조회 성공", content = @Content(mediaType = "application/json", schema = @Schema(implementation = BObject.class))),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 리소스 접근")})
    @GetMapping("/data/{bucketName}")
    public ResponseEntity<List<BObject>> getObjects(@PathVariable String bucketName,
                                                    @GetIdFromToken Map<String, Object> userInfo
    ) {
        return ResponseEntity.status(HttpStatus.OK).body(rgwService.getObjects((S3Credential) userInfo.get("credential"), bucketName));
    }

    /*
        Data - Delete
     */
    @Operation(summary = "Object 삭제", description = "key값과 버킷 이름을 확인하여 해당 Object를 삭제합니다", responses = {
            @ApiResponse(responseCode = "200", description = "Object 삭제 성공"),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 리소스 접근")})
    @PostMapping("/data/{bucketName}/{object}/delete")
    public ResponseEntity<?> deleteObject(@Parameter(name = "key", description = "해당 key 값") @GetIdFromToken Map<String, Object> userInfo,
                             @Parameter(name = "bucketName", description = "버킷 이름") @PathVariable String bucketName,
                             @Parameter(name = "object", description = "해당 object") @PathVariable String object) {
        rgwService.deleteObject((S3Credential) userInfo.get("credential"), bucketName, object);
        return ResponseEntity.ok().build();
    }

    /*
        Data - Create
     */
    @Operation(summary = "object 생성", description = "파일,버킷 이름,접근키,비밀키를 입력하여 오브젝트를 생성합니다", responses = {
            @ApiResponse(responseCode = "200", description = "Object 생성 성공", content = @Content(mediaType = "application/json", schema = @Schema(implementation = BObject.class))),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 리소스 접근")})
    @PostMapping(value = "/data")
    public ResponseEntity<String> objectUpload(@Parameter(name = "file", description = "파일") @RequestPart(value = "file", required = false) MultipartFile file,
                               @Parameter(name = "bucketName", description = "버킷 이름") @RequestParam("bucketName") String bucketName,
                               @Parameter(name = "token", description = "토큰") @GetIdFromToken Map<String, Object> userInfo) throws IOException {
        rgwService.objectUpload(file, bucketName, (S3Credential) userInfo.get("credential"));
        return ResponseEntity.ok(file.getOriginalFilename());
    }

    /*
        Data - Get
     */
    @Operation(summary = "object의 url 다운로드", description = "key 값과 버킷 이름, 오브젝트를 입력하여 해당 오브젝트의 url을 다운로드합니다", responses = {
            @ApiResponse(responseCode = "200", description = "Object의 url 다운로드 성공"),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 리소스 접근")})
    @GetMapping("/data/{bucketName}/{object}")
    public ResponseEntity<URL> objectDownUrl(@Parameter(name = "token", description = "토큰") @GetIdFromToken Map<String, Object> userInfo,
                             @Parameter(name = "bucketName", description = "버킷 이름") @PathVariable String bucketName,
                             @Parameter(name = "object", description = "오브젝트") @PathVariable String object) {
        return ResponseEntity.ok(rgwService.objectDownUrl((S3Credential) userInfo.get("credential"), bucketName, object));
    }

    // TODO: excel 정리된 것 처럼 매핑해야함
    @PostMapping("/permission/acl/bucket/{bucketName}")
    public ResponseEntity<?> addBucketUser(@GetIdFromToken Map<String, Object> userInfo, @RequestBody String rgwUser, @RequestBody String permission, @PathVariable String bucketName) {
        rgwService.addBucketUser((S3Credential) userInfo.get("credential"), rgwUser, permission, bucketName);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "테스트용 api")
    @GetMapping("/bucket/test")
    public void test(@GetIdFromToken Map<String, Object> userInfo) {
    }

    @Operation(summary = "전송속도 제한", description = "API의 과도한 호출을 제한하기 위해 유저의 API 전송속도와 호출수를 제한합니다", responses = {
            @ApiResponse(responseCode = "200", description = "전송속도 제한 성공", content = @Content(mediaType = "application/json", schema = @Schema(implementation = RateLimit.class))),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 리소스 접근")})
    @GetMapping("/permission/quota/user/rate-limit/{uid}")
    public ResponseEntity<?> getUserRateLimit(@Parameter(name = "uid", description = "유저 id") @PathVariable String uid, @GetIdFromToken Map<String, Object> userInfo) {
        if(rgwService.validAccess(userInfo, PF_ADMIN)){
            return ResponseEntity.ok(rgwService.getUserRateLimit(uid));
        }else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    @GetMapping("/permission/quota/users/rate-limit")
    public ResponseEntity<?> setUserRateLimit(@Parameter(name = "uidList", description = "유저 id list") @RequestBody List<String> userList, @GetIdFromToken Map<String, Object> userInfo) {
        if(rgwService.validAccess(userInfo, PF_ADMIN)){
            return ResponseEntity.ok(rgwService.getUserRateLimitList(userList));
        }else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    @PostMapping("/permission/quota/users/rate-limit")
    public ResponseEntity<String> setUserRateLimitList(@RequestBody Map<String, RateLimit> userRateLimits, @GetIdFromToken Map<String, Object> userInfo) {

        if(rgwService.validAccess(userInfo, PF_ADMIN)){
            return ResponseEntity.ok(rgwService.setUserRateLimitList(userRateLimits));
        }else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    @PostMapping("/permission/quota/user/rate-limit/{uid}")
    public ResponseEntity<String> setUserRateLimit(@PathVariable String uid, @RequestBody RateLimit rateLimit, @GetIdFromToken Map<String, Object> userInfo) {

        if(rgwService.validAccess(userInfo, PF_ADMIN)){
            return ResponseEntity.ok(rgwService.setUserRateLimit(uid, rateLimit));
        }else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

    }

    @Operation(summary = "prefix 경로의 폴더 및 파일 리스트 반환", description = "key 값과 버킷 이름, prefix을 입력하여 prefix 경로의 폴더 및 파일 리스트를 반환합니다", responses = {
            @ApiResponse(responseCode = "200", description = "prefix 경로의 폴더 및 파일 리스트 반환 성공"),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 리소스 접근")})
    @PostMapping("/data/{bucketName}/files")
    public ResponseEntity<Map<String, List<?>>> getFileList(@Parameter(name = "token", description = "토큰") @GetIdFromToken Map<String, Object> userInfo,
                                            @Parameter(name = "bucketName", description = "버킷 이름") @PathVariable String bucketName,
                                            @Parameter(name = "prefix", description = "prefix") @RequestParam(required = false) String prefix) {
        return ResponseEntity.ok(rgwService.getFileList((S3Credential) userInfo.get("credential"), bucketName, prefix));
    }

    /*
        Quota 반환 하기
        벼킷 각각의 크기 받아오기
     */
    @Operation(summary = "버킷 크기 조회", description = "버킷 이름을 입력하여 해당 버킷의 크기를 조회합니다", responses = {
            @ApiResponse(responseCode = "200", description = "버킷 크기 조회 성공", content = @Content(mediaType = "application/json", schema = @Schema(implementation = SQuota.class))),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 리소스 접근")})
    @GetMapping("/permission/quota/bucket/size/{bucketName}")
    public ResponseEntity<Map<String, Long>> getIndividualBucketQuota(@Parameter(name = "bucketName", description = "버킷 이름") @PathVariable String bucketName) {
        return ResponseEntity.ok(rgwService.getIndividualBucketQuota(bucketName));
    }

    /*
        버킷 각각의 크기 설정하기
     */
    @Operation(summary = "버킷 크기 설정", description = "유저 id와 버킷 이름, 할당량을 입력하여 버킷의 크기를 설정합니다", responses = {
            @ApiResponse(responseCode = "200", description = "버킷 크기 설정 성공", content = @Content(mediaType = "application/json", schema = @Schema(implementation = SQuota.class))),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 리소스 접근")})
    @PostMapping("/permission/quota/bucket/size/{bucketName}/{uid}")
    public ResponseEntity<SQuota> setIndividualBucketQuota(@Parameter(name = "bucketName", description = "버킷 이름") @PathVariable String bucketName,
                                          @Parameter(name = "uid", description = "유저 id") @PathVariable String uid,
                                          @Parameter(name = "quota", description = "할당량") @RequestBody SQuota quota, @GetIdFromToken Map<String, Object> userInfo) {

        if(rgwService.validAccess(userInfo, PF_ADMIN)){
            return ResponseEntity.ok(rgwService.setIndividualBucketQuota(uid, bucketName, quota));
        }else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }


    /*
        서브 유저 생성
     */
    @Operation(summary = "서브 유저 생성", description = "유저 id를 입력하여 해당 유저의 서브 유저를 생성합니다", responses = {
            @ApiResponse(responseCode = "200", description = "서브 유저 생성 성공", content = @Content(mediaType = "application/json", schema = @Schema(implementation = SSubUser.class))),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 리소스 접근")})
    @PostMapping("/credential/user/{uid}/sub-user")
    public ResponseEntity<List<SubUser>> createSubUser(@Parameter(name = "uid", description = "유저 id") @PathVariable("uid") String uid,
                                                       @Parameter(name = "subUser", description = "서브 유저") @RequestBody SSubUser subUser, @GetIdFromToken Map<String, Object> userInfo) {
        if(rgwService.validAccess(userInfo, PF_ADMIN)){
            return ResponseEntity.ok(rgwService.createSubUser(uid, subUser));
        }else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    /*
        서브 유저의 권한 정보 출력
     */
    @Operation(summary = "서브유저 권한정보 출력", description = "유저 id와 서브유저 id를 입력하여 해당 서브 유저의 권한정보를 출력합니다", responses = {
            @ApiResponse(responseCode = "200", description = "서브유저 권한정보 출력 성공", content = @Content(mediaType = "application/json", schema = @Schema(implementation = SSubUser.class))),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 리소스 접근")})
    @GetMapping("/credential/user/{uid}/sub-user/{subUid}")
    public ResponseEntity<String> subUserInfo(@Parameter(name = "uid", description = "유저 id") @PathVariable("uid") String uid,
                              @Parameter(name = "subUid", description = "서브유저 id") @PathVariable("subUid") String subUid, @GetIdFromToken Map<String, Object> userInfo) {

        if(rgwService.validAccess(userInfo, PF_ADMIN)){
            return ResponseEntity.ok(rgwService.subUserInfo(uid, subUid));
        }else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    /*
        서브 유저의 권한 수정
     */
    @Operation(summary = "서브유저 권한 수정", description = "유저 id와 서브유저 id를 입력하여 해당 서브 유저의 권한을 수정합니다", responses = {
            @ApiResponse(responseCode = "200", description = "서브유저 권한 수정 성공", content = @Content(mediaType = "application/json", schema = @Schema(implementation = SSubUser.class))),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 리소스 접근")})
    @PostMapping("/credential/user/{uid}/sub-user/{subUid}")
    public ResponseEntity<?> setSubUserPermission(@Parameter(name = "uid", description = "유저 id") @PathVariable("uid") String uid,
                                     @Parameter(name = "subUid", description = "서브유저 id") @PathVariable("subUid") String subUid,
                                     @Parameter(name = "permission", description = "권한") @RequestBody String permission,
                                     @GetIdFromToken Map<String, Object> userInfo) {

        if(rgwService.validAccess(userInfo, PF_ADMIN)){
            rgwService.setSubUserPermission(uid, subUid, permission);
            return ResponseEntity.ok("Subuser permission update successfully.");
        }else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    /*
        서브 유저 삭제
     */
    @Operation(summary = "서브 유저 삭제", description = "유저 id와 서브유저 id, key 값을 입력하여 해당 서브유저를 삭제합니다", responses = {
            @ApiResponse(responseCode = "200", description = "서브유저 삭제 성공"),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 리소스 접근")})
    @PostMapping("/credential/user/{uid}/sub-user/{subUid}/delete")
    public ResponseEntity<Object> deleteSubUser(@Parameter(name = "uid", description = "유저 id") @PathVariable("uid") String uid,
                              @Parameter(name = "subUid", description = "서브유저 id") @PathVariable("subUid") String subUid,
                              @Parameter(name = "key", description = "해당 key 값") @RequestBody Key key,
                              @GetIdFromToken Map<String, Object> userInfo) {

        if(rgwService.validAccess(userInfo, PF_ADMIN)){
//            rgwService.deleteSubUser(uid, subUid, key);
            return ResponseEntity.ok("Subuser deleted.");
        }else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    /*
        서브 유저의 엑세스키와 시크릿 키 변경
     */
    @Operation(summary = "서브유저 키 변경", description = "유저 id, 서브유저 id, key 값을 입력하여 서브 유저의 접근키와 비밀키를 변경합니다", responses = {
            @ApiResponse(responseCode = "200", description = "서브유저 키 변경 성공", content = @Content(mediaType = "application/json", schema = @Schema(implementation = SSubUser.class))),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 리소스 접근")})
    @PostMapping("/credential/user/{uid}/sub-user/{subUid}/key")
    public ResponseEntity<?> alterSubUserKey(@Parameter(name = "uid", description = "유저 id") @PathVariable("uid") String uid,
                                @Parameter(name = "subUid", description = "서브유저 id") @PathVariable("subUid") String subUid,
                                @Parameter(name = "key", description = "해당 key 값") @RequestBody Key key,
                                @GetIdFromToken Map<String, Object> userInfo) {

        if(rgwService.validAccess(userInfo, PF_ADMIN)){
            rgwService.alterSubUserKey(uid, subUid, key);
            return ResponseEntity.ok("Subuser permission set successfully.");
        }else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    /*
       Credential - List
       uid를 파라미터로 받아 S3Credential list를 반환하는 api
     */
    @Operation(summary = "S3Credential 리스트 반환", description = "유저 id를 입력하여 S3Credential list를 반환합니다", responses = {
            @ApiResponse(responseCode = "200", description = "S3Credential 리스트 반환 성공", content = @Content(mediaType = "application/json", schema = @Schema(implementation = S3Credential.class))),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 리소스 접근")})
    @GetMapping("/credential/user/{uid}")
    public ResponseEntity<?> getCredential(@Parameter(name = "uid", description = "유저 id")
                                                @PathVariable String uid,
                                            @GetIdFromToken Map<String, Object> userInfo) {

        if(rgwService.validAccess(userInfo, PF_ADMIN)){
            return ResponseEntity.ok(rgwService.getS3CredentialList(uid));
        }else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    /*
        Credential - Create
        uid를 파라미터로 받아 S3Credential을 생성하는 api
     */
    @Operation(summary = "S3Credential 생성", description = "유저 id를 입력하여 S3Credential을 생성합니다", responses = {
            @ApiResponse(responseCode = "200", description = "S3Credential 생성 성공", content = @Content(mediaType = "application/json", schema = @Schema(implementation = S3Credential.class))),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 리소스 접근")})
    @PostMapping("/credential/user/{uid}")
    public ResponseEntity<List<S3Credential>> createCredential(@Parameter(name = "uid", description = "유저 id") @PathVariable String uid,
                                               @GetIdFromToken Map<String, Object> userInfo) {

        if(rgwService.validAccess(userInfo, PF_ADMIN)){
            return ResponseEntity.ok(rgwService.createS3Credential(uid));
        }else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    // TODO: 자신의 subuser만 제어 가능하도록 valid access key 함수 넣어야 하는지?
    /*
        Credential - Delete
        uid와 key를 받아 S3Credential을 삭제하는 api
     */
    @Operation(summary = "S3Credential 삭제", description = "유저 id와 key 값을 입력하여 S3Credential을 삭제합니다", responses = {
            @ApiResponse(responseCode = "200", description = "S3Credential 리스트 삭제 성공"),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 리소스 접근")})
    @PostMapping("/credential/user/delete")
    public ResponseEntity<?> deleteCredential(@GetIdFromToken Map<String, Object> userInfo) {
        if(rgwService.validAccess(userInfo, PF_ADMIN)){
            rgwService.deleteS3Credential(uid, key.getAccessKey());
            return ResponseEntity.ok("Subuser permission set successfully.");
        }else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    @GetMapping("/credential/user/sub-user/{uid}")
    public ResponseEntity<Map<String, String>> subUserList(@PathVariable String uid) {
        return ResponseEntity.ok(rgwService.subUserList(uid));
    }

    @PostMapping("/credential/user")
    public ResponseEntity<User> createUser(@RequestBody SUser user,
                           @GetIdFromToken Map<String, Object> userInfo) {

        if(rgwService.validAccess(userInfo, PF_ADMIN)){
            return ResponseEntity.ok(rgwService.createUser(user));
        }else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    /*
        버킷 사용도 %로 계산하여 출력
    */
    @Operation(summary = "버킷 사용도 출력", description = "버킷 이름을 입력하여 해당 버킷의 사용도를 %로 출력합니다", responses = {
            @ApiResponse(responseCode = "200", description = "버킷 사용도 출력 성공"),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 리소스 접근")})
    @GetMapping("/monitoring/{bucketName}")
    public ResponseEntity<Double> quotaUtilizationInfo(@Parameter(name = "bucketName", description = "버킷 이름") @PathVariable String bucketName) {
        return ResponseEntity.ok(rgwService.quotaUtilizationInfo(bucketName));
    }

    @GetMapping("/quota/user/size")
    public ResponseEntity<Map<String, Map<String, Quota>>> usersQuotaList(@GetIdFromToken Map<String, Object> userInfo){
        if(rgwService.validAccess(userInfo, PF_ADMIN)){
            return ResponseEntity.ok(rgwService.usersQuota());
        }else{
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    @GetMapping("/quota/user/rate-limit")
    public ResponseEntity<Map<String, Map<String, String>>> usersRateLimit(@GetIdFromToken Map<String, Object> userInfo){
        if(rgwService.validAccess(userInfo, PF_ADMIN)){
            return ResponseEntity.ok(rgwService.usersRateLimit());
        }else{
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    @GetMapping("/quota/bucket/size")
    public ResponseEntity<Map<String, Map<String, Quota>>> bucketsQuotaList(@GetIdFromToken Map<String, Object> userInfo){
        if(rgwService.validAccess(userInfo, PF_ADMIN)){
            return ResponseEntity.ok(rgwService.bucketsQuota());
        }else{
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    @PostMapping("/quota/bucket/size/{uid}")
    public ResponseEntity<Quota> bucketsQuota(@PathVariable String uid){
        return ResponseEntity.ok(rgwService.bucketsQuota(uid));
    }

    @GetMapping("/monitoring")
    public ResponseEntity<Map<String, Double>> quotaUtilizationList(@GetIdFromToken Map<String, Object> userInfo) {
        return ResponseEntity.ok(rgwService.quotaUtilizationList((S3Credential) userInfo.get("credential")));
    }
}