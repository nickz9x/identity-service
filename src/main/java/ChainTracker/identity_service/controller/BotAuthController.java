package ChainTracker.identity_service.controller;

import ChainTracker.identity_service.dto.TelegramLinkDto;
import ChainTracker.identity_service.service.UserService;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Copia da rota de vinculacao para o bot do Telegram. O fluxo do bot nao
 * carrega JWT de usuario, entao esta versao fica sob /api/bot/** (liberada no
 * SecurityConfiguration). A rota original /api/auth/link continua existindo e
 * exige autenticacao.
 */
@RestController
@RequestMapping("/api/bot/auth")
@AllArgsConstructor
public class BotAuthController {
    private UserService service;

    @PostMapping("/link")
    public boolean linkUser(@RequestBody TelegramLinkDto dto){
        return service.linkUser(dto.chatId(), dto.code());
    }
}
