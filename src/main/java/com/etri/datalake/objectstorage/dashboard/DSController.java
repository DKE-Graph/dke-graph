package com.etri.datalake.objectstorage.dashboard;

import com.etri.datalake.auth.GetIdFromToken;
import com.etri.datalake.objectstorage.constants.SQuota;
import com.etri.datalake.objectstorage.rgw.RGWService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
@Tag(name = "DS Controller", description = "DS 컨트롤러 API 문서입니다")
@RestController
@RequiredArgsConstructor
@RequestMapping("/datalake/object-storage")
public class DSController {
    private final DSService dsService;
    private final RGWService rgwService;
    private final String PF_ADMIN = "/organization/default_org/roles/platform_admin";

    /*
        TODO: 2023.7.24 Keycloak과 연동해 관리자 확인하는 코드 추가해야 함.
        Permission-Quota-Get
        유저의 쿼타 정보 출력
     */
    @Operation(summary = "유저 쿼타 정보 출력", description = "유저 id를 입력하여 유저의 쿼타 정보를 출력합니다", responses = {
            @ApiResponse(responseCode = "200", description = "유저 쿼타 정보 출력 성공", content = @Content(mediaType = "application/json",schema = @Schema(implementation = SQuota.class))),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 리소스 접근")})
    @GetMapping("/quota/user/size/{uid}")
    public ResponseEntity<List<HashMap>> userQuotaInfo(@Parameter(name = "uid", description = "유저 id") @PathVariable("uid") String userName, @GetIdFromToken Map<String, Object> userInfo){
        if(rgwService.validAccess(userInfo, PF_ADMIN)){
            return ResponseEntity.status(HttpStatus.OK).body(dsService.userQoutaInfo(userName));
        }else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    @Operation(summary = "유저 버킷 쿼타 정보 출력", description = "유저 id를 입력하여 유저의 버킷 쿼타 정보를 출력합니다", responses = {
            @ApiResponse(responseCode = "200", description = "유저 버킷 쿼타 정보 출력 성공", content = @Content(mediaType = "application/json",schema = @Schema(implementation = SQuota.class))),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 리소스 접근")})
    @GetMapping("/quota/bucket/size/{uid}")
    public ResponseEntity<List<HashMap>> bucketQuotaInfo(@Parameter(name = "uid", description = "유저 id") @PathVariable("uid") String userName, @GetIdFromToken Map<String, Object> userInfo){
        if(rgwService.validAccess(userInfo, PF_ADMIN)){
            return ResponseEntity.status(HttpStatus.OK).body(dsService.bucketQoutaInfo(userName));
        }else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    /*
        Permission-Quota-Create, Update
        유저 쿼타 설정
     */
    @Operation(summary = "유저 쿼타 설정", description = "유저 id와 쿼타를 입력하여 유저의 쿼타를 설정합니다", responses = {
            @ApiResponse(responseCode = "200", description = "유저 쿼타 설정 성공", content = @Content(mediaType = "application/json",schema = @Schema(implementation = SQuota.class))),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 리소스 접근")})
    @PostMapping("/permission/quota/user/{uid}/size/update")
    public ResponseEntity userQuotaConfig(@Parameter(name = "uid", description = "유저 id")@PathVariable("uid") String userName,
                                          @RequestBody SQuota quota, @GetIdFromToken Map<String, Object> userInfo){

        if(rgwService.validAccess(userInfo, PF_ADMIN)){
            quota.setQuota_type("user");
            dsService.qoutaConfig(userName, quota);
            return ResponseEntity.status(HttpStatus.OK).body("UserQuota configuration success!");
        }else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

    }

    @Operation(summary = "버킷 쿼타 설정", description = "유저 id와 쿼타를 입력하여 버킷의 쿼타를 설정합니다", responses = {
            @ApiResponse(responseCode = "200", description = "버킷 쿼타 설정 성공", content = @Content(mediaType = "application/json",schema = @Schema(implementation = SQuota.class))),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 리소스 접근")})
    @PostMapping("/permission/quota/bucket/{uid}/size/update")
    public ResponseEntity bucketQuotaConfig(@Parameter(name = "uid", description = "유저 id")@PathVariable("uid") String userName,
                                          @RequestBody SQuota quota, @GetIdFromToken Map<String, Object> userInfo){

        if(rgwService.validAccess(userInfo, PF_ADMIN)){
            quota.setQuota_type("bucket");
            dsService.qoutaConfig(userName, quota);
            return ResponseEntity.status(HttpStatus.OK).body("BucketQuota configuration success!");
        }else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.status(HttpStatus.OK).build();
    }

    /*
        Permission-Quota-Delete
        쿼타 사용 금지
     */
    @Operation(summary = "유저 쿼타 사용 금지 설정", description = "유저 id를 입력하여 유저의 쿼타의 사용을 금지합니다", responses = {
            @ApiResponse(responseCode = "200", description = "유저 쿼타 사용 금지 설정 성공"),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 리소스 접근")})
    @PostMapping("/permission/quota/user/{uid}/size/remove")
    public ResponseEntity<String> userQuotaDisable(@Parameter(name = "uid", description = "유저 id")@PathVariable("uid") String userName,
                                 @RequestBody Map<String, String> body, @GetIdFromToken Map<String, Object> userInfo){
        if(rgwService.validAccess(userInfo, PF_ADMIN)){
            dsService.qoutaDisable(userName, body.get("user"));
            return ResponseEntity.status(HttpStatus.OK).body("UserQuota remove success!");
        }else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    @Operation(summary = "버킷 쿼타 사용 금지 설정", description = "유저 id를 입력하여 유저의 쿼타의 사용을 금지합니다", responses = {
            @ApiResponse(responseCode = "200", description = "유저 쿼타 사용 금지 설정 성공"),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 리소스 접근")})
    @PostMapping("/permission/quota/bucket/{uid}/size/remove")
    public ResponseEntity<String> bucketQuotaDisable(@Parameter(name = "uid", description = "유저 id")@PathVariable("uid") String userName,
                                 @RequestBody Map<String, String> body, @GetIdFromToken Map<String, Object> userInfo){
        if(rgwService.validAccess(userInfo, PF_ADMIN)){
            dsService.qoutaDisable(userName, body.get("bucket"));
            return ResponseEntity.status(HttpStatus.OK).body("BucketQuota remove success!");
        }else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

}
