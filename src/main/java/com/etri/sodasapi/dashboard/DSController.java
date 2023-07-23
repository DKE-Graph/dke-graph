package com.etri.sodasapi.dashboard;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/dashboard")
public class DSController {
    private final DSService dsService;

    @GetMapping("/test")
    public void test(){
        dsService.getToken();
    }
}
