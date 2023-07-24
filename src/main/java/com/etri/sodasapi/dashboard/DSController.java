package com.etri.sodasapi.dashboard;

import com.etri.sodasapi.common.Quota;
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
     */
    @GetMapping("/quota/{uid}")
    public ResponseEntity<List<HashMap>> userQuotaInfo(@PathVariable("uid") String userName){
        return ResponseEntity.status(HttpStatus.OK).body(dsService.userQoutaInfo(userName));
    }

    @PostMapping("/quota/{uid}/config")
    public ResponseEntity userQuotaConfig(@PathVariable("uid") String userName, @RequestBody Quota quota){
        dsService.userQoutaConfig(userName, quota);
        return ResponseEntity.status(HttpStatus.OK).build();
    }

    @DeleteMapping("/quota/{uid}/config")
    public void userQuotaDisable(@PathVariable("uid") String userName, @RequestBody Map<String, String> body){
        dsService.userQoutaDisable(userName, body.get("quota_type"));
    }
}
