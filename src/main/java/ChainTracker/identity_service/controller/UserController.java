package ChainTracker.identity_service.controller;

import ChainTracker.identity_service.dto.UserLoginDto;
import ChainTracker.identity_service.dto.UserRegisterDto;
import ChainTracker.identity_service.service.UserService;
import jakarta.annotation.security.PermitAll;
import lombok.AllArgsConstructor;
import org.keycloak.representations.AccessTokenResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api/auth")
@AllArgsConstructor
public class UserController {
    private UserService service;

    @PostMapping("/register")
    public ResponseEntity<String> createUser(@RequestBody UserRegisterDto dto){
        return ResponseEntity.ok(service.createUser(dto));
    }
    @PostMapping("/login")
    public ResponseEntity<AccessTokenResponse> login(@RequestBody UserLoginDto dto){
       return ResponseEntity.ok(service.login(dto));
    }
}
