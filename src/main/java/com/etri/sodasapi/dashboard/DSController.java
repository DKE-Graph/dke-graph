package com.etri.sodasapi.dashboard;

import com.etri.sodasapi.common.Quota;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/dashboard")
public class DSController {
    private final DSService dsService;

    /*
        TODO: 2023.7.24 Keycloak과 연동해 관리자 확인하는 코드 추가해야 함.
        Permission-Quota-Get
        유저의 쿼타 정보 출력
     */
    @Operation(summary = "유저 쿼타 정보 출력", description = "유저 id를 입력하여 유저의 쿼타 정보를 출력합니다")
    @GetMapping("/quota/{uid}")
    public ResponseEntity<List<HashMap>> userQuotaInfo(@Parameter(name = "uid", description = "유저 id") @PathVariable("uid") String userName){
        return ResponseEntity.status(HttpStatus.OK).body(dsService.userQoutaInfo(userName));
    }

    /*
        Permission-Quota-Create, Update
        유저 쿼타 설정
     */
    @Operation(summary = "유저 쿼타 설정", description = "유저 id와 쿼타를 입력하여 유저의 쿼타를 설정합니다")
    @PostMapping("/quota/{uid}/config")
    public ResponseEntity userQuotaConfig(@Parameter(name = "uid", description = "유저 id")@PathVariable("uid") String userName,
                                          @RequestBody Quota quota){
        dsService.qoutaConfig(userName, quota);
        return ResponseEntity.status(HttpStatus.OK).build();
    }

    /*
        Permission-Quota-Delete
        쿼타 사용 금지
     */
    @Operation(summary = "유저 쿼타 사용 금지", description = "유저 id를 입력하여 유저의 쿼타의 사용을 금지합니다")
    @DeleteMapping("/quota/{uid}/config")
    public void userQuotaDisable(@Parameter(name = "uid", description = "유저  id")@PathVariable("uid") String userName,
                                 @RequestBody Map<String, String> body){
        dsService.qoutaDisable(userName, body.get("quota_type"));
    }

}
