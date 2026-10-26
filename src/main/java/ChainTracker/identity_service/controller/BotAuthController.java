package ChainTracker.identity_service.controller;

import ChainTracker.identity_service.dto.TelegramLinkDto;
import ChainTracker.identity_service.dto.TelegramRegisterDto;
import ChainTracker.identity_service.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Bot - autenticação", description = "Vínculo e cadastro de contas pelo bot do Telegram. Exigem token com escopo bot.")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/bot/auth")
@AllArgsConstructor
public class BotAuthController {
    private UserService service;

    @Operation(
            summary = "Vincula um chat a uma conta existente",
            description = "Recebe o código gerado no site e o chatId do Telegram e cria o vínculo."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Vínculo criado"),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido"),
            @ApiResponse(responseCode = "403", description = "Token sem o escopo bot"),
            @ApiResponse(responseCode = "409", description = "Usuário ou chat já vinculado")
    })
    @PostMapping("/link")
    public boolean linkUser(@RequestBody TelegramLinkDto dto){
        return service.linkUser(dto.chatId(), dto.code());
    }

    @Operation(
            summary = "Cria uma conta e vincula o chat",
            description = "Cria o usuário no Keycloak, grava o vínculo com o chat e publica o evento linked.users."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Conta criada e vinculada"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos"),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido"),
            @ApiResponse(responseCode = "403", description = "Token sem o escopo bot"),
            @ApiResponse(responseCode = "409", description = "Username, e-mail ou chat já em uso")
    })
    @PostMapping("/register")
    public boolean registerUser(@RequestBody TelegramRegisterDto dto){
        return service.registerFromTelegram(dto);
    }
}
