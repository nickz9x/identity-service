package ChainTracker.identity_service.controller;

import ChainTracker.identity_service.dto.LinkResponseDto;
import ChainTracker.identity_service.dto.TelegramLinkDto;
import ChainTracker.identity_service.dto.UserLoginDto;
import ChainTracker.identity_service.dto.UserRegisterDto;
import ChainTracker.identity_service.service.UserService;
import lombok.AllArgsConstructor;
import org.keycloak.representations.AccessTokenResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

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

    @PostMapping("/telegram")
    public ResponseEntity<LinkResponseDto> generateCode(@AuthenticationPrincipal Jwt jwt){
        return ResponseEntity.ok(service.generateLinkCode(jwt.getSubject()));
    }

    @PostMapping("/link")
    public boolean linkUser(@RequestBody TelegramLinkDto dto){
       return service.linkUser(dto.chatId(), dto.code());
    }
}
