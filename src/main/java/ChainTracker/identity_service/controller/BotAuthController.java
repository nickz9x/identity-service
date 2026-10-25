package ChainTracker.identity_service.controller;

import ChainTracker.identity_service.dto.TelegramLinkDto;
import ChainTracker.identity_service.dto.TelegramRegisterDto;
import ChainTracker.identity_service.service.UserService;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/bot/auth")
@AllArgsConstructor
public class BotAuthController {
    private UserService service;

    @PostMapping("/link")
    public boolean linkUser(@RequestBody TelegramLinkDto dto){
        return service.linkUser(dto.chatId(), dto.code());
    }

    @PostMapping("/register")
    public boolean registerUser(@RequestBody TelegramRegisterDto dto){
        return service.registerFromTelegram(dto);
    }
}
