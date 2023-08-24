package com.etri.sodasapi.keycloak;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/keycloak")
public class AdminClientController {
    private final AdminClientService adminClientService;

    @GetMapping("/test")
    public void test(){
        adminClientService.searchUsers();
    }
}
