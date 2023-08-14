package com.etri.sodasapi.objectstorage.dashboard;

import com.etri.sodasapi.objectstorage.common.Quota;
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
    @GetMapping("/quota/{uid}")
    public ResponseEntity<List<HashMap>> userQuotaInfo(@PathVariable("uid") String userName){
        return ResponseEntity.status(HttpStatus.OK).body(dsService.userQoutaInfo(userName));
    }

    /*
        Permission-Quota-Create, Update
        유저 쿼타 설정
     */
    @PostMapping("/quota/{uid}/config")
    public ResponseEntity userQuotaConfig(@PathVariable("uid") String userName, @RequestBody Quota quota){
        dsService.qoutaConfig(userName, quota);
        return ResponseEntity.status(HttpStatus.OK).build();
    }

    /*
        Permission-Quota-Delete
        쿼타 사용 금지
     */
    @DeleteMapping("/quota/{uid}/config")
    public void userQuotaDisable(@PathVariable("uid") String userName, @RequestBody Map<String, String> body){
        dsService.qoutaDisable(userName, body.get("quota_type"));
    }

}
