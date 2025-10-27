package com.droid.bss.api;

import com.droid.bss.application.HelloService;
import com.droid.bss.application.dto.HelloResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/hello")
class HelloController {

    private final HelloService helloService;

    HelloController(HelloService helloService) {
        this.helloService = helloService;
    }

    @GetMapping
    ResponseEntity<HelloResponse> getGreeting(@AuthenticationPrincipal Jwt principal) {
        return ResponseEntity.ok(helloService.greet(principal));
    }
}
