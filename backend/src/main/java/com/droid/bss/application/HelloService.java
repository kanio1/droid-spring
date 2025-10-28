package com.droid.bss.application;

import com.droid.bss.application.dto.HelloResponse;
import java.util.List;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

@Service
public class HelloService {

    public HelloResponse greet(Jwt principal) {
        String subject = principal.getSubject();
        List<String> roles = principal.getClaimAsStringList("realm_access.roles");
        return new HelloResponse("Hello, %s".formatted(subject), subject, roles != null ? roles : List.of());
    }
}
