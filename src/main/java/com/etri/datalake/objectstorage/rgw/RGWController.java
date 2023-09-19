package com.etri.datalake.objectstorage.rgw;

import com.amazonaws.services.s3.model.Bucket;
import com.etri.datalake.auth.GetIdFromToken;
import com.etri.datalake.objectstorage.constants.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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
@Tag(name = "RGW Controller", description = "RGW 컨트롤러 API 문서입니다")
@RestController
@RequiredArgsConstructor
@RequestMapping("/datalake/object-storage")
public class RGWController {
    private final RGWService rgwService;
    private final String PF_ADMIN = "/organization/default_org/roles/platform_admin";

    /*
        Permission - Data - List
        버킷 정보를 읽어옴
     */
    @Operation(summary = "bucket 조회", description = "유저의 버킷을 조회합니다", responses = {
            @ApiResponse(responseCode = "200", description = "버킷 조회 성공", content = @Content(mediaType = "application/json", schema = @Schema(implementation = SBucket.class))),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 리소스 접근")})
    @GetMapping("/bucket/list")
    public ResponseEntity<List<SBucket>> getBuckets(@GetIdFromToken Map<String, Object> userInfo) {

        return ResponseEntity.status(HttpStatus.OK).body(rgwService.getBuckets((S3Credential) userInfo.get("credential")));
    }

    /*
        Permission - Data - Create
     */
    @Operation(summary = "bucket 생성", description = "버킷 이름을 주어 버킷을 생성합니다", responses = {
            @ApiResponse(responseCode = "200", description = "버킷 생성 성공", content = @Content(mediaType = "application/json", schema = @Schema(implementation = SBucket.class))),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 리소스 접근")})
    @PostMapping("/bucket/{bucketName}/create")
    public ResponseEntity<Bucket> createBucket(@GetIdFromToken Map<String, Object> userInfo,
                                               @Parameter(name = "bucketName", description = "버킷 이름") @PathVariable String bucketName) {
        return ResponseEntity.status(HttpStatus.OK).body(rgwService.createBucket((S3Credential) userInfo.get("credential"), bucketName));
    }

    /*
        Permission - Data - Delete
     */
    @Operation(summary = "bucket 삭제", description = "버킷 이름을 확인하여 해당 버킷을 삭제합니다", responses = {
            @ApiResponse(responseCode = "200", description = "버킷 삭제 성공"),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 리소스 접근")})
    @PostMapping("/bucket/{bucketName}/delete")
    public ResponseEntity<?> deleteBucket(@GetIdFromToken Map<String, Object> userInfo,
                             @Parameter(name = "bucketName", description = "버킷 이름") @PathVariable String bucketName) {
        rgwService.deleteBucket((S3Credential) userInfo.get("credential"), bucketName);
        return ResponseEntity.ok().build();
    }

    /*
        Data - List
     */
    @Operation(summary = "object 조회", description = "버킷 이름을 확인하여 해당 오브젝트를 조회합니다", responses = {
            @ApiResponse(responseCode = "200", description = "오브젝트 조회 성공", content = @Content(mediaType = "application/json", schema = @Schema(implementation = BObject.class))),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 리소스 접근")})
    @GetMapping("/data/{bucketName}/list")
    public ResponseEntity<List<BObject>> getObjects(@Parameter(name = "bucketName", description = "버킷 이름") @PathVariable String bucketName,
                                                    @GetIdFromToken Map<String, Object> userInfo
    ) {
        return ResponseEntity.status(HttpStatus.OK).body(rgwService.getObjects((S3Credential) userInfo.get("credential"), bucketName));
    }

    /*
        Data - Delete
     */
    @Operation(summary = "object 삭제", description = "버킷 이름을 확인하여 해당 오브젝트를 삭제합니다", responses = {
            @ApiResponse(responseCode = "200", description = "오브젝트 삭제 성공"),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 리소스 접근")})
    @PostMapping("/data/{bucketName}/{objectKey}/delete")
    public ResponseEntity<?> deleteObject(@GetIdFromToken Map<String, Object> userInfo,
                             @Parameter(name = "bucketName", description = "버킷 이름") @PathVariable String bucketName,
                             @Parameter(name = "objectKey", description = "오브젝트 이름") @PathVariable String objectKey) {
        rgwService.deleteObject((S3Credential) userInfo.get("credential"), bucketName, objectKey);
        return ResponseEntity.ok().build();
    }

    /*
        Data - Create
     */
    @Operation(summary = "object 생성", description = "파일,버킷 이름, 오브젝트 키를 입력하여 오브젝트를 생성합니다", responses = {
            @ApiResponse(responseCode = "200", description = "오브젝트 생성 성공", content = @Content(mediaType = "application/json", schema = @Schema(implementation = BObject.class))),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 리소스 접근")})
    @PostMapping(value = "/data/create", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> objectUpload(@Parameter(name = "file", description = "파일") @RequestPart(value = "file", required = false) MultipartFile file,
                                               @Parameter(name = "bucketName", description = "버킷 이름") @RequestParam(value = "bucketName") String bucketName,
                                               @Parameter(name = "objectKey", description = "object의 key") @RequestParam(value = "objectKey", required = false) String objectKey,
                                               @GetIdFromToken Map<String, Object> userInfo) throws IOException {
        rgwService.objectUpload(file, bucketName, (S3Credential) userInfo.get("credential"), objectKey);
        return ResponseEntity.ok(file.getOriginalFilename());
    }

    /*
        Data - Get
     */
    @Operation(summary = "object의 다운로드 url 생성", description = "버킷 이름, 오브젝트를 입력하여 해당 오브젝트의 다운로드 url을 생성합니다.", responses = {
            @ApiResponse(responseCode = "200", description = "오브젝트의 url 다운로드 성공"),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 리소스 접근")})
    @GetMapping("/data/{bucketName}/{object}/get")
    public ResponseEntity<URL> objectDownUrl(@GetIdFromToken Map<String, Object> userInfo,
                                             @Parameter(name = "bucketName", description = "버킷 이름") @PathVariable String bucketName,
                                             @Parameter(name = "object", description = "오브젝트") @PathVariable String object) {
        return ResponseEntity.ok(rgwService.objectDownUrl((S3Credential) userInfo.get("credential"), bucketName, object));
    }

    @Operation(summary = "버킷 사용자 추가", description = "버킷 이름, 권한, 유저를 입력하여 해당 유저에게 사용자 권한을 부여합니다.(FullControl, Read, Write, ReadAcp, WriteAcp)")
    @PostMapping("/permission/acl/bucket/{bucketName}")
    public ResponseEntity<?> addBucketUser(@GetIdFromToken Map<String, Object> userInfo,
                                           @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "유저") @RequestBody SUserPerm sUserPerm,
                                           @Parameter(name = "bucketName", description = "버킷 이름") @PathVariable String bucketName) {
        rgwService.addBucketUser((S3Credential) userInfo.get("credential"), sUserPerm.getUserId(), sUserPerm.getPermission(), bucketName);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "전송 속도 조회", description = "유저의 API ratelimit(전송 속도와 호출 수)을 조회합니다", responses = {
            @ApiResponse(responseCode = "200", description = "전송 속도 조회 성공", content = @Content(mediaType = "application/json", schema = @Schema(implementation = RateLimit.class))),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 리소스 접근")})
    @GetMapping("/permission/quota/user/rate-limit/{uid}/list")
    public ResponseEntity<?> getUserRateLimit(@Parameter(name = "uid", description = "유저 아이디") @PathVariable String uid,
                                              @GetIdFromToken Map<String, Object> userInfo) {
        if (rgwService.validAccess(userInfo, PF_ADMIN)) {
            return ResponseEntity.ok(rgwService.getUserRateLimit(uid));
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    @Operation(summary = "여러 유저의 전송 속도 조회", description = "사용자 이름의 배열을 입력받아 전송 속도 배열을 반환합니다.", responses = {
            @ApiResponse(responseCode = "200", description = "전송 속도 배열 조회 성공", content = @Content(mediaType = "application/json", schema = @Schema(implementation = RateLimit.class))),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 리소스 접근")})
    @GetMapping("/permission/quota/users/rate-limit/list")
    public ResponseEntity<?> getUserRateLimitList(@Parameter(name = "uidList", description = "유저 아이디 리스트") @RequestBody List<String> userList,
                                                  @GetIdFromToken Map<String, Object> userInfo) {
        System.out.println(userList.toString());
        if(rgwService.validAccess(userInfo, PF_ADMIN)){
            return ResponseEntity.ok(rgwService.getUserRateLimitList(userList));
        }else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    @Operation(summary = "여러 유저의 전송 속도 설정", description = "사용자 이름의 배열을 입력받아 다수의 전송 속도를 설정합니다.", responses = {
            @ApiResponse(responseCode = "200", description = "전송 속도 설정 성공", content = @Content(mediaType = "application/json", schema = @Schema(implementation = RateLimit.class))),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 리소스 접근")})
    @PostMapping("/permission/quota/users/rate-limit/update")
    public ResponseEntity<String> setUserRateLimitList(@RequestBody Map<String, RateLimit> userRateLimits, @GetIdFromToken Map<String, Object> userInfo) {

        if(rgwService.validAccess(userInfo, PF_ADMIN)){
            return ResponseEntity.ok(rgwService.setUserRateLimitList(userRateLimits));
        }else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    @Operation(summary = "전송 속도 설정", description = "유저의 전송 속도를 설정합니다", responses = {
            @ApiResponse(responseCode = "200", description = "전송 속도 설정 성공"),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 리소스 접근")})
    @PostMapping("/permission/quota/user/rate-limit/{uid}/update")
    public ResponseEntity<String> setUserRateLimit(@Parameter(name = "uid", description = "유저 아이디") @PathVariable String uid,
                                                   @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "제한 속도") @RequestBody RateLimit rateLimit,
                                                   @GetIdFromToken Map<String, Object> userInfo) {
        if (rgwService.validAccess(userInfo, PF_ADMIN)) {
            return ResponseEntity.ok(rgwService.setUserRateLimit(uid, rateLimit));
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

    }

    @Operation(summary = "prefix 경로의 폴더 및 파일 리스트 반환", description = "버킷 이름, prefix을 입력하여 prefix 경로의 폴더 및 파일 리스트를 반환합니다", responses = {
            @ApiResponse(responseCode = "200", description = "prefix 경로의 폴더 및 파일 리스트 반환 성공"),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 리소스 접근")})
    @PostMapping("/data/{bucketName}/files/get")
    public ResponseEntity<Map<String, List<?>>> getFileList(@GetIdFromToken Map<String, Object> userInfo,
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
    @GetMapping("/permission/quota/bucket/size/{bucketName}/get")
    public ResponseEntity<Map<String, Long>> getIndividualBucketQuota(@Parameter(name = "bucketName", description = "버킷 이름") @PathVariable String bucketName) {
        return ResponseEntity.ok(rgwService.getIndividualBucketQuota(bucketName));
    }

    /*
        버킷 각각의 크기 설정하기
     */
    @Operation(summary = "버킷 크기 설정", description = "유저 아이디와 버킷 이름, 할당량을 입력하여 버킷의 크기를 설정합니다", responses = {
            @ApiResponse(responseCode = "200", description = "버킷 크기 설정 성공", content = @Content(mediaType = "application/json", schema = @Schema(implementation = SQuota.class))),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 리소스 접근")})
    @PostMapping("/permission/quota/bucket/size/{bucketName}/{uid}/update")
    public ResponseEntity<SQuota> setIndividualBucketQuota(@Parameter(name = "bucketName", description = "버킷 이름") @PathVariable String bucketName,
                                                           @Parameter(name = "uid", description = "유저 아이디") @PathVariable String uid,
                                                           @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "할당량") @RequestBody SQuota quota,
                                                           @GetIdFromToken Map<String, Object> userInfo) {

        if (rgwService.validAccess(userInfo, PF_ADMIN)) {
            return ResponseEntity.ok(rgwService.setIndividualBucketQuota(uid, bucketName, quota));
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }


    /*
        서브 유저 생성
     */
    @Operation(summary = "서브 유저 생성", description = "유저 아이디를 입력하여 해당 유저의 서브 유저를 생성합니다", responses = {
            @ApiResponse(responseCode = "200", description = "서브 유저 생성 성공", content = @Content(mediaType = "application/json", schema = @Schema(implementation = SSubUser.class))),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 리소스 접근")})
    @PostMapping("/user/credential/sub-user/create")
    public ResponseEntity<List<SubUser>> createSubUser(@io.swagger.v3.oas.annotations.parameters.RequestBody(description = "서브 유저") @RequestBody SSubUser subUser,
                                                       @GetIdFromToken Map<String, Object> userInfo) {
            return ResponseEntity.ok(rgwService.createSubUser((String)userInfo.get("userId"), subUser));
    }

    /*
        서브 유저의 권한 정보 출력
     */
    @Operation(summary = "서브유저 권한정보 출력", description = "유저 아이디와 서브유저 아이디를 입력하여 해당 서브 유저의 권한정보를 출력합니다", responses = {
            @ApiResponse(responseCode = "200", description = "서브유저 권한정보 출력 성공", content = @Content(mediaType = "application/json", schema = @Schema(implementation = SSubUser.class))),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 리소스 접근")})
    @GetMapping("/user/credential/sub-user/{subUid}/get")
    public ResponseEntity<String> subUserInfo(@Parameter(name = "subUid", description = "서브유저 아이디") @PathVariable("subUid") String subUid,
                                              @GetIdFromToken Map<String, Object> userInfo) {

        if (rgwService.validAccess(userInfo, PF_ADMIN)) {
            return ResponseEntity.ok(rgwService.subUserInfo((String)userInfo.get("userId"), subUid));
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    /*
        서브 유저의 권한 수정
     */
    @Operation(summary = "서브유저 권한 수정", description = "유저 아이디와 서브유저 아이디를 입력하여 해당 서브 유저의 권한을 수정합니다.(Read, Write, read-write, pull)", responses = {
            @ApiResponse(responseCode = "200", description = "서브유저 권한 수정 성공", content = @Content(mediaType = "application/json", schema = @Schema(implementation = SSubUser.class))),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 리소스 접근")})
    @PostMapping("/user/credential/sub-user/{subUid}/update")
    public ResponseEntity<?> setSubUserPermission(@Parameter(name = "subUid", description = "서브유저 아이디") @PathVariable("subUid") String subUid,
                                                  @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "권한") @RequestBody String permission,
                                                  @GetIdFromToken Map<String, Object> userInfo) {
            rgwService.setSubUserPermission((String)userInfo.get("userId"), subUid, permission);
            return ResponseEntity.ok("Subuser permission update successfully.");
    }

    /*
        서브 유저 삭제
     */
    @Operation(summary = "서브 유저 삭제", description = "유저 아이디와 서브유저 아이디을 입력하여 해당 서브유저를 삭제합니다", responses = {
            @ApiResponse(responseCode = "200", description = "서브유저 삭제 성공"),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 리소스 접근")})
    @PostMapping("/user/credential/sub-user/{subUid}/delete")
    public ResponseEntity<Object> deleteSubUser(@Parameter(name = "subUid", description = "서브유저 아이디") @PathVariable("subUid") String subUid,
                              @Parameter(name = "key", description = "해당 키 값") @RequestBody Key key,
                              @GetIdFromToken Map<String, Object> userInfo) {
            rgwService.deleteSubUser((String)userInfo.get("userId"), subUid, key);
            return ResponseEntity.ok("Subuser deleted.");
    }

    /*
        서브 유저의 엑세스키와 시크릿 키 변경
     */
    @Operation(summary = "서브유저 키 변경", description = "유저 아이디, 서브유저 아이디, 키 값을 입력하여 서브 유저의 비밀키를 변경합니다", responses = {
            @ApiResponse(responseCode = "200", description = "서브유저 키 변경 성공", content = @Content(mediaType = "application/json", schema = @Schema(implementation = SSubUser.class))),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 리소스 접근")})
    @PostMapping("/user/credential/sub-user/{subUid}/key/update")
    public ResponseEntity<?> alterSubUserKey(@Parameter(name = "subUid", description = "서브유저 아이디") @PathVariable("subUid") String subUid,
                                             @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "해당 키 값") @RequestBody Key key,
                                             @GetIdFromToken Map<String, Object> userInfo) {

        rgwService.alterSubUserKey((String)userInfo.get("userId"), subUid, key);
        return ResponseEntity.ok("Subuser key change successfully.");
    }

    /*
       Credential - List
       uid를 파라미터로 받아 S3Credential list를 반환하는 api
     */
    @Operation(summary = "S3Credential 리스트 반환", description = "유저 아이디를 입력하여 S3Credential list를 반환합니다", responses = {
            @ApiResponse(responseCode = "200", description = "S3Credential 리스트 반환 성공", content = @Content(mediaType = "application/json", schema = @Schema(implementation = S3Credential.class))),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 리소스 접근")})
    @GetMapping("/user/credential/{uid}/list")
    public ResponseEntity<?> getCredential(@Parameter(name = "uid", description = "유저 아이디") @PathVariable String uid,
                                           @GetIdFromToken Map<String, Object> userInfo) {

        if (rgwService.validAccess(userInfo, PF_ADMIN)) {
            return ResponseEntity.ok(rgwService.getS3CredentialList(uid));
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    /*
        Credential - Create
        uid를 파라미터로 받아 S3Credential을 생성하는 api
     */
    @Operation(summary = "S3Credential 생성", description = "유저 아이디를 입력하여 S3Credential을 생성합니다", responses = {
            @ApiResponse(responseCode = "200", description = "S3Credential 생성 성공", content = @Content(mediaType = "application/json", schema = @Schema(implementation = S3Credential.class))),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 리소스 접근")})
    @PostMapping("/user/credential/{uid}/create")
    public ResponseEntity<List<S3Credential>> createCredential(@Parameter(name = "uid", description = "유저 아이디") @PathVariable String uid,
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
    @Operation(summary = "S3Credential 리스트 삭제", description = "유저 아이디를 입력하여 S3Credential list를 삭제합니다", responses = {
            @ApiResponse(responseCode = "200", description = "S3Credential 리스트 삭제 성공"),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 리소스 접근")})
    @PostMapping("/user/credential/delete")
    public ResponseEntity<?> deleteCredential(@GetIdFromToken Map<String, Object> userInfo) {
        if(rgwService.validAccess(userInfo, PF_ADMIN)){
//            rgwService.deleteS3Credential(uid, key.getAccessKey());
            return ResponseEntity.ok("Delete credential successfully.");
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    @Operation(summary = "서브유저 리스트 출력", description = "유저 아이디를 입력하여 해당 유저의 서브유저 리스트를 출력합니다", responses = {
            @ApiResponse(responseCode = "200", description = "서브 유저 리스트 출력 성공"),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 리소스 접근")})
    @GetMapping("/user/credential/sub-user/list")
    public ResponseEntity<Map<String, String>> subUserList(@Parameter(name = "uid", description = "유저 아이디")@GetIdFromToken Map<String, Object> userInfo) {
            return ResponseEntity.ok(rgwService.subUserList((String) userInfo.get("userId")));
    }

    @Operation(summary = "유저 삭제", description = "유저를 삭제합니다", responses = {
            @ApiResponse(responseCode = "200", description = "유저 삭제 성공"),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 리소스 접근")})
    @PostMapping("/user/delete")
    public ResponseEntity<Map<String, String>> deleteUser(@GetIdFromToken Map<String, Object> userInfo, @RequestBody String userId){
        if(rgwService.validAccess(userInfo, PF_ADMIN)){
            return ResponseEntity.ok(rgwService.deleteUser(userId));
        }else{
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    @Operation(summary = "유저 생성", description = "유저를 생성합니다")
    @PostMapping("/user/credential/create")
    public ResponseEntity<User> createUser(@io.swagger.v3.oas.annotations.parameters.RequestBody(description = "유저") @RequestBody SUser user,
                                           @GetIdFromToken Map<String, Object> userInfo) {
        if (rgwService.validAccess(userInfo, PF_ADMIN)) {
            return ResponseEntity.ok(rgwService.createUser(user));
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    /*
        버킷 사용도 %로 계산하여 출력
    */
    @Operation(summary = "버킷 사용도 출력", description = "버킷 이름을 입력하여 해당 버킷의 사용도를 %로 출력합니다", responses = {
            @ApiResponse(responseCode = "200", description = "버킷 사용도 출력 성공"),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 리소스 접근")})
    @GetMapping("/monitoring/{bucketName}/get")
    public ResponseEntity<Map<String, String>> quotaUtilizationInfo(@Parameter(name = "bucketName", description = "버킷 이름") @PathVariable String bucketName) {
        return ResponseEntity.ok(rgwService.quotaUtilizationInfo(bucketName));
    }

    @Operation(summary = "유저 쿼타 리스트 출력", description = "유저의 쿼타 리스트를 출력합니다", responses = {
            @ApiResponse(responseCode = "200", description = "쿼타 리스트 출력 성공"),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 리소스 접근")})
    @GetMapping("/permission/quota/user/size/list")
    public ResponseEntity<Map<String, Map<String, Quota>>> usersQuotaList(@GetIdFromToken Map<String, Object> userInfo){
        if(rgwService.validAccess(userInfo, PF_ADMIN)){
            return ResponseEntity.ok(rgwService.usersQuota());
        }else{
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    @Operation(summary = "모든 유저 전송 속도 출력", description = "모든 유저의 전송 속도를 출력합니다", responses = {
            @ApiResponse(responseCode = "200", description = "전송 속도 출력 성공"),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 리소스 접근")})
    @GetMapping("/permission/quota/user/rate-limit/list")
    public ResponseEntity<Map<String, Map<String, String>>> usersRateLimit(@GetIdFromToken Map<String, Object> userInfo){
        if(rgwService.validAccess(userInfo, PF_ADMIN)){
            return ResponseEntity.ok(rgwService.usersRateLimit());
        }else{
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    @Operation(summary = "모든 유저 버킷 쿼타 출력", description = "모든 유저의 버킷 쿼타를 출력합니다", responses = {
            @ApiResponse(responseCode = "200", description = "버킷 쿼타 출력 성공"),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 리소스 접근")})
    @GetMapping("/permission/quota/bucket/size/list")
    public ResponseEntity<Map<String, Map<String, Quota>>> bucketsQuotaList(@GetIdFromToken Map<String, Object> userInfo){
        if(rgwService.validAccess(userInfo, PF_ADMIN)){
            return ResponseEntity.ok(rgwService.bucketsQuota());
        }else{
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    @Operation(summary = "유저의 버킷 쿼타 출력", description = "해당 토큰 유저의 버킷 쿼타를 출력합니다", responses = {
            @ApiResponse(responseCode = "200", description = "버킷 쿼타 출력 성공"),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 리소스 접근")})
    @PostMapping("/permission/quota/bucket/size/{uid}/get")
    public ResponseEntity<Quota> bucketsQuota(@PathVariable String uid){
        return ResponseEntity.ok(rgwService.bucketsQuota(uid));
    }

    @Operation(summary = "유저의 모든 버킷 사용도 출력", description = "해당 토큰 유저의 모든 버킷 사용도를 출력합니다", responses = {
            @ApiResponse(responseCode = "200", description = "버킷 사용도 출력 성공"),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 리소스 접근")})
    @GetMapping("/monitoring/list")
    public ResponseEntity<Map<String, String>> quotaUtilizationList(@GetIdFromToken Map<String, Object> userInfo) {
        return ResponseEntity.ok(rgwService.quotaUtilizationList((S3Credential) userInfo.get("credential")));
    }
}